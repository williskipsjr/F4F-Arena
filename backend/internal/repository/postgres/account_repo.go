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
	age *int,
) (*PlayerAccountInfo, error) {
	trimmedName := strings.TrimSpace(displayName)
	cleanPhone := normalizePhone(phone)
	trimmedUser := strings.ToLower(strings.TrimSpace(username))
	if trimmedUser == "" && len(cleanPhone) >= 10 {
		trimmedUser = cleanPhone[len(cleanPhone)-10:]
	} else if trimmedUser == "" && trimmedName != "" {
		trimmedUser = strings.ToLower(strings.ReplaceAll(trimmedName, " ", "_"))
	}
	if trimmedName == "" || trimmedUser == "" || len(password) < 4 {
		return nil, errors.New("name, phone number, and 4-digit PIN are required")
	}
	if len(cleanPhone) < 10 {
		return nil, errors.New("valid phone number (at least 10 digits) is required")
	}
	if age == nil || *age < 10 || *age > 80 {
		return nil, errors.New("valid age (10–80) is required")
	}

	// Only Admin assigns/edits player positions; new self-registered players default to MIDFIELDER
	defaultPosition := domain.PositionMidfielder

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
	err = tx.QueryRow(ctx, "SELECT id FROM player_accounts WHERE username = $1 OR phone = $2", trimmedUser, cleanPhone).Scan(&existingID)
	if err == nil {
		return nil, ErrUsernameTaken
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return nil, fmt.Errorf("failed checking username: %w", err)
	}

	var playerID string
	now := time.Now().UTC()
	err = tx.QueryRow(ctx, `
		INSERT INTO players (display_name, primary_position, age, is_active, created_at, updated_at)
		VALUES ($1, $2, $3, true, $4, $4)
		RETURNING id
	`, trimmedName, string(defaultPosition), age, now).Scan(&playerID)
	if err != nil {
		return nil, fmt.Errorf("failed creating player profile: %w", err)
	}

	phonePtr := &cleanPhone

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
	cleanPhone := normalizePhone(username)
	if trimmedUser == "" || password == "" {
		return nil, ErrInvalidCredentials
	}

	var info PlayerAccountInfo
	var storedHash string
	err := r.pool.QueryRow(ctx, `
		SELECT pa.id, pa.player_id, pa.username, p.display_name, pa.role, pa.password_hash
		FROM player_accounts pa
		JOIN players p ON p.id = pa.player_id
		WHERE pa.username = $1 OR ($2 != '' AND RIGHT(COALESCE(pa.phone, ''), 10) = RIGHT($2, 10))
		LIMIT 1
	`, trimmedUser, cleanPhone).Scan(&info.ID, &info.PlayerID, &info.Username, &info.DisplayName, &info.Role, &storedHash)
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

// ResetPinByPhone verifies username + registered phone number and updates the player's PIN/password.
func (r *AccountRepository) ResetPinByPhone(ctx context.Context, username, phone, newPassword string) error {
	trimmedUser := strings.ToLower(strings.TrimSpace(username))
	cleanPhone := normalizePhone(phone)
	if trimmedUser == "" || len(cleanPhone) < 10 || len(newPassword) < 4 {
		return errors.New("username, registered phone number (10+ digits), and new PIN (min 4 chars) are required")
	}

	var accountID string
	var storedPhone *string
	err := r.pool.QueryRow(ctx, `
		SELECT id, phone FROM player_accounts WHERE username = $1
	`, trimmedUser).Scan(&accountID, &storedPhone)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ErrInvalidCredentials
		}
		return fmt.Errorf("failed looking up account: %w", err)
	}

	if storedPhone == nil {
		return errors.New("no phone number registered for this account; contact admin")
	}
	storedDigits := normalizePhone(*storedPhone)
	// Compare last 10 digits so +91 / 0 prefixes match seamlessly
	if len(storedDigits) < 10 || storedDigits[len(storedDigits)-10:] != cleanPhone[len(cleanPhone)-10:] {
		return errors.New("phone number does not match this account")
	}

	newHash, err := hashPassword(newPassword)
	if err != nil {
		return fmt.Errorf("failed hashing new PIN: %w", err)
	}

	_, err = r.pool.Exec(ctx, `
		UPDATE player_accounts SET password_hash = $1, updated_at = NOW() WHERE id = $2
	`, newHash, accountID)
	if err != nil {
		return fmt.Errorf("failed updating PIN: %w", err)
	}
	return nil
}

