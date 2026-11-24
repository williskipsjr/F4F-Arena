package postgres

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
)

func TestPostgres_TeamPersistence_Live(t *testing.T) {
	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		dbURL = "postgres://f4f_admin:f4f_secure_password@localhost:5432/f4f_arena?sslmode=disable"
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	pool, err := pgxpool.New(ctx, dbURL)
	if err != nil {
		t.Skipf("skipping live postgres test (connection failed): %v", err)
		return
	}
	defer pool.Close()

	if err := pool.Ping(ctx); err != nil {
		t.Skipf("skipping live postgres test (ping failed): %v", err)
		return
	}

	matchRepo := NewMatchRepository(pool)
	teamRepo := NewTeamRepository(pool)

	// 1. Create a live match
	match := &domain.Match{
		Title:     "Live Test Session",
		Format:    domain.FormatSevenASide,
		Status:    domain.StatusDraft,
		SubPolicy: domain.PolicyStrictSubstitutes,
	}
	if err := matchRepo.Create(ctx, match); err != nil {
		t.Fatalf("failed creating match: %v", err)
	}
	defer func() {
		_, _ = pool.Exec(context.Background(), "DELETE FROM matches WHERE id = $1", match.ID)
	}()

	// 2. Fetch active players from DB
	playerRepo := NewPlayerRepository(pool)
	players, err := playerRepo.List(ctx, domain.PlayerFilter{})
	if err != nil {
		t.Fatalf("failed fetching players: %v", err)
	}
	if len(players) < 16 {
		t.Fatalf("not enough players seeded: %d", len(players))
	}

	// Pick 15 players (14 assigned, 1 substitute)
	selectedPlayerIDs := make([]string, 15)
	for i := 0; i < 15; i++ {
		selectedPlayerIDs[i] = players[i].ID
	}

	if err := matchRepo.SyncAttendees(ctx, match.ID, selectedPlayerIDs); err != nil {
		t.Fatalf("failed syncing attendees: %v", err)
	}

	// 3. Generate teams using engine
	attendees, err := matchRepo.GetAttendees(ctx, match.ID)
	if err != nil {
		t.Fatalf("failed getting attendees: %v", err)
	}

	var attendeePlayers []domain.Player
	for _, a := range attendees {
		attendeePlayers = append(attendeePlayers, a.Player)
	}

	engine := matchmaker.NewEngine()
	result, err := engine.Generate(matchmaker.MatchmakingRequest{
		Format:    match.Format,
		SubPolicy: match.SubPolicy,
		Attendees: attendeePlayers,
	})
	if err != nil {
		t.Fatalf("failed generating teams: %v", err)
	}

	// 4. Save generated teams via teamRepo
	if err := teamRepo.SaveGeneratedTeams(ctx, match.ID, result); err != nil {
		t.Fatalf("failed saving generated teams: %v", err)
	}

	// 5. Query saved teams via teamRepo
	saved, err := teamRepo.GetTeamsByMatchID(ctx, match.ID)
	if err != nil {
		t.Fatalf("failed getting saved teams: %v", err)
	}

	if saved.TeamCount != 2 {
		t.Errorf("expected 2 teams, got %d", saved.TeamCount)
	}
	if saved.AssignedCount != 14 {
		t.Errorf("expected 14 assigned players, got %d", saved.AssignedCount)
	}
	if saved.SubstitutesCount != 1 {
		t.Errorf("expected 1 substitute, got %d", saved.SubstitutesCount)
	}
	if saved.Status != domain.StatusTeamsGenerated {
		t.Errorf("expected status TEAMS_GENERATED, got %s", saved.Status)
	}

	// 6. Test Swap Players (Team player <-> Bench player)
	team1PlayerID := saved.Teams[0].Players[0].ID
	subPlayerID := saved.Substitutes[0].ID

	if err := teamRepo.SwapPlayers(ctx, match.ID, team1PlayerID, subPlayerID); err != nil {
		t.Fatalf("failed swapping team player and sub player: %v", err)
	}

	afterSwap, err := teamRepo.GetTeamsByMatchID(ctx, match.ID)
	if err != nil {
		t.Fatalf("failed getting teams after swap: %v", err)
	}
	// Verify subPlayer is now on Team 1
	foundOnTeam1 := false
	for _, p := range afterSwap.Teams[0].Players {
		if p.ID == subPlayerID {
			foundOnTeam1 = true
			if !p.IsManualOverride {
				t.Error("expected IsManualOverride to be true for swapped player")
			}
		}
	}
	if !foundOnTeam1 {
		t.Errorf("expected sub player %s to be on Team 1 after swap", subPlayerID)
	}
	// Verify team1Player is now on bench
	foundOnBench := false
	for _, s := range afterSwap.Substitutes {
		if s.ID == team1PlayerID {
			foundOnBench = true
			if !s.IsManualOverride {
				t.Error("expected IsManualOverride to be true for benched player")
			}
		}
	}
	if !foundOnBench {
		t.Errorf("expected previous team player %s to be on bench after swap", team1PlayerID)
	}

	// 7. Test Move Player (late arrival player 16 moved to Team 2)
	player16ID := players[15].ID
	targetTeamID := afterSwap.Teams[1].ID
	err = teamRepo.MovePlayer(ctx, match.ID, domain.MovePlayerInput{
		PlayerID:     player16ID,
		TargetTeamID: &targetTeamID,
		IsSubstitute: false,
	})
	if err != nil {
		t.Fatalf("failed moving late arrival player to Team 2: %v", err)
	}

	afterMove, err := teamRepo.GetTeamsByMatchID(ctx, match.ID)
	if err != nil {
		t.Fatalf("failed getting teams after move: %v", err)
	}
	if afterMove.AssignedCount != 15 {
		t.Errorf("expected 15 assigned players after moving player 16, got %d", afterMove.AssignedCount)
	}

	// 8. Test Finalise Match
	if err := teamRepo.FinaliseMatch(ctx, match.ID); err != nil {
		t.Fatalf("failed finalising match: %v", err)
	}

	afterFinalise, err := teamRepo.GetTeamsByMatchID(ctx, match.ID)
	if err != nil {
		t.Fatalf("failed getting teams after finalise: %v", err)
	}
	if afterFinalise.Status != domain.StatusFinalised {
		t.Errorf("expected match status FINALISED, got %s", afterFinalise.Status)
	}

	t.Logf("Live Phase 4 operations verified: Swap, Move (late arrival), and Finalise all succeeded!")
}
