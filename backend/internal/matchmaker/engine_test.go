package matchmaker

import (
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

func helperMakePlayers(prefix string, count int, pos domain.Position) []domain.Player {
	players := make([]domain.Player, count)
	for i := 0; i < count; i++ {
		players[i] = domain.Player{
			ID:              fmt.Sprintf("%s-%d", prefix, i+1),
			DisplayName:     fmt.Sprintf("%s Player %d", pos, i+1),
			PrimaryPosition: pos,
			IsActive:        true,
		}
	}
	return players
}

// TC-ENG-01: Empty Attendees
func TestMatchmaker_TC01_EmptyAttendees(t *testing.T) {
	e := NewEngine()
	_, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: []domain.Player{},
	})
	if !errors.Is(err, ErrNoAttendees) {
		t.Fatalf("expected ErrNoAttendees, got %v", err)
	}
}

// TC-ENG-02: Insufficient Attendees
func TestMatchmaker_TC02_InsufficientAttendees(t *testing.T) {
	e := NewEngine()
	attendees := helperMakePlayers("def", 5, domain.PositionDefender)
	_, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees,
	})
	if !errors.Is(err, ErrInsufficientAttendees) {
		t.Fatalf("expected ErrInsufficientAttendees, got %v", err)
	}
}

// TC-ENG-03: Exact Divisibility (28 players for 7v7: 4 GK, 12 DEF, 8 MID, 4 STR)
func TestMatchmaker_TC03_ExactDivisibility(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 4, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 12, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 8, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 4, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		SubPolicy: domain.PolicyStrictSubstitutes,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(res.Teams) != 4 {
		t.Fatalf("expected 4 teams, got %d", len(res.Teams))
	}
	if len(res.Substitutes) != 0 {
		t.Errorf("expected 0 substitutes, got %d", len(res.Substitutes))
	}

	for _, team := range res.Teams {
		if len(team.Players) != 7 {
			t.Errorf("expected team %d to have 7 players, got %d", team.TeamNumber, len(team.Players))
		}
		if !team.HasNaturalKeeper {
			t.Errorf("expected team %d to have natural keeper", team.TeamNumber)
		}
		if team.PositionCounts[domain.PositionKeeper] != 1 {
			t.Errorf("expected 1 keeper in team %d, got %d", team.TeamNumber, team.PositionCounts[domain.PositionKeeper])
		}
		if team.PositionCounts[domain.PositionDefender] != 3 {
			t.Errorf("expected 3 defenders in team %d, got %d", team.TeamNumber, team.PositionCounts[domain.PositionDefender])
		}
		if team.PositionCounts[domain.PositionMidfielder] != 2 {
			t.Errorf("expected 2 midfielders in team %d, got %d", team.TeamNumber, team.PositionCounts[domain.PositionMidfielder])
		}
		if team.PositionCounts[domain.PositionStriker] != 1 {
			t.Errorf("expected 1 striker in team %d, got %d", team.TeamNumber, team.PositionCounts[domain.PositionStriker])
		}
	}
}

// TC-ENG-04: Remainder with STRICT_SUBSTITUTES (30 players for 7v7)
func TestMatchmaker_TC04_RemainderStrictSubstitutes(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 4, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 12, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 10, domain.PositionMidfielder)...) // +2 surplus
	attendees = append(attendees, helperMakePlayers("str", 4, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		SubPolicy: domain.PolicyStrictSubstitutes,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(res.Teams) != 4 {
		t.Fatalf("expected 4 teams, got %d", len(res.Teams))
	}
	if len(res.Substitutes) != 2 {
		t.Fatalf("expected 2 substitutes on bench, got %d", len(res.Substitutes))
	}
	for _, team := range res.Teams {
		if len(team.Players) != 7 {
			t.Errorf("expected team %d to have exactly 7 players, got %d", team.TeamNumber, len(team.Players))
		}
	}
}

// TC-ENG-05: Remainder with DISTRIBUTE_ALL (30 players for 7v7 -> 8, 8, 7, 7)
func TestMatchmaker_TC05_RemainderDistributeAll(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 4, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 12, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 10, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 4, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		SubPolicy: domain.PolicyDistributeAll,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(res.Teams) != 4 {
		t.Fatalf("expected 4 teams, got %d", len(res.Teams))
	}
	if len(res.Substitutes) != 0 {
		t.Fatalf("expected 0 substitutes with DISTRIBUTE_ALL, got %d", len(res.Substitutes))
	}

	// 30 players across 4 teams: first 2 teams have 8, last 2 have 7
	if len(res.Teams[0].Players) != 8 || len(res.Teams[1].Players) != 8 {
		t.Errorf("expected first 2 teams to have 8 players, got %d and %d", len(res.Teams[0].Players), len(res.Teams[1].Players))
	}
	if len(res.Teams[2].Players) != 7 || len(res.Teams[3].Players) != 7 {
		t.Errorf("expected last 2 teams to have 7 players, got %d and %d", len(res.Teams[2].Players), len(res.Teams[3].Players))
	}
}

// TC-ENG-06: Zero Keepers
func TestMatchmaker_TC06_ZeroKeepers(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("def", 6, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 6, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 2, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(res.Teams) != 2 {
		t.Fatalf("expected 2 teams, got %d", len(res.Teams))
	}
	for _, team := range res.Teams {
		if team.HasNaturalKeeper {
			t.Errorf("expected team %d to have NO natural keeper", team.TeamNumber)
		}
	}
	if len(res.Warnings) < 2 {
		t.Errorf("expected at least 2 keeper shortage warnings, got %d", len(res.Warnings))
	}
}

// TC-ENG-07: Keeper Shortage (2 Keepers for 4 Teams)
func TestMatchmaker_TC07_KeeperShortage(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 2, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 12, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 10, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 4, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if !res.Teams[0].HasNaturalKeeper || !res.Teams[1].HasNaturalKeeper {
		t.Error("expected Teams 1 & 2 to have natural Keepers")
	}
	if res.Teams[2].HasNaturalKeeper || res.Teams[3].HasNaturalKeeper {
		t.Error("expected Teams 3 & 4 to lack natural Keepers")
	}
}

// TC-ENG-08: Keeper Surplus (4 Keepers for 2 Teams)
func TestMatchmaker_TC08_KeeperSurplus(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 4, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 6, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 2, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 2, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(res.Teams) != 2 {
		t.Fatalf("expected 2 teams, got %d", len(res.Teams))
	}
	// Both teams should have 1 keeper
	for _, team := range res.Teams {
		if team.PositionCounts[domain.PositionKeeper] != 1 {
			t.Errorf("expected 1 keeper in team %d, got %d", team.TeamNumber, team.PositionCounts[domain.PositionKeeper])
		}
	}
}

// TC-ENG-09: Mono-position (All 28 Strikers)
func TestMatchmaker_TC09_MonoPosition(t *testing.T) {
	e := NewEngine()
	attendees := helperMakePlayers("str", 28, domain.PositionStriker)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees,
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(res.Teams) != 4 {
		t.Fatalf("expected 4 teams, got %d", len(res.Teams))
	}
	for _, team := range res.Teams {
		if len(team.Players) != 7 {
			t.Errorf("expected team %d to have 7 strikers, got %d", team.TeamNumber, len(team.Players))
		}
	}
}

// TC-ENG-10: Duplicate Attendee ID
func TestMatchmaker_TC10_DuplicateAttendeeID(t *testing.T) {
	e := NewEngine()
	attendees := []domain.Player{
		{ID: "p-dup", DisplayName: "Player 1", PrimaryPosition: domain.PositionDefender},
		{ID: "p-dup", DisplayName: "Player 2", PrimaryPosition: domain.PositionMidfielder},
	}

	_, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSixASide,
		Attendees: attendees,
	})
	if !errors.Is(err, ErrDuplicatePlayerID) {
		t.Fatalf("expected ErrDuplicatePlayerID, got %v", err)
	}
}

