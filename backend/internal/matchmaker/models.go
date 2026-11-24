package matchmaker

import (
	"errors"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

var (
	ErrNoAttendees           = errors.New("cannot generate teams with zero attendees")
	ErrInsufficientAttendees = errors.New("insufficient attendees to form at least one full team")
	ErrDuplicatePlayerID     = errors.New("duplicate player ID detected in attendees list")
	ErrInvalidFormat         = errors.New("invalid match format")
)

type MatchmakingRequest struct {
	Format    domain.MatchFormat
	SubPolicy domain.SubstitutePolicy
	Attendees []domain.Player
}

type AssignedPlayer struct {
	Player            domain.Player   `json:"player"`
	AssignedPosition  domain.Position `json:"assigned_position"`
	IsEmergencyKeeper bool            `json:"is_emergency_keeper"`
	IsManualOverride  bool            `json:"is_manual_override"`
}

type Team struct {
	TeamNumber       int                     `json:"team_number"`
	TeamName         string                  `json:"team_name"`
	BibColor         string                  `json:"bib_color,omitempty"`
	TargetSize       int                     `json:"target_size"`
	Players          []AssignedPlayer        `json:"players"`
	PositionCounts   map[domain.Position]int `json:"position_counts"`
	HasNaturalKeeper bool                    `json:"has_natural_keeper"`
}

type MatchmakingResult struct {
	Format           domain.MatchFormat    `json:"format"`
	SubPolicy        domain.SubstitutePolicy `json:"sub_policy"`
	Teams            []Team                `json:"teams"`
	Substitutes      []domain.Player       `json:"substitutes"`
	Warnings         []string              `json:"warnings"`
	TotalAttendees   int                   `json:"total_attendees"`
	AssignedCount    int                   `json:"assigned_count"`
	SubstitutesCount int                   `json:"substitutes_count"`
}
