package service

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
	"github.com/williskipsjr/f4f-arena/backend/internal/matchmaker"
)

type mockTeamStore struct {
	savedResult *matchmaker.MatchmakingResult
	response    *domain.MatchTeamsResponse
}

func (m *mockTeamStore) SaveGeneratedTeams(ctx context.Context, matchID string, result *matchmaker.MatchmakingResult) error {
	m.savedResult = result
	var teams []domain.TeamDetail
	for _, t := range result.Teams {
		var players []domain.TeamPlayerDetail
		for _, p := range t.Players {
			players = append(players, domain.TeamPlayerDetail{
				ID:                p.Player.ID,
				DisplayName:       p.Player.DisplayName,
				PrimaryPosition:   p.Player.PrimaryPosition,
				AssignedPosition:  p.AssignedPosition,
				IsEmergencyKeeper: p.IsEmergencyKeeper,
				IsManualOverride:  p.IsManualOverride,
			})
		}
		teams = append(teams, domain.TeamDetail{
			ID:             fmt.Sprintf("team-uuid-%d", t.TeamNumber),
			TeamNumber:     t.TeamNumber,
			TeamName:       t.TeamName,
			BibColor:       &t.BibColor,
			Players:        players,
			PositionCounts: t.PositionCounts,
		})
	}

	var subs []domain.TeamPlayerDetail
	for _, s := range result.Substitutes {
		subs = append(subs, domain.TeamPlayerDetail{
			ID:               s.ID,
			DisplayName:      s.DisplayName,
			PrimaryPosition:  s.PrimaryPosition,
			AssignedPosition: s.PrimaryPosition,
		})
	}

	m.response = &domain.MatchTeamsResponse{
		MatchID:          matchID,
		Format:           result.Format,
		SubPolicy:        result.SubPolicy,
		Status:           domain.StatusTeamsGenerated,
		TeamCount:        len(teams),
		Teams:            teams,
		Substitutes:      subs,
		Warnings:         result.Warnings,
		TotalAttendees:   result.TotalAttendees,
		AssignedCount:    result.AssignedCount,
		SubstitutesCount: result.SubstitutesCount,
	}
	return nil
}

func (m *mockTeamStore) GetTeamsByMatchID(ctx context.Context, matchID string) (*domain.MatchTeamsResponse, error) {
	if m.response == nil {
		return nil, domain.ErrMatchNotFound
	}
	return m.response, nil
}

type mockMatchStoreForTeam struct {
	match     *domain.Match
	attendees []domain.MatchAttendee
}

func (m *mockMatchStoreForTeam) Create(ctx context.Context, match *domain.Match) error { return nil }
func (m *mockMatchStoreForTeam) GetByID(ctx context.Context, id string) (*domain.Match, error) {
	if m.match == nil || m.match.ID != id {
		return nil, domain.ErrMatchNotFound
	}
	return m.match, nil
}
func (m *mockMatchStoreForTeam) List(ctx context.Context, status *domain.MatchStatus) ([]domain.Match, error) {
	return nil, nil
}
func (m *mockMatchStoreForTeam) Update(ctx context.Context, match *domain.Match) error { return nil }
func (m *mockMatchStoreForTeam) SyncAttendees(ctx context.Context, matchID string, playerIDs []string) error {
	return nil
}
func (m *mockMatchStoreForTeam) GetAttendees(ctx context.Context, matchID string) ([]domain.MatchAttendee, error) {
	return m.attendees, nil
}
func (m *mockMatchStoreForTeam) AddLateAttendee(ctx context.Context, matchID string, playerID string) error {
	return nil
}
func (m *mockMatchStoreForTeam) RemoveAttendee(ctx context.Context, matchID string, playerID string) error {
	return nil
}

func makeAttendees(gk, def, mid, str int) []domain.MatchAttendee {
	var att []domain.MatchAttendee
	idCounter := 1
	add := func(pos domain.Position, count int) {
		for i := 0; i < count; i++ {
			id := fmt.Sprintf("p-%d", idCounter)
			idCounter++
			att = append(att, domain.MatchAttendee{
				MatchID:  "match-1",
				PlayerID: id,
				Player: domain.Player{
					ID:              id,
					DisplayName:     fmt.Sprintf("%s %d", pos, i+1),
					PrimaryPosition: pos,
					IsActive:        true,
				},
				CheckedInAt: time.Now(),
			})
		}
	}
	add(domain.PositionKeeper, gk)
	add(domain.PositionDefender, def)
	add(domain.PositionMidfielder, mid)
	add(domain.PositionStriker, str)
	return att
}

