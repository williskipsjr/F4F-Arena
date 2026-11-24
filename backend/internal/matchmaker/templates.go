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

	Template9v9 = PositionalTemplate{
		Format:      domain.FormatNineASide,
		TeamSize:    9,
		Keepers:     1,
		Defenders:   3,
		Midfielders: 3,
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
	case domain.FormatSixASide:
		return Template6v6
	case domain.FormatSevenASide:
		return Template7v7
	case domain.FormatNineASide:
		return Template9v9
	default:
		return Template7v7
	}
}

func GetBibColor(teamIndex int) string {
	if teamIndex < len(DefaultBibColors) {
		return DefaultBibColors[teamIndex]
	}
	return ""
}
