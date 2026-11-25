package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
)

var (
	ErrNoTeamsGenerated = errors.New("no teams generated for this match")
)

type TeamRepository struct {
	pool *pgxpool.Pool
}

func NewTeamRepository(pool *pgxpool.Pool) *TeamRepository {
	return &TeamRepository{pool: pool}
}

// SaveGeneratedTeams saves the matchmaking result into teams and team_assignments atomically
func (r *TeamRepository) SaveGeneratedTeams(ctx context.Context, matchID string, result *matchmaker.MatchmakingResult) error {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	// 1. Clear existing assignments and teams for this match
	if _, err := tx.Exec(ctx, "DELETE FROM team_assignments WHERE match_id = $1", matchID); err != nil {
		return fmt.Errorf("failed deleting existing assignments: %w", err)
	}
	if _, err := tx.Exec(ctx, "DELETE FROM teams WHERE match_id = $1", matchID); err != nil {
		return fmt.Errorf("failed deleting existing teams: %w", err)
	}

	// 2. Insert Teams and their player assignments
	now := time.Now().UTC()
	for _, team := range result.Teams {
		var teamID string
		var bibColor *string
		if team.BibColor != "" {
			bibColor = &team.BibColor
		}

		err := tx.QueryRow(ctx, `
			INSERT INTO teams (match_id, team_number, team_name, bib_color, created_at)
			VALUES ($1, $2, $3, $4, $5)
			RETURNING id
		`, matchID, team.TeamNumber, team.TeamName, bibColor, now).Scan(&teamID)
		if err != nil {
			return fmt.Errorf("failed inserting team %d: %w", team.TeamNumber, err)
		}

		// Insert team assignments for this team
		for _, player := range team.Players {
			_, err = tx.Exec(ctx, `
				INSERT INTO team_assignments (
					match_id, team_id, player_id, assigned_position, is_substitute, is_manual_override, created_at, updated_at
				) VALUES ($1, $2, $3, $4, false, $5, $6, $6)
			`, matchID, teamID, player.Player.ID, string(player.AssignedPosition), player.IsManualOverride, now)
			if err != nil {
				return fmt.Errorf("failed assigning player %s to team %d: %w", player.Player.DisplayName, team.TeamNumber, err)
			}
		}
	}

	// 3. Insert substitutes (team_id is NULL)
	for _, sub := range result.Substitutes {
		_, err = tx.Exec(ctx, `
			INSERT INTO team_assignments (
				match_id, team_id, player_id, assigned_position, is_substitute, is_manual_override, created_at, updated_at
			) VALUES ($1, NULL, $2, $3, true, false, $4, $4)
		`, matchID, sub.ID, string(sub.PrimaryPosition), now)
		if err != nil {
			return fmt.Errorf("failed assigning substitute %s: %w", sub.DisplayName, err)
		}
	}

	// 4. Update match status to TEAMS_GENERATED
	_, err = tx.Exec(ctx, `
		UPDATE matches
		SET status = 'TEAMS_GENERATED', updated_at = $2
		WHERE id = $1
	`, matchID, now)
	if err != nil {
		return fmt.Errorf("failed updating match status: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("failed committing team generation transaction: %w", err)
	}

	return nil
}

// GetTeamsByMatchID retrieves all teams, assigned players, and substitutes for a match
func (r *TeamRepository) GetTeamsByMatchID(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	// 1. Fetch match metadata
	var match domain.Match
	var fmtStr, statusStr, subStr string
	err := r.pool.QueryRow(ctx, `
		SELECT id, format, status, sub_policy
		FROM matches
		WHERE id = $1
	`, matchID).Scan(&match.ID, &fmtStr, &statusStr, &subStr)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrMatchNotFound
		}
		return nil, fmt.Errorf("failed fetching match for teams: %w", err)
	}
	match.Format = domain.MatchFormat(fmtStr)
	match.Status = domain.MatchStatus(statusStr)
	match.SubPolicy = domain.SubstitutePolicy(subStr)

	// 2. Fetch teams
	rows, err := r.pool.Query(ctx, `
		SELECT id, team_number, team_name, bib_color
		FROM teams
		WHERE match_id = $1
		ORDER BY team_number ASC
	`, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed fetching teams: %w", err)
	}
	defer rows.Close()

	teamMap := make(map[string]*domain.TeamDetail)
	var teams []domain.TeamDetail

	for rows.Next() {
		var td domain.TeamDetail
		var bibColor *string
		if err := rows.Scan(&td.ID, &td.TeamNumber, &td.TeamName, &bibColor); err != nil {
			return nil, fmt.Errorf("failed scanning team: %w", err)
		}
		td.BibColor = bibColor
		td.Players = make([]domain.TeamPlayerDetail, 0)
		td.PositionCounts = map[domain.Position]int{
			domain.PositionKeeper:     0,
			domain.PositionDefender:   0,
			domain.PositionMidfielder: 0,
			domain.PositionStriker:    0,
		}
		teams = append(teams, td)
	}
	rows.Close()

	// Link map pointers for quick lookup by ID
	for i := range teams {
		teamMap[teams[i].ID] = &teams[i]
	}

	// 3. Fetch player assignments
	assignRows, err := r.pool.Query(ctx, `
		SELECT 
			ta.team_id, ta.player_id, ta.assigned_position, ta.is_substitute, ta.is_manual_override,
			p.display_name, p.primary_position
		FROM team_assignments ta
		JOIN players p ON ta.player_id = p.id
		WHERE ta.match_id = $1
		ORDER BY ta.is_substitute ASC, ta.assigned_position ASC, p.display_name ASC
	`, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed fetching assignments: %w", err)
	}
	defer assignRows.Close()

	var substitutes []domain.TeamPlayerDetail
	assignedCount := 0

	for assignRows.Next() {
		var teamID *string
		var playerID, assignPosStr, primaryPosStr, displayName string
		var isSub, isManual bool

		err := assignRows.Scan(
			&teamID,
			&playerID,
			&assignPosStr,
			&isSub,
			&isManual,
			&displayName,
			&primaryPosStr,
		)
		if err != nil {
			return nil, fmt.Errorf("failed scanning assignment: %w", err)
		}

		assignedPos := domain.Position(assignPosStr)
		primaryPos := domain.Position(primaryPosStr)
		isEmergency := assignedPos == domain.PositionKeeper && primaryPos != domain.PositionKeeper

		pDetail := domain.TeamPlayerDetail{
			ID:                playerID,
			DisplayName:       displayName,
			PrimaryPosition:   primaryPos,
			AssignedPosition:  assignedPos,
			IsEmergencyKeeper: isEmergency,
			IsManualOverride:  isManual,
		}

		if isSub || teamID == nil {
			substitutes = append(substitutes, pDetail)
		} else {
			if t, ok := teamMap[*teamID]; ok {
				t.Players = append(t.Players, pDetail)
				t.PositionCounts[assignedPos]++
				assignedCount++
			}
		}
	}

	// 4. Generate warnings if any team is missing goalkeeper
	var warnings []string
	for _, t := range teams {
		if t.PositionCounts[domain.PositionKeeper] == 0 {
			warnings = append(warnings, fmt.Sprintf("%s has no goalkeeper (Needs Emergency GK)", t.TeamName))
		}
	}

	return &domain.MatchTeamsResponse{
		MatchID:          match.ID,
		Format:           match.Format,
		SubPolicy:        match.SubPolicy,
		Status:           match.Status,
		TeamCount:        len(teams),
		Teams:            teams,
		Substitutes:      substitutes,
		Warnings:         warnings,
		TotalAttendees:   assignedCount + len(substitutes),
		AssignedCount:    assignedCount,
		SubstitutesCount: len(substitutes),
	}, nil
}

