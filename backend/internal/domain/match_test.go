package domain

import (
	"testing"
)

func TestMatchFormat(t *testing.T) {
	if !FormatFiveASide.IsValid() || FormatFiveASide.PlayersPerTeam() != 5 {
		t.Errorf("expected FIVE_A_SIDE to be 5 players")
	}
	if !FormatSixASide.IsValid() || FormatSixASide.PlayersPerTeam() != 6 {
		t.Errorf("expected SIX_A_SIDE to be 6 players")
	}
	if !FormatSevenASide.IsValid() || FormatSevenASide.PlayersPerTeam() != 7 {
		t.Errorf("expected SEVEN_A_SIDE to be 7 players")
	}
	if !FormatEightASide.IsValid() || FormatEightASide.PlayersPerTeam() != 8 {
		t.Errorf("expected EIGHT_A_SIDE to be 8 players")
	}
	if !FormatNineASide.IsValid() || FormatNineASide.PlayersPerTeam() != 9 {
		t.Errorf("expected NINE_A_SIDE to be 9 players")
	}
	if !FormatTenASide.IsValid() || FormatTenASide.PlayersPerTeam() != 10 {
		t.Errorf("expected TEN_A_SIDE to be 10 players")
	}
	if !FormatElevenASide.IsValid() || FormatElevenASide.PlayersPerTeam() != 11 {
		t.Errorf("expected ELEVEN_A_SIDE to be 11 players")
	}

	invalidFormat := MatchFormat("TWELVE_A_SIDE")
	if invalidFormat.IsValid() {
		t.Error("expected TWELVE_A_SIDE to be invalid")
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
