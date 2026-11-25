package service

import (
	"context"
	"fmt"
	"strings"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
)

type TeamStore interface {
	SaveGeneratedTeams(ctx context.Context, matchID string, result *matchmaker.MatchmakingResult) error
	GetTeamsByMatchID(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
	SwapPlayers(ctx context.Context, matchID, playerAID, playerBID string) error
	MovePlayer(ctx context.Context, matchID string, input domain.MovePlayerInput) error
	FinaliseMatch(ctx context.Context, matchID string) error
}

type TeamService interface {
	GenerateTeams(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error)
	GetTeams(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
	SwapPlayers(ctx context.Context, matchID string, input domain.SwapPlayersInput) (*domain.MatchTeamsResponse, error)
	MovePlayer(ctx context.Context, matchID string, input domain.MovePlayerInput) (*domain.MatchTeamsResponse, error)
	FinaliseMatch(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
	GetWhatsAppExport(ctx context.Context, matchID string) (*domain.WhatsAppExportResponse, error)
}

type teamService struct {
	matchRepo MatchStore
	teamRepo  TeamStore
	engine    *matchmaker.Engine
}

func NewTeamService(matchRepo MatchStore, teamRepo TeamStore) TeamService {
	return &teamService{
		matchRepo: matchRepo,
		teamRepo:  teamRepo,
		engine:    matchmaker.NewEngine(),
	}
}

func (s *teamService) GenerateTeams(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error) {
	match, err := s.matchRepo.GetByID(ctx, matchID)
	if err != nil {
		return nil, err
	}
	if match.Status == domain.StatusFinalised {
		return nil, domain.ErrMatchFinalised
	}
	if match.Status == domain.StatusCompleted || match.Status == domain.StatusCancelled {
		return nil, domain.ErrMatchCompleted
	}

	subPolicy := match.SubPolicy
	if subPolicyOverride != nil && subPolicyOverride.IsValid() {
		subPolicy = *subPolicyOverride
	}

	attendees, err := s.matchRepo.GetAttendees(ctx, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed fetching match attendees: %w", err)
	}

	if len(attendees) == 0 {
		return nil, matchmaker.ErrNoAttendees
	}

	players := make([]domain.Player, len(attendees))
	for i, a := range attendees {
		players[i] = a.Player
	}

	req := matchmaker.MatchmakingRequest{
		Format:    match.Format,
		SubPolicy: subPolicy,
		Attendees: players,
	}

	result, err := s.engine.Generate(req)
	if err != nil {
		return nil, err
	}

	if err := s.teamRepo.SaveGeneratedTeams(ctx, matchID, result); err != nil {
		return nil, fmt.Errorf("failed saving generated teams: %w", err)
	}

	resp, err := s.teamRepo.GetTeamsByMatchID(ctx, matchID)
	if err != nil {
		return nil, fmt.Errorf("failed retrieving saved teams: %w", err)
	}

	// Merge any engine algorithmic warnings (e.g. shortages/surpluses)
	warningSet := make(map[string]bool)
	combinedWarnings := make([]string, 0)
	for _, w := range resp.Warnings {
		if !warningSet[w] {
			warningSet[w] = true
			combinedWarnings = append(combinedWarnings, w)
		}
	}
	for _, w := range result.Warnings {
		if !warningSet[w] {
			warningSet[w] = true
			combinedWarnings = append(combinedWarnings, w)
		}
	}
	resp.Warnings = combinedWarnings

	return resp, nil
}

func (s *teamService) GetTeams(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	return s.teamRepo.GetTeamsByMatchID(ctx, matchID)
}

func (s *teamService) SwapPlayers(ctx context.Context, matchID string, input domain.SwapPlayersInput) (*domain.MatchTeamsResponse, error) {
	if input.PlayerAID == "" || input.PlayerBID == "" {
		return nil, fmt.Errorf("both player_a_id and player_b_id are required")
	}

	if err := s.teamRepo.SwapPlayers(ctx, matchID, input.PlayerAID, input.PlayerBID); err != nil {
		return nil, err
	}

	return s.teamRepo.GetTeamsByMatchID(ctx, matchID)
}

func (s *teamService) MovePlayer(ctx context.Context, matchID string, input domain.MovePlayerInput) (*domain.MatchTeamsResponse, error) {
	if input.PlayerID == "" {
		return nil, fmt.Errorf("player_id is required")
	}

	if err := s.teamRepo.MovePlayer(ctx, matchID, input); err != nil {
		return nil, err
	}

	return s.teamRepo.GetTeamsByMatchID(ctx, matchID)
}

func (s *teamService) FinaliseMatch(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	if err := s.teamRepo.FinaliseMatch(ctx, matchID); err != nil {
		return nil, err
	}

	return s.teamRepo.GetTeamsByMatchID(ctx, matchID)
}

func (s *teamService) GetWhatsAppExport(ctx context.Context, matchID string) (*domain.WhatsAppExportResponse, error) {
	match, err := s.matchRepo.GetByID(ctx, matchID)
	if err != nil {
		return nil, err
	}

	teamsResp, err := s.teamRepo.GetTeamsByMatchID(ctx, matchID)
	if err != nil {
		return nil, err
	}

	formatted := formatWhatsAppMessage(match, teamsResp)
	return &domain.WhatsAppExportResponse{
		MatchID:       matchID,
		FormattedText: formatted,
	}, nil
}

func formatWhatsAppMessage(match *domain.Match, teamsResp *domain.MatchTeamsResponse) string {
	var sb strings.Builder

	sb.WriteString("⚽ *F4F ARENA — MATCH DAY TEAMS*\n")
	if match.Title != "" {
		sb.WriteString(fmt.Sprintf("📌 *%s* (%s)\n", match.Title, formatLabel(teamsResp.Format)))
	}
	if match.Venue != nil && strings.TrimSpace(*match.Venue) != "" {
		sb.WriteString(fmt.Sprintf("📍 Venue: %s\n", strings.TrimSpace(*match.Venue)))
	}
	sb.WriteString("\n")

	for _, team := range teamsResp.Teams {
		if team.BibColor != nil && strings.TrimSpace(*team.BibColor) != "" {
			sb.WriteString(fmt.Sprintf("*%s (%s)*\n", strings.ToUpper(team.TeamName), strings.TrimSpace(*team.BibColor)))
		} else {
			sb.WriteString(fmt.Sprintf("*%s*\n", strings.ToUpper(team.TeamName)))
		}

		var keepers, defenders, midfielders, strikers []string
		for _, p := range team.Players {
			name := p.DisplayName
			if p.IsEmergencyKeeper {
				name += " (Emergency GK)"
			}
			switch p.AssignedPosition {
			case domain.PositionKeeper:
				keepers = append(keepers, name)
			case domain.PositionDefender:
				defenders = append(defenders, name)
			case domain.PositionMidfielder:
				midfielders = append(midfielders, name)
			case domain.PositionStriker:
				strikers = append(strikers, name)
			}
		}

		if len(keepers) > 0 {
			sb.WriteString(fmt.Sprintf("🧤 Keeper: %s\n", strings.Join(keepers, ", ")))
		} else {
			sb.WriteString("🧤 Keeper: _Needs Emergency GK_\n")
		}
		if len(defenders) > 0 {
			sb.WriteString(fmt.Sprintf("🛡️ Defenders: %s\n", strings.Join(defenders, ", ")))
		}
		if len(midfielders) > 0 {
			sb.WriteString(fmt.Sprintf("⚙️ Midfielders: %s\n", strings.Join(midfielders, ", ")))
		}
		if len(strikers) > 0 {
			sb.WriteString(fmt.Sprintf("⚡ Strikers: %s\n", strings.Join(strikers, ", ")))
		}
		sb.WriteString("\n")
	}

	if len(teamsResp.Substitutes) > 0 {
		sb.WriteString("*SUBSTITUTES / BENCH*\n")
		for _, sub := range teamsResp.Substitutes {
			sb.WriteString(fmt.Sprintf("- %s (%s)\n", sub.DisplayName, posShort(sub.PrimaryPosition)))
		}
		sb.WriteString("\n")
	}

	sb.WriteString("Let's have a great game! ⚽🔥")
	return sb.String()
}

func formatLabel(f domain.MatchFormat) string {
	switch f {
	case domain.FormatSixASide:
		return "6-a-side"
	case domain.FormatSevenASide:
		return "7-a-side"
	case domain.FormatNineASide:
		return "9-a-side"
	default:
		return string(f)
	}
}

func posShort(p domain.Position) string {
	switch p {
	case domain.PositionKeeper:
		return "GK"
	case domain.PositionDefender:
		return "DEF"
	case domain.PositionMidfielder:
		return "MID"
	case domain.PositionStriker:
		return "STR"
	default:
		return string(p)
	}
}
