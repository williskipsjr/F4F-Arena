package matchmaker

import "github.com/williskipsjr/f4f-arena/backend/internal/domain"

type PositionalTemplate struct {
	Format      domain.MatchFormat
	TeamSize    int
	Keepers     int
	Defenders   int
	Midfielders int
	Strikers    int
}

var (
	Template5v5 = PositionalTemplate{
		Format:      domain.FormatFiveASide,
		TeamSize:    5,
		Keepers:     1,
		Defenders:   2,
		Midfielders: 1,
		Strikers:    1,
	}

	Template6v6 = PositionalTemplate{
		Format:      domain.FormatSixASide,
		TeamSize:    6,
		Keepers:     1,
		Defenders:   2,
		Midfielders: 2,
		Strikers:    1,
	}

	Template7v7 = PositionalTemplate{
		Format:      domain.FormatSevenASide,
		TeamSize:    7,
		Keepers:     1,
		Defenders:   3,
		Midfielders: 2,
		Strikers:    1,
	}

	Template8v8 = PositionalTemplate{
		Format:      domain.FormatEightASide,
		TeamSize:    8,
		Keepers:     1,
		Defenders:   3,
		Midfielders: 3,
		Strikers:    1,
	}

	Template9v9 = PositionalTemplate{
		Format:      domain.FormatNineASide,
		TeamSize:    9,
		Keepers:     1,
		Defenders:   3,
		Midfielders: 3,
		Strikers:    2,
	}

	Template10v10 = PositionalTemplate{
		Format:      domain.FormatTenASide,
		TeamSize:    10,
		Keepers:     1,
		Defenders:   4,
		Midfielders: 3,
		Strikers:    2,
	}

	Template11v11 = PositionalTemplate{
		Format:      domain.FormatElevenASide,
		TeamSize:    11,
		Keepers:     1,
		Defenders:   4,
		Midfielders: 4,
		Strikers:    2,
	}

	DefaultBibColors = []string{
		"Orange",
		"Green",
		"Blue",
		"White",
		"Yellow",
		"Red",
		"Black",
		"Purple",
	}
)

func GetTemplate(format domain.MatchFormat) PositionalTemplate {
	switch format {
	case domain.FormatFiveASide:
		return Template5v5
	case domain.FormatSixASide:
		return Template6v6
	case domain.FormatSevenASide:
		return Template7v7
	case domain.FormatEightASide:
		return Template8v8
	case domain.FormatNineASide:
		return Template9v9
	case domain.FormatTenASide:
		return Template10v10
	case domain.FormatElevenASide:
		return Template11v11
	default:
		return Template7v7
	}
}

// RecommendFormat chooses the best match format for playerCount, prioritizing
// even team counts (2, 4, 6, ...) and minimal leftover substitutes.
func RecommendFormat(playerCount int) domain.MatchFormat {
	if playerCount < 10 {
		return domain.FormatFiveASide
	}

	// Preference order for tie-breaking (7v7 and 6v6 most common for turf, then 8v8, 5v5, 9v9, 10v10, 11v11)
	candidates := []domain.MatchFormat{
		domain.FormatSevenASide,
		domain.FormatSixASide,
		domain.FormatEightASide,
		domain.FormatFiveASide,
		domain.FormatNineASide,
		domain.FormatTenASide,
		domain.FormatElevenASide,
	}

	bestFormat := domain.FormatSevenASide
	bestRemainder := playerCount + 1
	bestTeams := 0

	for _, fmt := range candidates {
		size := fmt.PlayersPerTeam()
		teams := playerCount / size
		if teams < 2 {
			continue
		}
		// Enforce even team count when possible (e.g., 29 -> 4 teams of 7 instead of 3 teams of 9)
		if teams%2 == 1 {
			teams--
		}
		if teams < 2 {
			continue
		}
		rem := playerCount - (teams * size)
		if rem < bestRemainder || (rem == bestRemainder && teams > bestTeams) {
			bestRemainder = rem
			bestFormat = fmt
			bestTeams = teams
		}
	}

	return bestFormat
}

func GetBibColor(teamIndex int) string {
	if teamIndex < len(DefaultBibColors) {
		return DefaultBibColors[teamIndex]
	}
	return ""
}
