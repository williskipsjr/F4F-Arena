package service

import (
	"context"
	"fmt"
	"strings"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

type MatchStore interface {
	Create(ctx context.Context, m *domain.Match) error
	GetByID(ctx context.Context, id string) (*domain.Match, error)
	List(ctx context.Context, status *domain.MatchStatus) ([]domain.Match, error)
	Update(ctx context.Context, m *domain.Match) error
	Delete(ctx context.Context, id string) error
	SyncAttendees(ctx context.Context, matchID string, playerIDs []string) error
	GetAttendees(ctx context.Context, matchID string) ([]domain.MatchAttendee, error)
	AddLateAttendee(ctx context.Context, matchID string, playerID string) error
	RemoveAttendee(ctx context.Context, matchID string, playerID string) error
}

type MatchService struct {
	store MatchStore
}

func NewMatchService(store MatchStore) *MatchService {
	return &MatchService{store: store}
}

func (s *MatchService) CreateMatch(ctx context.Context, input domain.CreateMatchInput) (*domain.Match, error) {
	subPolicy := domain.PolicyStrictSubstitutes
	if input.SubPolicy != nil {
		subPolicy = *input.SubPolicy
	}

	isPollOpen := false
	if input.IsPollOpen != nil {
		isPollOpen = *input.IsPollOpen
	}

	match := &domain.Match{
		Title:       strings.TrimSpace(input.Title),
		MatchDate:   input.MatchDate,
		KickoffTime: input.KickoffTime,
		Venue:       input.Venue,
		Format:      input.Format,
		Status:      domain.StatusDraft,
		SubPolicy:   subPolicy,
		IsPollOpen:  isPollOpen,
		Notes:       input.Notes,
	}

	if err := match.Validate(); err != nil {
		return nil, err
	}

	if err := s.store.Create(ctx, match); err != nil {
		return nil, err
	}
	return match, nil
}

func (s *MatchService) GetMatch(ctx context.Context, id string) (*domain.Match, error) {
	if strings.TrimSpace(id) == "" {
		return nil, fmt.Errorf("invalid match id")
	}
	return s.store.GetByID(ctx, id)
}

func (s *MatchService) ListMatches(ctx context.Context, status *domain.MatchStatus) ([]domain.Match, error) {
	return s.store.List(ctx, status)
}

func (s *MatchService) DeleteMatch(ctx context.Context, id string) error {
	if strings.TrimSpace(id) == "" {
		return fmt.Errorf("invalid match id")
	}
	return s.store.Delete(ctx, id)
}

func (s *MatchService) UpdateMatch(ctx context.Context, id string, input domain.UpdateMatchInput) (*domain.Match, error) {
	match, err := s.store.GetByID(ctx, id)
	if err != nil {
		return nil, err
	}

	if input.Title != nil {
		match.Title = strings.TrimSpace(*input.Title)
	}
	if input.MatchDate != nil {
		match.MatchDate = input.MatchDate
	}
	if input.KickoffTime != nil {
		match.KickoffTime = input.KickoffTime
	}
	if input.Venue != nil {
		match.Venue = input.Venue
	}
	if input.Format != nil {
		match.Format = *input.Format
	}
	if input.Status != nil {
		match.Status = *input.Status
	}
	if input.SubPolicy != nil {
		match.SubPolicy = *input.SubPolicy
	}
	if input.IsPollOpen != nil {
		match.IsPollOpen = *input.IsPollOpen
	}
	if input.PostMatchPhotoURL != nil {
		match.PostMatchPhotoURL = input.PostMatchPhotoURL
	}
	if input.TurfFee != nil && *input.TurfFee >= 0 {
		match.TurfFee = input.TurfFee
		attendees, _ := s.store.GetAttendees(ctx, id)
		playerCount := len(attendees)
		if playerCount == 0 {
			playerCount = match.InCount
		}
		if playerCount > 0 {
			perPlayer := (*input.TurfFee + playerCount - 1) / playerCount
			match.FeePerPlayer = &perPlayer
		}
	}
	if input.Notes != nil {
		match.Notes = input.Notes
	}

	if err := match.Validate(); err != nil {
		return nil, err
	}

	if err := s.store.Update(ctx, match); err != nil {
		return nil, err
	}
	return match, nil
}

func (s *MatchService) SyncAttendees(ctx context.Context, matchID string, playerIDs []string) (*domain.AttendanceSummary, error) {
	match, err := s.store.GetByID(ctx, matchID)
	if err != nil {
		return nil, err
	}

	// De-duplicate incoming player IDs
	uniqueIDs := make([]string, 0, len(playerIDs))
	seen := make(map[string]bool)
	for _, pid := range playerIDs {
		trimmed := strings.TrimSpace(pid)
		if trimmed != "" && !seen[trimmed] {
			seen[trimmed] = true
			uniqueIDs = append(uniqueIDs, trimmed)
		}
	}

	if err := s.store.SyncAttendees(ctx, match.ID, uniqueIDs); err != nil {
		return nil, err
	}

	return s.GetAttendanceSummary(ctx, match.ID)
}

func (s *MatchService) GetAttendanceSummary(ctx context.Context, matchID string) (*domain.AttendanceSummary, error) {
	match, err := s.store.GetByID(ctx, matchID)
	if err != nil {
		return nil, err
	}

	attendees, err := s.store.GetAttendees(ctx, matchID)
	if err != nil {
		return nil, err
	}

	var gk, def, mid, str int
	for _, a := range attendees {
		switch a.Player.PrimaryPosition {
		case domain.PositionKeeper:
			gk++
		case domain.PositionDefender:
			def++
		case domain.PositionMidfielder:
			mid++
		case domain.PositionStriker:
			str++
		}
	}

	total := len(attendees)
	teamSize := match.Format.PlayersPerTeam()
	suggestedTeams := 0
	subs := total
	if teamSize > 0 && total >= teamSize {
		suggestedTeams = total / teamSize
		subs = total % teamSize
	}

	keeperDeficit := 0
	if suggestedTeams > gk {
		keeperDeficit = suggestedTeams - gk
	}

	return &domain.AttendanceSummary{
		MatchID:          matchID,
		TotalAttendees:   total,
		Keepers:          gk,
		Defenders:        def,
		Midfielders:      mid,
		Strikers:         str,
		SuggestedTeams:   suggestedTeams,
		SubstitutesCount: subs,
		KeeperDeficit:    keeperDeficit,
		Attendees:        attendees,
	}, nil
}

func (s *MatchService) AddLateArrival(ctx context.Context, matchID string, playerID string) (*domain.AttendanceSummary, error) {
	if err := s.store.AddLateAttendee(ctx, matchID, strings.TrimSpace(playerID)); err != nil {
		return nil, err
	}
	return s.GetAttendanceSummary(ctx, matchID)
}

func (s *MatchService) RemoveAttendee(ctx context.Context, matchID string, playerID string) (*domain.AttendanceSummary, error) {
	if err := s.store.RemoveAttendee(ctx, matchID, strings.TrimSpace(playerID)); err != nil {
		return nil, err
	}
	return s.GetAttendanceSummary(ctx, matchID)
}