// SwapPlayers swaps two players between teams or between team and bench atomically
func (r *TeamRepository) SwapPlayers(ctx context.Context, matchID, playerAID, playerBID string) error {
	if playerAID == playerBID {
		return domain.ErrCannotSwapSelf
	}

	// 1. Verify match editable state
	var matchStatus string
	err := r.pool.QueryRow(ctx, "SELECT status FROM matches WHERE id = $1", matchID).Scan(&matchStatus)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.ErrMatchNotFound
		}
		return fmt.Errorf("failed fetching match status: %w", err)
	}
	if matchStatus == string(domain.StatusCompleted) || matchStatus == string(domain.StatusCancelled) {
		return domain.ErrMatchCompleted
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to begin swap transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	// 2. Fetch current assignments for both players
	type assignData struct {
		teamID       *string
		assignedPos  domain.Position
		isSubstitute bool
	}
	assignments := make(map[string]assignData)

	rows, err := tx.Query(ctx, `
		SELECT player_id, team_id, assigned_position, is_substitute
		FROM team_assignments
		WHERE match_id = $1 AND player_id IN ($2, $3)
	`, matchID, playerAID, playerBID)
	if err != nil {
		return fmt.Errorf("failed querying player assignments: %w", err)
	}
	defer rows.Close()

	for rows.Next() {
		var pid string
		var tid *string
		var posStr string
		var isSub bool
		if err := rows.Scan(&pid, &tid, &posStr, &isSub); err != nil {
			return fmt.Errorf("failed scanning player assignment: %w", err)
		}
		assignments[pid] = assignData{
			teamID:       tid,
			assignedPos:  domain.Position(posStr),
			isSubstitute: isSub,
		}
	}
	rows.Close()

	if len(assignments) != 2 {
		return domain.ErrAssignmentNotFound
	}

	// 3. Fetch primary positions
	pRows, err := tx.Query(ctx, `
		SELECT id, primary_position
		FROM players
		WHERE id IN ($1, $2)
	`, playerAID, playerBID)
	if err != nil {
		return fmt.Errorf("failed querying players primary positions: %w", err)
	}
	defer pRows.Close()

	primaryPositions := make(map[string]domain.Position)
	for pRows.Next() {
		var pid, posStr string
		if err := pRows.Scan(&pid, &posStr); err != nil {
			return fmt.Errorf("failed scanning player: %w", err)
		}
		primaryPositions[pid] = domain.Position(posStr)
	}
	pRows.Close()

	aCurrent := assignments[playerAID]
	bCurrent := assignments[playerBID]
	aPrimary := primaryPositions[playerAID]
	bPrimary := primaryPositions[playerBID]

	// 4. Determine swapped positions
	var aNewPos domain.Position
	if bCurrent.isSubstitute {
		aNewPos = aPrimary
	} else if bCurrent.assignedPos == domain.PositionKeeper {
		aNewPos = domain.PositionKeeper
	} else if aCurrent.assignedPos == domain.PositionKeeper && bCurrent.assignedPos != domain.PositionKeeper {
		aNewPos = aPrimary
		if aNewPos == domain.PositionKeeper {
			aNewPos = domain.PositionMidfielder
		}
	} else {
		aNewPos = bCurrent.assignedPos
	}

	var bNewPos domain.Position
	if aCurrent.isSubstitute {
		bNewPos = bPrimary
	} else if aCurrent.assignedPos == domain.PositionKeeper {
		bNewPos = domain.PositionKeeper
	} else if bCurrent.assignedPos == domain.PositionKeeper && aCurrent.assignedPos != domain.PositionKeeper {
		bNewPos = bPrimary
		if bNewPos == domain.PositionKeeper {
			bNewPos = domain.PositionMidfielder
		}
	} else {
		bNewPos = aCurrent.assignedPos
	}

	now := time.Now().UTC()

	// 5. Update Player A assignment
	_, err = tx.Exec(ctx, `
		UPDATE team_assignments
		SET team_id = $1, assigned_position = $2, is_substitute = $3, is_manual_override = true, updated_at = $4
		WHERE match_id = $5 AND player_id = $6
	`, bCurrent.teamID, string(aNewPos), bCurrent.isSubstitute, now, matchID, playerAID)
	if err != nil {
		return fmt.Errorf("failed updating player A assignment: %w", err)
	}

	// 6. Update Player B assignment
	_, err = tx.Exec(ctx, `
		UPDATE team_assignments
		SET team_id = $1, assigned_position = $2, is_substitute = $3, is_manual_override = true, updated_at = $4
		WHERE match_id = $5 AND player_id = $6
	`, aCurrent.teamID, string(bNewPos), aCurrent.isSubstitute, now, matchID, playerBID)
	if err != nil {
		return fmt.Errorf("failed updating player B assignment: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("failed committing swap transaction: %w", err)
	}

	return nil
}

// MovePlayer moves a player to a target team or to the bench (also handles inserting late arrivals)
func (r *TeamRepository) MovePlayer(ctx context.Context, matchID string, input domain.MovePlayerInput) error {
	// 1. Verify match editable state
	var matchStatus string
	err := r.pool.QueryRow(ctx, "SELECT status FROM matches WHERE id = $1", matchID).Scan(&matchStatus)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.ErrMatchNotFound
		}
		return fmt.Errorf("failed fetching match status: %w", err)
	}
	if matchStatus == string(domain.StatusCompleted) || matchStatus == string(domain.StatusCancelled) {
		return domain.ErrMatchCompleted
	}

	// 2. Fetch player primary position
	var primaryPosStr string
	err = r.pool.QueryRow(ctx, "SELECT primary_position FROM players WHERE id = $1", input.PlayerID).Scan(&primaryPosStr)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return errors.New("player not found")
		}
		return fmt.Errorf("failed fetching player: %w", err)
	}
	primaryPos := domain.Position(primaryPosStr)

	// 3. Verify target team if not substitute
	var targetTeamID *string
	if !input.IsSubstitute {
		if input.TargetTeamID == nil || *input.TargetTeamID == "" {
			return errors.New("target team ID is required when moving to a team")
		}
		var count int
		err := r.pool.QueryRow(ctx, "SELECT COUNT(*) FROM teams WHERE match_id = $1 AND id = $2", matchID, *input.TargetTeamID).Scan(&count)
		if err != nil || count == 0 {
			return domain.ErrTargetTeamNotFound
		}
		targetTeamID = input.TargetTeamID
	}

	// 4. Determine assigned position
	assignedPos := primaryPos
	if input.AssignedPosition != nil && input.AssignedPosition.IsValid() {
		assignedPos = *input.AssignedPosition
	} else if input.IsSubstitute {
		assignedPos = primaryPos
	}

	now := time.Now().UTC()

	// 5. Ensure player is checked into match attendees (especially for late arrivals)
	_, _ = r.pool.Exec(ctx, `
		INSERT INTO match_attendees (match_id, player_id, is_late_arrival, checked_in_at)
		VALUES ($1, $2, true, $3)
		ON CONFLICT (match_id, player_id) DO NOTHING
	`, matchID, input.PlayerID, now)

	// 6. Upsert into team_assignments
	_, err = r.pool.Exec(ctx, `
		INSERT INTO team_assignments (
			match_id, team_id, player_id, assigned_position, is_substitute, is_manual_override, created_at, updated_at
		) VALUES ($1, $2, $3, $4, $5, true, $6, $6)
		ON CONFLICT (match_id, player_id) DO UPDATE SET
			team_id = EXCLUDED.team_id,
			assigned_position = EXCLUDED.assigned_position,
			is_substitute = EXCLUDED.is_substitute,
			is_manual_override = true,
			updated_at = $6
	`, matchID, targetTeamID, input.PlayerID, string(assignedPos), input.IsSubstitute, now)
	if err != nil {
		return fmt.Errorf("failed moving player: %w", err)
	}

	return nil
}

// FinaliseMatch locks the team allocations and marks status as FINALISED
func (r *TeamRepository) FinaliseMatch(ctx context.Context, matchID string) error {
	now := time.Now().UTC()
	res, err := r.pool.Exec(ctx, `
		UPDATE matches
		SET status = 'FINALISED', updated_at = $2
		WHERE id = $1 AND status NOT IN ('COMPLETED', 'CANCELLED')
	`, matchID, now)
	if err != nil {
		return fmt.Errorf("failed finalising match: %w", err)
	}
	if res.RowsAffected() == 0 {
		// Check if match exists
		var status string
		err := r.pool.QueryRow(ctx, "SELECT status FROM matches WHERE id = $1", matchID).Scan(&status)
		if err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return domain.ErrMatchNotFound
			}
			return err
		}
		if status == string(domain.StatusCompleted) || status == string(domain.StatusCancelled) {
			return domain.ErrMatchCompleted
		}
	}
	return nil
}
