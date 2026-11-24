package domain

import (
	"errors"
	"strings"
	"time"
)

type Position string

const (
	PositionKeeper     Position = "KEEPER"
	PositionDefender   Position = "DEFENDER"
	PositionMidfielder Position = "MIDFIELDER"
	PositionStriker    Position = "STRIKER"
)

func (p Position) IsValid() bool {
	switch p {
	case PositionKeeper, PositionDefender, PositionMidfielder, PositionStriker:
		return true
	default:
		return false
	}
}

type Player struct {
	ID              string    `json:"id"`
	DisplayName     string    `json:"display_name"`
	PrimaryPosition Position  `json:"primary_position"`
	IsActive        bool      `json:"is_active"`
	Notes           *string   `json:"notes,omitempty"`
	CreatedAt       time.Time `json:"created_at"`
	UpdatedAt       time.Time `json:"updated_at"`
}

func (p *Player) Validate() error {
	trimmed := strings.TrimSpace(p.DisplayName)
	if trimmed == "" {
		return errors.New("display_name cannot be empty")
	}
	if len(trimmed) > 100 {
		return errors.New("display_name cannot exceed 100 characters")
	}
	p.DisplayName = trimmed

	if !p.PrimaryPosition.IsValid() {
		return errors.New("invalid primary_position. Must be KEEPER, DEFENDER, MIDFIELDER, or STRIKER")
	}
	return nil
}

type PlayerFilter struct {
	IsActive *bool
	Position *Position
	Query    *string
}

type CreatePlayerInput struct {
	DisplayName     string   `json:"display_name"`
	PrimaryPosition Position `json:"primary_position"`
	IsActive        *bool    `json:"is_active,omitempty"`
	Notes           *string  `json:"notes,omitempty"`
}

type UpdatePlayerInput struct {
	DisplayName     *string   `json:"display_name,omitempty"`
	PrimaryPosition *Position `json:"primary_position,omitempty"`
	IsActive        *bool     `json:"is_active,omitempty"`
	Notes           *string   `json:"notes,omitempty"`
}