// VerifyAdminPassword checks a DB-stored ADMIN hash first, falling back to envFallback
// only when no DB admin password has been set yet.
func (r *AccountRepository) VerifyAdminPassword(ctx context.Context, username, password, envFallback string) (string, bool, error) {
	trimmedUser := strings.ToLower(strings.TrimSpace(username))
	if trimmedUser == "" || password == "" {
		return "", false, nil
	}

	var playerID, storedHash string
	err := r.pool.QueryRow(ctx, `
		SELECT player_id, password_hash
		FROM player_accounts
		WHERE username = $1 AND role = 'ADMIN'
		LIMIT 1
	`, trimmedUser).Scan(&playerID, &storedHash)
	if err == nil {
		return playerID, verifyPassword(password, storedHash), nil
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return "", false, err
	}

	if envFallback == "" || subtle.ConstantTimeCompare([]byte(password), []byte(envFallback)) != 1 {
		return "", false, nil
	}
	pid, err := r.EnsureAdminPlayer(ctx, username)
	if err != nil {
		return "", true, nil
	}
	return pid, true, nil
}

// ChangePassword updates the authenticated user's password/PIN in player_accounts.
func (r *AccountRepository) ChangePassword(ctx context.Context, username, role, currentPassword, newPassword, envFallback string) error {
	trimmedUser := strings.ToLower(strings.TrimSpace(username))
	minLen := 4
	if strings.EqualFold(role, "ADMIN") {
		minLen = 6
	}
	if trimmedUser == "" || currentPassword == "" || len(newPassword) < minLen {
		return fmt.Errorf("current password and new password (min %d characters) are required", minLen)
	}

	var accountID, storedHash string
	err := r.pool.QueryRow(ctx, `
		SELECT id, password_hash FROM player_accounts WHERE username = $1 LIMIT 1
	`, trimmedUser).Scan(&accountID, &storedHash)

	now := time.Now().UTC()
	newHash, hashErr := hashPassword(newPassword)
	if hashErr != nil {
		return fmt.Errorf("failed securing new password: %w", hashErr)
	}

	if err == nil {
		if !verifyPassword(currentPassword, storedHash) {
			return ErrInvalidCredentials
		}
		_, updErr := r.pool.Exec(ctx, `
			UPDATE player_accounts SET password_hash = $1, updated_at = $2 WHERE id = $3
		`, newHash, now, accountID)
		return updErr
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return err
	}

	if !strings.EqualFold(role, "ADMIN") || envFallback == "" ||
		subtle.ConstantTimeCompare([]byte(currentPassword), []byte(envFallback)) != 1 {
		return ErrInvalidCredentials
	}

	pid, pidErr := r.EnsureAdminPlayer(ctx, username)
	if pidErr != nil {
		return fmt.Errorf("failed resolving admin profile: %w", pidErr)
	}
	_, insErr := r.pool.Exec(ctx, `
		INSERT INTO player_accounts (player_id, username, password_hash, role, created_at, updated_at)
		VALUES ($1, $2, $3, 'ADMIN', $4, $4)
	`, pid, trimmedUser, newHash, now)
	return insErr
}

