package service

import (
	"context"
	"fmt"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
)

type TeamStore interface {
	SaveGeneratedTeams(ctx context.Context, matchID string, result *matchmaker.MatchmakingResult) error
	GetTeamsByMatchID(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
}

type TeamService interface {
	GenerateTeams(ctx context.Context, matchID string, subPolicyOverride *domain.SubstitutePolicy) (*domain.MatchTeamsResponse, error)
	GetTeams(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error)
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
	var combinedWarnings []string
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
