package postgres

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/williskipsjr/f4f-arena/backend/internal/domain"
)

var (
	ErrUsernameTaken      = errors.New("username is already taken")
	ErrInvalidCredentials = errors.New("invalid username or password")
)

type PlayerAccountInfo struct {
	ID          string
	PlayerID    string
	Username    string
	DisplayName string
	Role        string
}

type AccountRepository struct {
	pool *pgxpool.Pool
}

func NewAccountRepository(pool *pgxpool.Pool) *AccountRepository {
	return &AccountRepository{pool: pool}
}

func (r *AccountRepository) RegisterPlayer(
	ctx context.Context,
	displayName, username, phone, password string,
	position domain.Position,
	claimedPlayerID *string,
) (*PlayerAccountInfo, error) {
	trimmedName := strings.TrimSpace(displayName)
	trimmedUser := strings.ToLower(strings.TrimSpace(username))
	if trimmedUser == "" && trimmedName != "" {
		trimmedUser = strings.ToLower(strings.ReplaceAll(trimmedName, " ", "_"))
	}
	if trimmedName == "" || trimmedUser == "" || len(password) < 4 {
		return nil, errors.New("display_name, username, and PIN/password (min 4 chars) are required")
	}

	if !position.IsValid() {
		position = domain.PositionMidfielder
	}

	hash, err := hashPassword(password)
	if err != nil {
		return nil, fmt.Errorf("failed hashing password: %w", err)
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed starting transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	var existingID string
	err = tx.QueryRow(ctx, "SELECT id FROM player_accounts WHERE username = $1", trimmedUser).Scan(&existingID)
	if err == nil {
		return nil, ErrUsernameTaken
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return nil, fmt.Errorf("failed checking username: %w", err)
	}

	// Link to claimed player_id if provided & unclaimed, else match display_name, else create new player
	var playerID string
	now := time.Now().UTC()
	if claimedPlayerID != nil && strings.TrimSpace(*claimedPlayerID) != "" {
		cid := strings.TrimSpace(*claimedPlayerID)
		var existingAcc string
		err = tx.QueryRow(ctx, "SELECT id FROM player_accounts WHERE player_id = $1", cid).Scan(&existingAcc)
		if err == nil {
			return nil, errors.New("this player profile has already been claimed — please sign in")
		}
		if !errors.Is(err, pgx.ErrNoRows) {
			return nil, fmt.Errorf("failed checking claimed player: %w", err)
		}
		err = tx.QueryRow(ctx, "SELECT id, display_name FROM players WHERE id = $1", cid).Scan(&playerID, &trimmedName)
		if err != nil {
			return nil, fmt.Errorf("selected player not found")
		}
	} else {
		err = tx.QueryRow(ctx, `
			SELECT p.id
			FROM players p
			LEFT JOIN player_accounts pa ON pa.player_id = p.id
			WHERE LOWER(p.display_name) = LOWER($1) AND pa.id IS NULL
			LIMIT 1
		`, trimmedName).Scan(&playerID)

		if errors.Is(err, pgx.ErrNoRows) {
			err = tx.QueryRow(ctx, `
				INSERT INTO players (display_name, primary_position, is_active, created_at, updated_at)
				VALUES ($1, $2, true, $3, $3)
				RETURNING id
			`, trimmedName, string(position), now).Scan(&playerID)
			if err != nil {
				return nil, fmt.Errorf("failed creating player profile: %w", err)
			}
		} else if err != nil {
			return nil, fmt.Errorf("failed looking up player profile: %w", err)
		}
	}

	var phonePtr *string
	if strings.TrimSpace(phone) != "" {
		p := strings.TrimSpace(phone)
		phonePtr = &p
	}

	var accountID string
	err = tx.QueryRow(ctx, `
		INSERT INTO player_accounts (player_id, username, phone, password_hash, role, created_at, updated_at)
		VALUES ($1, $2, $3, $4, 'PLAYER', $5, $5)
		RETURNING id
	`, playerID, trimmedUser, phonePtr, hash, now).Scan(&accountID)
	if err != nil {
		return nil, fmt.Errorf("failed creating player account: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, fmt.Errorf("failed committing registration: %w", err)
	}

	return &PlayerAccountInfo{
		ID:          accountID,
		PlayerID:    playerID,
		Username:    trimmedUser,
		DisplayName: trimmedName,
		Role:        "PLAYER",
	}, nil
}

func (r *AccountRepository) Authenticate(ctx context.Context, username, password string) (*PlayerAccountInfo, error) {
	trimmedUser := strings.ToLower(strings.TrimSpace(username))
	if trimmedUser == "" || password == "" {
		return nil, ErrInvalidCredentials
	}

	var info PlayerAccountInfo
	var storedHash string
	err := r.pool.QueryRow(ctx, `
		SELECT pa.id, pa.player_id, pa.username, p.display_name, pa.role, pa.password_hash
		FROM player_accounts pa
		JOIN players p ON p.id = pa.player_id
		WHERE pa.username = $1
	`, trimmedUser).Scan(&info.ID, &info.PlayerID, &info.Username, &info.DisplayName, &info.Role, &storedHash)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrInvalidCredentials
		}
		return nil, fmt.Errorf("failed querying account: %w", err)
	}

	if !verifyPassword(password, storedHash) {
		return nil, ErrInvalidCredentials
	}

	return &info, nil
}

// EnsureAdminPlayer returns an existing player ID for the admin or creates one so the admin can also vote IN/OUT.
func (r *AccountRepository) EnsureAdminPlayer(ctx context.Context, adminName string) (string, error) {
	name := strings.TrimSpace(adminName)
	if name == "" {
		name = "Admin"
	}
	var playerID string
	err := r.pool.QueryRow(ctx, `
		SELECT id FROM players WHERE LOWER(display_name) = LOWER($1) LIMIT 1
	`, name).Scan(&playerID)
	if err == nil {
		return playerID, nil
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return "", err
	}

	now := time.Now().UTC()
	err = r.pool.QueryRow(ctx, `
		INSERT INTO players (display_name, primary_position, is_active, notes, created_at, updated_at)
		VALUES ($1, 'MIDFIELDER', true, 'Community Admin', $2, $2)
		RETURNING id
	`, name, now).Scan(&playerID)
	if err != nil {
		return "", err
	}
	return playerID, nil
}

func hashPassword(password string) (string, error) {
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return "", err
	}
	saltHex := hex.EncodeToString(salt)
	sum := sha256.Sum256([]byte(saltHex + ":" + password))
	return saltHex + "$" + hex.EncodeToString(sum[:]), nil
}

func verifyPassword(password, stored string) bool {
	parts := strings.SplitN(stored, "$", 2)
	if len(parts) != 2 {
		return false
	}
	sum := sha256.Sum256([]byte(parts[0] + ":" + password))
	computed := hex.EncodeToString(sum[:])
	return subtle.ConstantTimeCompare([]byte(computed), []byte(parts[1])) == 1
}