// ClaimProfile verifies that a pre-seeded player's phone matches and creates their 4-digit PIN login.
func (r *AccountRepository) ClaimProfile(ctx context.Context, playerID, phone, pin string) (*PlayerAccountInfo, error) {
	cleanPhone := normalizePhone(phone)
	if strings.TrimSpace(playerID) == "" || len(cleanPhone) < 10 || len(pin) < 4 {
		return nil, errors.New("player_id, valid phone number (10+ digits), and 4-digit PIN are required")
	}

	var displayName string
	var prePhone *string
	err := r.pool.QueryRow(ctx, `
		SELECT display_name, pre_phone FROM players WHERE id = $1
	`, strings.TrimSpace(playerID)).Scan(&displayName, &prePhone)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, errors.New("player profile not found")
		}
		return nil, err
	}

	if prePhone == nil || normalizePhone(*prePhone) == "" {
		return nil, errors.New("this profile has no pre-registered phone number; use Sign Up instead")
	}
	preDigits := normalizePhone(*prePhone)
	if len(preDigits) < 10 || preDigits[len(preDigits)-10:] != cleanPhone[len(cleanPhone)-10:] {
		return nil, errors.New("phone number does not match our community roster")
	}

	var existingAcc string
	err = r.pool.QueryRow(ctx, `SELECT id FROM player_accounts WHERE player_id = $1`, playerID).Scan(&existingAcc)
	if err == nil {
		return nil, errors.New("this profile is already claimed — please Sign In")
	}
	if !errors.Is(err, pgx.ErrNoRows) {
		return nil, err
	}

	username := cleanPhone[len(cleanPhone)-10:]
	hash, err := hashPassword(pin)
	if err != nil {
		return nil, err
	}
	now := time.Now().UTC()
	var accountID string
	err = r.pool.QueryRow(ctx, `
		INSERT INTO player_accounts (player_id, username, phone, password_hash, role, created_at, updated_at)
		VALUES ($1, $2, $3, $4, 'PLAYER', $5, $5)
		RETURNING id
	`, playerID, username, &cleanPhone, hash, now).Scan(&accountID)
	if err != nil {
		return nil, fmt.Errorf("failed claiming profile: %w", err)
	}

	return &PlayerAccountInfo{
		ID:          accountID,
		PlayerID:    playerID,
		Username:    username,
		DisplayName: displayName,
		Role:        "PLAYER",
	}, nil
}

type PhoneLookupResult struct {
	Found       bool   `json:"found"`
	PlayerID    string `json:"player_id,omitempty"`
	DisplayName string `json:"display_name,omitempty"`
	Claimed     bool   `json:"claimed"`
}

// LookupByPhone finds a community player by the last 10 digits of their phone number.
func (r *AccountRepository) LookupByPhone(ctx context.Context, phone string) (*PhoneLookupResult, error) {
	clean := normalizePhone(phone)
	if len(clean) < 10 {
		return &PhoneLookupResult{Found: false}, nil
	}
	last10 := clean[len(clean)-10:]

	var playerID, displayName string
	var accountID *string
	err := r.pool.QueryRow(ctx, `
		SELECT p.id, p.display_name, pa.id
		FROM players p
		LEFT JOIN player_accounts pa ON pa.player_id = p.id
		WHERE RIGHT(REGEXP_REPLACE(COALESCE(p.pre_phone, ''), '[^0-9]', '', 'g'), 10) = $1
		   OR RIGHT(REGEXP_REPLACE(COALESCE(pa.phone, ''), '[^0-9]', '', 'g'), 10) = $1
		LIMIT 1
	`, last10).Scan(&playerID, &displayName, &accountID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return &PhoneLookupResult{Found: false}, nil
		}
		return nil, err
	}

	return &PhoneLookupResult{
		Found:       true,
		PlayerID:    playerID,
		DisplayName: displayName,
		Claimed:     accountID != nil,
	}, nil
}

// UpdateOwnProfile updates the authenticated user's display_name and/or avatar_url.
func (r *AccountRepository) UpdateOwnProfile(ctx context.Context, playerID string, displayName, avatarURL *string) (string, *string, error) {
	if strings.TrimSpace(playerID) == "" {
		return "", nil, errors.New("no player profile linked to this account")
	}
	var currentName string
	var currentAvatar *string
	err := r.pool.QueryRow(ctx, `SELECT display_name, avatar_url FROM players WHERE id = $1`, playerID).Scan(&currentName, &currentAvatar)
	if err != nil {
		return "", nil, err
	}
	if displayName != nil && strings.TrimSpace(*displayName) != "" {
		currentName = strings.TrimSpace(*displayName)
	}
	if avatarURL != nil {
		currentAvatar = avatarURL
	}
	_, err = r.pool.Exec(ctx, `
		UPDATE players SET display_name = $1, avatar_url = $2, updated_at = NOW() WHERE id = $3
	`, currentName, currentAvatar, playerID)
	if err != nil {
		return "", nil, err
	}
	return currentName, currentAvatar, nil
}

func normalizePhone(raw string) string {
	var b strings.Builder
	for _, ch := range raw {
		if ch >= '0' && ch <= '9' {
			b.WriteRune(ch)
		}
	}
	return b.String()
}
