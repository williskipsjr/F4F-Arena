package domain

import (
	"errors"
	"strings"
	"time"
)

var (
	ErrMatchNotFound = errors.New("match not found")
)

type MatchFormat string

const (
	FormatSixASide   MatchFormat = "SIX_A_SIDE"
	FormatSevenASide MatchFormat = "SEVEN_A_SIDE"
	FormatNineASide  MatchFormat = "NINE_A_SIDE"
)

func (f MatchFormat) IsValid() bool {
	switch f {
	case FormatSixASide, FormatSevenASide, FormatNineASide:
		return true
	default:
		return false
	}
}

func (f MatchFormat) PlayersPerTeam() int {
	switch f {
	case FormatSixASide:
		return 6
	case FormatSevenASide:
		return 7
	case FormatNineASide:
		return 9
	default:
		return 7
	}
}

type MatchStatus string

const (
	StatusDraft            MatchStatus = "DRAFT"
	StatusAttendanceLocked MatchStatus = "ATTENDANCE_LOCKED"
	StatusTeamsGenerated   MatchStatus = "TEAMS_GENERATED"
	StatusFinalised        MatchStatus = "FINALISED"
	StatusCompleted        MatchStatus = "COMPLETED"
	StatusCancelled        MatchStatus = "CANCELLED"
)

func (s MatchStatus) IsValid() bool {
	switch s {
	case StatusDraft, StatusAttendanceLocked, StatusTeamsGenerated, StatusFinalised, StatusCompleted, StatusCancelled:
		return true
	default:
		return false
	}
}

type SubstitutePolicy string

const (
	PolicyStrictSubstitutes SubstitutePolicy = "STRICT_SUBSTITUTES"
	PolicyDistributeAll     SubstitutePolicy = "DISTRIBUTE_ALL"
)

func (p SubstitutePolicy) IsValid() bool {
	switch p {
	case PolicyStrictSubstitutes, PolicyDistributeAll:
		return true
	default:
		return false
	}
}

type Match struct {
	ID        string           `json:"id"`
	Title     string           `json:"title"`
	MatchDate *time.Time       `json:"match_date,omitempty"`
	Venue     *string          `json:"venue,omitempty"`
	Format    MatchFormat      `json:"format"`
	Status    MatchStatus      `json:"status"`
	SubPolicy SubstitutePolicy `json:"sub_policy"`
	Notes     *string          `json:"notes,omitempty"`
	CreatedAt time.Time        `json:"created_at"`
	UpdatedAt time.Time        `json:"updated_at"`
}

func (m *Match) Validate() error {
	trimmed := strings.TrimSpace(m.Title)
	if trimmed == "" {
		m.Title = "Match Session"
	} else {
		m.Title = trimmed
	}

	if !m.Format.IsValid() {
		return errors.New("invalid format. Must be SIX_A_SIDE, SEVEN_A_SIDE, or NINE_A_SIDE")
	}

	if m.Status == "" {
		m.Status = StatusDraft
	} else if !m.Status.IsValid() {
		return errors.New("invalid status")
	}

	if m.SubPolicy == "" {
		m.SubPolicy = PolicyStrictSubstitutes
	} else if !m.SubPolicy.IsValid() {
		return errors.New("invalid sub_policy. Must be STRICT_SUBSTITUTES or DISTRIBUTE_ALL")
	}

	return nil
}

type MatchAttendee struct {
	MatchID       string    `json:"match_id"`
	PlayerID      string    `json:"player_id"`
	Player        Player    `json:"player"`
	IsLateArrival bool      `json:"is_late_arrival"`
	CheckedInAt   time.Time `json:"checked_in_at"`
}

type AttendanceSummary struct {
	MatchID           string          `json:"match_id"`
	TotalAttendees    int             `json:"total_attendees"`
	Keepers           int             `json:"keepers"`
	Defenders         int             `json:"defenders"`
	Midfielders       int             `json:"midfielders"`
	Strikers          int             `json:"strikers"`
	SuggestedTeams    int             `json:"suggested_teams"`
	SubstitutesCount  int             `json:"substitutes_count"`
	KeeperDeficit     int             `json:"keeper_deficit"`
	Attendees         []MatchAttendee `json:"attendees"`
}

type CreateMatchInput struct {
	Title     string            `json:"title"`
	MatchDate *time.Time        `json:"match_date,omitempty"`
	Venue     *string           `json:"venue,omitempty"`
	Format    MatchFormat       `json:"format"`
	SubPolicy *SubstitutePolicy `json:"sub_policy,omitempty"`
	Notes     *string           `json:"notes,omitempty"`
}

type UpdateMatchInput struct {
	Title     *string           `json:"title,omitempty"`
	MatchDate *time.Time        `json:"match_date,omitempty"`
	Venue     *string           `json:"venue,omitempty"`
	Format    *MatchFormat      `json:"format,omitempty"`
	Status    *MatchStatus      `json:"status,omitempty"`
	SubPolicy *SubstitutePolicy `json:"sub_policy,omitempty"`
	Notes     *string           `json:"notes,omitempty"`
}

type SyncAttendeesInput struct {
	PlayerIDs []string `json:"player_ids"`
}

type AddLateArrivalInput struct {
	PlayerID string `json:"player_id"`
}