// TC-ENG-11: Invariant Check — No Player Omission
func TestMatchmaker_TC11_NoPlayerOmissionInvariant(t *testing.T) {
	e := NewEngine()
	for total := 7; total <= 42; total++ {
		var attendees []domain.Player
		attendees = append(attendees, helperMakePlayers("gk", 5, domain.PositionKeeper)...)
		attendees = append(attendees, helperMakePlayers("def", 14, domain.PositionDefender)...)
		attendees = append(attendees, helperMakePlayers("mid", 14, domain.PositionMidfielder)...)
		attendees = append(attendees, helperMakePlayers("str", 9, domain.PositionStriker)...)
		input := attendees[:total]

		res, err := e.Generate(MatchmakingRequest{
			Format:    domain.FormatSevenASide,
			Attendees: input,
		})
		if err != nil {
			t.Fatalf("failed at total %d: %v", total, err)
		}

		if res.AssignedCount+res.SubstitutesCount != total {
			t.Fatalf("invariant broken at total %d: assigned (%d) + subs (%d) != %d",
				total, res.AssignedCount, res.SubstitutesCount, total)
		}
	}
}

// TC-ENG-12: Invariant Check — Zero Duplicate Assignments
func TestMatchmaker_TC12_ZeroDuplicateAssignmentsInvariant(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 5, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 14, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 14, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 9, domain.PositionStriker)...)

	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees, // all 42 players
	})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	seen := make(map[string]bool)
	for _, team := range res.Teams {
		for _, p := range team.Players {
			if seen[p.Player.ID] {
				t.Fatalf("duplicate assignment detected for player %s in team %d", p.Player.ID, team.TeamNumber)
			}
			seen[p.Player.ID] = true
		}
	}

	for _, sub := range res.Substitutes {
		if seen[sub.ID] {
			t.Fatalf("player %s assigned to both team and substitute", sub.ID)
		}
		seen[sub.ID] = true
	}
}

// TC-ENG-13: Performance Benchmark (< 5ms for 50 players)
func TestMatchmaker_TC13_PerformanceBenchmark(t *testing.T) {
	e := NewEngine()
	var attendees []domain.Player
	attendees = append(attendees, helperMakePlayers("gk", 6, domain.PositionKeeper)...)
	attendees = append(attendees, helperMakePlayers("def", 18, domain.PositionDefender)...)
	attendees = append(attendees, helperMakePlayers("mid", 16, domain.PositionMidfielder)...)
	attendees = append(attendees, helperMakePlayers("str", 10, domain.PositionStriker)...)

	start := time.Now()
	res, err := e.Generate(MatchmakingRequest{
		Format:    domain.FormatSevenASide,
		Attendees: attendees, // 50 players
	})
	elapsed := time.Since(start)

	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if elapsed > 5*time.Millisecond {
		t.Fatalf("performance benchmark failed: took %v (expected < 5ms)", elapsed)
	}

	t.Logf("TC-ENG-13 passed: 50 players partitioned into %d teams + %d substitutes in %v",
		len(res.Teams), len(res.Substitutes), elapsed)
}
