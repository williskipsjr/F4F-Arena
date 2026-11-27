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
	FormatFiveASide   MatchFormat = "FIVE_A_SIDE"
	FormatSixASide    MatchFormat = "SIX_A_SIDE"
	FormatSevenASide  MatchFormat = "SEVEN_A_SIDE"
	FormatEightASide  MatchFormat = "EIGHT_A_SIDE"
	FormatNineASide   MatchFormat = "NINE_A_SIDE"
	FormatTenASide    MatchFormat = "TEN_A_SIDE"
	FormatElevenASide MatchFormat = "ELEVEN_A_SIDE"
)

func (f MatchFormat) IsValid() bool {
	switch f {
	case FormatFiveASide, FormatSixASide, FormatSevenASide, FormatEightASide, FormatNineASide, FormatTenASide, FormatElevenASide:
		return true
	default:
		return false
	}
}

func (f MatchFormat) PlayersPerTeam() int {
	switch f {
	case FormatFiveASide:
		return 5
	case FormatSixASide:
		return 6
	case FormatSevenASide:
		return 7
	case FormatEightASide:
		return 8
	case FormatNineASide:
		return 9
	case FormatTenASide:
		return 10
	case FormatElevenASide:
		return 11
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
	ID          string           `json:"id"`
	Title       string           `json:"title"`
	MatchDate   *time.Time       `json:"match_date,omitempty"`
	KickoffTime *string          `json:"kickoff_time,omitempty"`
	Venue       *string          `json:"venue,omitempty"`
	Format      MatchFormat      `json:"format"`
	Status      MatchStatus      `json:"status"`
	SubPolicy   SubstitutePolicy `json:"sub_policy"`
	IsPollOpen  bool             `json:"is_poll_open"`
	InCount     int              `json:"in_count"`
	OutCount    int              `json:"out_count"`
	MyVote      *string          `json:"my_vote,omitempty"`
	Notes       *string          `json:"notes,omitempty"`
	CreatedAt   time.Time        `json:"created_at"`
	UpdatedAt   time.Time        `json:"updated_at"`
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
	Title       string            `json:"title"`
	MatchDate   *time.Time        `json:"match_date,omitempty"`
	KickoffTime *string           `json:"kickoff_time,omitempty"`
	Venue       *string           `json:"venue,omitempty"`
	Format      MatchFormat       `json:"format"`
	SubPolicy   *SubstitutePolicy `json:"sub_policy,omitempty"`
	IsPollOpen  *bool             `json:"is_poll_open,omitempty"`
	Notes       *string           `json:"notes,omitempty"`
}

type UpdateMatchInput struct {
	Title       *string           `json:"title,omitempty"`
	MatchDate   *time.Time        `json:"match_date,omitempty"`
	KickoffTime *string           `json:"kickoff_time,omitempty"`
	Venue       *string           `json:"venue,omitempty"`
	Format      *MatchFormat      `json:"format,omitempty"`
	Status      *MatchStatus      `json:"status,omitempty"`
	SubPolicy   *SubstitutePolicy `json:"sub_policy,omitempty"`
	IsPollOpen  *bool             `json:"is_poll_open,omitempty"`
	Notes       *string           `json:"notes,omitempty"`
}

type CastVoteInput struct {
	Vote     string  `json:"vote"`
	PlayerID *string `json:"player_id,omitempty"`
}

type MatchVoteDetail struct {
	PlayerID        string    `json:"player_id"`
	DisplayName     string    `json:"display_name"`
	PrimaryPosition Position  `json:"primary_position"`
	Vote            string    `json:"vote"`
	VotedAt         time.Time `json:"voted_at"`
}

type MatchComment struct {
	ID         string    `json:"id"`
	MatchID    string    `json:"match_id"`
	PlayerID   *string   `json:"player_id,omitempty"`
	AuthorName string    `json:"author_name"`
	AuthorRole string    `json:"author_role"`
	Message    string    `json:"message"`
	CreatedAt  time.Time `json:"created_at"`
}

type CreateCommentInput struct {
	Message string `json:"message"`
}

type SyncAttendeesInput struct {
	PlayerIDs []string `json:"player_ids"`
}

type AddLateArrivalInput struct {
	PlayerID string `json:"player_id"`
}
