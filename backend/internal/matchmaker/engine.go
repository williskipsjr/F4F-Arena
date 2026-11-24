package matchmaker

import (
	"fmt"
	"sort"

	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

type Engine struct{}

func NewEngine() *Engine {
	return &Engine{}
}

func (e *Engine) Generate(req MatchmakingRequest) (*MatchmakingResult, error) {
	if len(req.Attendees) == 0 {
		return nil, ErrNoAttendees
	}

	if !req.Format.IsValid() {
		return nil, ErrInvalidFormat
	}

	// 1. Validate Unique Attendee IDs
	seenIDs := make(map[string]bool, len(req.Attendees))
	for _, a := range req.Attendees {
		if a.ID == "" {
			return nil, fmt.Errorf("attendee '%s' has empty ID", a.DisplayName)
		}
		if seenIDs[a.ID] {
			return nil, fmt.Errorf("%w: %s (%s)", ErrDuplicatePlayerID, a.DisplayName, a.ID)
		}
		seenIDs[a.ID] = true
	}

	template := GetTemplate(req.Format)
	teamSize := template.TeamSize
	totalAttendees := len(req.Attendees)
	teamCount := totalAttendees / teamSize

	if teamCount == 0 {
		return nil, fmt.Errorf("%w: %d attendees provided for %s (requires minimum %d)",
			ErrInsufficientAttendees, totalAttendees, string(req.Format), teamSize)
	}

	// 2. Determine Team Target Capacities
	subPolicy := req.SubPolicy
	if subPolicy == "" {
		subPolicy = domain.PolicyStrictSubstitutes
	}

	teams := make([]Team, teamCount)
	remainder := totalAttendees % teamSize

	for i := 0; i < teamCount; i++ {
		targetCap := teamSize
		if subPolicy == domain.PolicyDistributeAll && i < remainder {
			targetCap = teamSize + 1
		}

		teams[i] = Team{
			TeamNumber:     i + 1,
			TeamName:       fmt.Sprintf("Team %d", i+1),
			BibColor:       GetBibColor(i),
			TargetSize:     targetCap,
			Players:        make([]AssignedPlayer, 0, targetCap),
			PositionCounts: make(map[domain.Position]int),
		}
	}

	// 3. Partition Attendees by Position
	var (
		keepers     []domain.Player
		defenders   []domain.Player
		midfielders []domain.Player
		strikers    []domain.Player
		warnings    []string
	)

	for _, p := range req.Attendees {
		switch p.PrimaryPosition {
		case domain.PositionKeeper:
			keepers = append(keepers, p)
		case domain.PositionDefender:
			defenders = append(defenders, p)
		case domain.PositionMidfielder:
			midfielders = append(midfielders, p)
		case domain.PositionStriker:
			strikers = append(strikers, p)
		default:
			midfielders = append(midfielders, p)
		}
	}

	assignedIDs := make(map[string]bool)

	// 4. Allocate Keepers First (1 per team target)
	for i := 0; i < teamCount; i++ {
		if i < len(keepers) {
			kp := keepers[i]
			teams[i].Players = append(teams[i].Players, AssignedPlayer{
				Player:           kp,
				AssignedPosition: domain.PositionKeeper,
			})
			teams[i].PositionCounts[domain.PositionKeeper]++
			teams[i].HasNaturalKeeper = true
			assignedIDs[kp.ID] = true
		} else {
			teams[i].HasNaturalKeeper = false
			warnings = append(warnings, fmt.Sprintf("Team %d has no natural Keeper. Emergency Keeper assignment recommended.", i+1))
		}
	}

	// Surplus keepers beyond team count move to outfield overflow pool
	var overflow []domain.Player
	if len(keepers) > teamCount {
		surplus := keepers[teamCount:]
		overflow = append(overflow, surplus...)
		warnings = append(warnings, fmt.Sprintf("%d natural Keeper(s) surplus beyond team quota; placed in general pool.", len(surplus)))
	}

	// 5. Allocate Outfield Positions: Defenders, Midfielders, Strikers, then Overflow
	type positionGroup struct {
		position domain.Position
		target   int
		players  []domain.Player
	}

	groups := []positionGroup{
		{position: domain.PositionDefender, target: template.Defenders, players: defenders},
		{position: domain.PositionMidfielder, target: template.Midfielders, players: midfielders},
		{position: domain.PositionStriker, target: template.Strikers, players: strikers},
		{position: domain.PositionMidfielder, target: 0, players: overflow},
	}

	var substitutes []domain.Player

	for _, group := range groups {
		for _, player := range group.players {
			if assignedIDs[player.ID] {
				continue
			}

			// Find best team for this player
			bestTeamIdx := -1
			maxDeficit := -9999
			minTotalPlayers := 9999

			for i := 0; i < teamCount; i++ {
				if len(teams[i].Players) >= teams[i].TargetSize {
					continue // Team is full
				}

				currentCount := teams[i].PositionCounts[group.position]
				deficit := group.target - currentCount

				if deficit > maxDeficit {
					maxDeficit = deficit
					bestTeamIdx = i
					minTotalPlayers = len(teams[i].Players)
				} else if deficit == maxDeficit && len(teams[i].Players) < minTotalPlayers {
					bestTeamIdx = i
					minTotalPlayers = len(teams[i].Players)
				}
			}

			if bestTeamIdx != -1 {
				assignedPos := player.PrimaryPosition
				// If player is a Keeper but the team already has a starting Keeper, assign them to outfield
				if player.PrimaryPosition == domain.PositionKeeper && teams[bestTeamIdx].HasNaturalKeeper {
					assignedPos = domain.PositionMidfielder
				}

				teams[bestTeamIdx].Players = append(teams[bestTeamIdx].Players, AssignedPlayer{
					Player:           player,
					AssignedPosition: assignedPos,
				})
				teams[bestTeamIdx].PositionCounts[assignedPos]++
				assignedIDs[player.ID] = true
			} else {
				// No team has capacity; player is a substitute
				substitutes = append(substitutes, player)
			}
		}
	}

	// 6. Sort Players in each Team for uniform, clean presentation (GK -> DEF -> MID -> STR)
	for i := 0; i < teamCount; i++ {
		sort.SliceStable(teams[i].Players, func(a, b int) bool {
			return positionRank(teams[i].Players[a].AssignedPosition) < positionRank(teams[i].Players[b].AssignedPosition)
		})
	}

	// 7. Invariant Checks
	totalAssigned := 0
	for _, t := range teams {
		totalAssigned += len(t.Players)
	}

	if totalAssigned+len(substitutes) != totalAttendees {
		return nil, fmt.Errorf("invariant violation: assigned (%d) + substitutes (%d) != total attendees (%d)",
			totalAssigned, len(substitutes), totalAttendees)
	}

	return &MatchmakingResult{
		Format:           req.Format,
		SubPolicy:        subPolicy,
		Teams:            teams,
		Substitutes:      substitutes,
		Warnings:         warnings,
		TotalAttendees:   totalAttendees,
		AssignedCount:    totalAssigned,
		SubstitutesCount: len(substitutes),
	}, nil
}

func positionRank(pos domain.Position) int {
	switch pos {
	case domain.PositionKeeper:
		return 1
	case domain.PositionDefender:
		return 2
	case domain.PositionMidfielder:
		return 3
	case domain.PositionStriker:
		return 4
	default:
		return 5
	}
}
