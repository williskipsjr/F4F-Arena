package domain

import (
	"testing"
)

func TestMatchFormat(t *testing.T) {
	if !FormatSixASide.IsValid() || FormatSixASide.PlayersPerTeam() != 6 {
		t.Errorf("expected SIX_A_SIDE to be 6 players")
	}
	if !FormatSevenASide.IsValid() || FormatSevenASide.PlayersPerTeam() != 7 {
		t.Errorf("expected SEVEN_A_SIDE to be 7 players")
	}
	if !FormatNineASide.IsValid() || FormatNineASide.PlayersPerTeam() != 9 {
		t.Errorf("expected NINE_A_SIDE to be 9 players")
	}

	invalidFormat := MatchFormat("ELEVEN_A_SIDE")
	if invalidFormat.IsValid() {
		t.Error("expected ELEVEN_A_SIDE to be invalid")
	}
}

func TestMatchValidate(t *testing.T) {
	m := Match{
		Title:  "Saturday Morning 7v7",
		Format: FormatSevenASide,
	}

	if err := m.Validate(); err != nil {
		t.Fatalf("unexpected validation error: %v", err)
	}

	if m.Status != StatusDraft {
		t.Errorf("expected default status DRAFT, got %s", m.Status)
	}

	if m.SubPolicy != PolicyStrictSubstitutes {
		t.Errorf("expected default sub_policy STRICT_SUBSTITUTES, got %s", m.SubPolicy)
	}

	mInvalid := Match{
		Format: "UNKNOWN_FORMAT",
	}
	if err := mInvalid.Validate(); err == nil {
		t.Fatal("expected error for invalid format, got nil")
	}
}
