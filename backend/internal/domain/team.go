package domain

import (
	"errors"
	"time"
)

var (
	ErrAssignmentNotFound = errors.New("player assignment not found for this match")
	ErrMatchCompleted     = errors.New("cannot edit a completed or cancelled match")
	ErrTargetTeamNotFound = errors.New("target team does not exist for this match")
	ErrPlayerNotAttendee  = errors.New("player is not an attendee in this match")
	ErrCannotSwapSelf     = errors.New("cannot swap a player with themselves")
)

type Team struct {
	ID         string    `json:"id"`
	MatchID    string    `json:"match_id"`
	TeamNumber int       `json:"team_number"`
	TeamName   string    `json:"team_name"`
	BibColor   *string   `json:"bib_color,omitempty"`
	CreatedAt  time.Time `json:"created_at"`
}

type TeamAssignment struct {
	ID               string    `json:"id"`
	MatchID          string    `json:"match_id"`
	TeamID           *string   `json:"team_id,omitempty"`
	PlayerID         string    `json:"player_id"`
	AssignedPosition Position  `json:"assigned_position"`
	IsSubstitute     bool      `json:"is_substitute"`
	IsManualOverride bool      `json:"is_manual_override"`
	CreatedAt        time.Time `json:"created_at"`
	UpdatedAt        time.Time `json:"updated_at"`
}

type TeamPlayerDetail struct {
	ID                string   `json:"id"`
	DisplayName       string   `json:"display_name"`
	PrimaryPosition   Position `json:"primary_position"`
	AssignedPosition  Position `json:"assigned_position"`
	IsEmergencyKeeper bool     `json:"is_emergency_keeper"`
	IsManualOverride  bool     `json:"is_manual_override"`
}

type TeamDetail struct {
	ID             string             `json:"id"`
	TeamNumber     int                `json:"team_number"`
	TeamName       string             `json:"team_name"`
	BibColor       *string            `json:"bib_color,omitempty"`
	Players        []TeamPlayerDetail `json:"players"`
	PositionCounts map[Position]int   `json:"position_counts"`
}

type MatchTeamsResponse struct {
	MatchID          string             `json:"match_id"`
	Format           MatchFormat        `json:"format"`
	SubPolicy        SubstitutePolicy   `json:"sub_policy"`
	Status           MatchStatus        `json:"status"`
	TeamCount        int                `json:"team_count"`
	Teams            []TeamDetail       `json:"teams"`
	Substitutes      []TeamPlayerDetail `json:"substitutes"`
	Warnings         []string           `json:"warnings"`
	TotalAttendees   int                `json:"total_attendees"`
	AssignedCount    int                `json:"assigned_count"`
	SubstitutesCount int                `json:"substitutes_count"`
}

type SwapPlayersInput struct {
	PlayerAID string `json:"player_a_id"`
	PlayerBID string `json:"player_b_id"`
}

type MovePlayerInput struct {
	PlayerID         string    `json:"player_id"`
	TargetTeamID     *string   `json:"target_team_id,omitempty"`
	IsSubstitute     bool      `json:"is_substitute"`
	AssignedPosition *Position `json:"assigned_position,omitempty"`
}