func TestTeamService_GenerateTeams_Success(t *testing.T) {
	matchStore := &mockMatchStoreForTeam{
		match: &domain.Match{
			ID:        "match-1",
			Format:    domain.FormatSevenASide,
			Status:    domain.StatusDraft,
			SubPolicy: domain.PolicyStrictSubstitutes,
		},
		attendees: makeAttendees(2, 4, 4, 4), // 14 attendees
	}
	teamStore := &mockTeamStore{}
	svc := NewTeamService(matchStore, teamStore)

	resp, err := svc.GenerateTeams(context.Background(), "match-1", nil)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if resp.TeamCount != 2 {
		t.Errorf("expected 2 teams, got %d", resp.TeamCount)
	}
	if resp.AssignedCount != 14 {
		t.Errorf("expected 14 assigned players, got %d", resp.AssignedCount)
	}
	if resp.SubstitutesCount != 0 {
		t.Errorf("expected 0 substitutes, got %d", resp.SubstitutesCount)
	}
	if len(resp.Teams[0].Players) != 7 || len(resp.Teams[1].Players) != 7 {
		t.Errorf("expected each team to have 7 players, got %d and %d",
			len(resp.Teams[0].Players), len(resp.Teams[1].Players))
	}
}

func TestTeamService_GenerateTeams_MatchNotFound(t *testing.T) {
	matchStore := &mockMatchStoreForTeam{}
	teamStore := &mockTeamStore{}
	svc := NewTeamService(matchStore, teamStore)

	_, err := svc.GenerateTeams(context.Background(), "non-existent", nil)
	if !errors.Is(err, domain.ErrMatchNotFound) {
		t.Errorf("expected ErrMatchNotFound, got %v", err)
	}
}

func TestTeamService_GenerateTeams_NoAttendees(t *testing.T) {
	matchStore := &mockMatchStoreForTeam{
		match: &domain.Match{
			ID:        "match-empty",
			Format:    domain.FormatSevenASide,
			Status:    domain.StatusDraft,
			SubPolicy: domain.PolicyStrictSubstitutes,
		},
		attendees: []domain.MatchAttendee{},
	}
	teamStore := &mockTeamStore{}
	svc := NewTeamService(matchStore, teamStore)

	_, err := svc.GenerateTeams(context.Background(), "match-empty", nil)
	if !errors.Is(err, matchmaker.ErrNoAttendees) {
		t.Errorf("expected ErrNoAttendees, got %v", err)
	}
}

func TestTeamService_GenerateTeams_InsufficientAttendees(t *testing.T) {
	matchStore := &mockMatchStoreForTeam{
		match: &domain.Match{
			ID:        "match-short",
			Format:    domain.FormatSevenASide,
			Status:    domain.StatusDraft,
			SubPolicy: domain.PolicyStrictSubstitutes,
		},
		attendees: makeAttendees(1, 1, 1, 1), // only 4 players for 7-a-side
	}
	teamStore := &mockTeamStore{}
	svc := NewTeamService(matchStore, teamStore)

	_, err := svc.GenerateTeams(context.Background(), "match-short", nil)
	if !errors.Is(err, matchmaker.ErrInsufficientAttendees) {
		t.Errorf("expected ErrInsufficientAttendees, got %v", err)
	}
}

func TestTeamService_GenerateTeams_SubPolicyOverride(t *testing.T) {
	matchStore := &mockMatchStoreForTeam{
		match: &domain.Match{
			ID:        "match-sub",
			Format:    domain.FormatSevenASide,
			Status:    domain.StatusDraft,
			SubPolicy: domain.PolicyStrictSubstitutes,
		},
		attendees: makeAttendees(2, 5, 5, 4), // 16 attendees
	}
	teamStore := &mockTeamStore{}
	svc := NewTeamService(matchStore, teamStore)

	override := domain.PolicyDistributeAll
	resp, err := svc.GenerateTeams(context.Background(), "match-sub", &override)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if resp.SubPolicy != domain.PolicyDistributeAll {
		t.Errorf("expected sub policy %s, got %s", domain.PolicyDistributeAll, resp.SubPolicy)
	}
	if resp.AssignedCount != 16 {
		t.Errorf("expected 16 assigned with DISTRIBUTE_ALL, got %d", resp.AssignedCount)
	}
	if resp.SubstitutesCount != 0 {
		t.Errorf("expected 0 substitutes with DISTRIBUTE_ALL, got %d", resp.SubstitutesCount)
	}
}

func TestTeamService_GetTeams(t *testing.T) {
	matchStore := &mockMatchStoreForTeam{}
	teamStore := &mockTeamStore{
		response: &domain.MatchTeamsResponse{
			MatchID:   "match-1",
			Format:    domain.FormatSevenASide,
			TeamCount: 2,
		},
	}
	svc := NewTeamService(matchStore, teamStore)

	resp, err := svc.GetTeams(context.Background(), "match-1")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if resp.TeamCount != 2 {
		t.Errorf("expected 2 teams, got %d", resp.TeamCount)
	}
}
