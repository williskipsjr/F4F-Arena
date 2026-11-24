package domain

import (
	"testing"
)

func TestPositionValidation(t *testing.T) {
	validPositions := []Position{PositionKeeper, PositionDefender, PositionMidfielder, PositionStriker}
	for _, pos := range validPositions {
		if !pos.IsValid() {
			t.Errorf("expected position %s to be valid", pos)
		}
	}

	invalidPositions := []Position{"GOALKEEPER", "FORWARD", "WING", "", "midfielder"}
	for _, pos := range invalidPositions {
		if pos.IsValid() {
			t.Errorf("expected position %s to be invalid", pos)
		}
	}
}

func TestPlayerValidate(t *testing.T) {
	// Case 1: Valid player
	p := Player{
		DisplayName:     "Lenpu",
		PrimaryPosition: PositionKeeper,
	}
	if err := p.Validate(); err != nil {
		t.Fatalf("unexpected validation error: %v", err)
	}

	// Case 2: Empty display name
	pEmpty := Player{
		DisplayName:     "   ",
		PrimaryPosition: PositionDefender,
	}
	if err := pEmpty.Validate(); err == nil {
		t.Fatal("expected error for empty display name, got nil")
	}

	// Case 3: Invalid position
	pInvalidPos := Player{
		DisplayName:     "Bob",
		PrimaryPosition: "COACH",
	}
	if err := pInvalidPos.Validate(); err == nil {
		t.Fatal("expected error for invalid position, got nil")
	}
}
