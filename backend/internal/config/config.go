package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	ServerPort           string
	ServerEnv            string
	DatabaseURL          string
	AdminUsername        string
	AdminPassword        string
	JWTSecret            string
	TokenExpiryHours     time.Duration
	LatestAppVersionCode int
	LatestAppVersionName string
	AppDownloadURL       string
	AppReleaseNotes      string
}

func Load() *Config {
	dbHost := getEnv("POSTGRES_HOST", "localhost")
	dbPort := getEnv("POSTGRES_PORT", "5432")
	dbUser := getEnv("POSTGRES_USER", "f4f_admin")
	dbPass := getEnv("POSTGRES_PASSWORD", "f4f_secure_password")
	dbName := getEnv("POSTGRES_DB", "f4f_arena")
	sslMode := getEnv("POSTGRES_SSLMODE", "disable")

	dbURL := getEnv("DATABASE_URL", "")
	if dbURL == "" {
		dbURL = "postgres://" + dbUser + ":" + dbPass + "@" + dbHost + ":" + dbPort + "/" + dbName + "?sslmode=" + sslMode
	}

	expiryHours, err := strconv.Atoi(getEnv("TOKEN_EXPIRY_HOURS", "24"))
	if err != nil {
		expiryHours = 24
	}

	adminPassphrase := getEnv("ADMIN_PASSPHRASE", "")
	if adminPassphrase == "" {
		adminPassphrase = getEnv("ADMIN_PASSWORD_HASH", "f4f_arena_secret_passphrase")
	}

	latestVersionCode, err := strconv.Atoi(getEnv("APP_LATEST_VERSION_CODE", "1"))
	if err != nil || latestVersionCode < 1 {
		latestVersionCode = 1
	}

	return &Config{
		ServerPort:           getEnv("SERVER_PORT", "8080"),
		ServerEnv:            getEnv("SERVER_ENV", "development"),
		DatabaseURL:          dbURL,
		AdminUsername:        getEnv("ADMIN_USERNAME", "admin"),
		AdminPassword:        adminPassphrase,
		JWTSecret:            getEnv("JWT_SECRET", "f4f_arena_dev_jwt_secret_key_32bytes_min!"),
		TokenExpiryHours:     time.Duration(expiryHours) * time.Hour,
		LatestAppVersionCode: latestVersionCode,
		LatestAppVersionName: getEnv("APP_LATEST_VERSION_NAME", "1.0.0"),
		AppDownloadURL:       getEnv("APP_DOWNLOAD_URL", ""),
		AppReleaseNotes:      getEnv("APP_RELEASE_NOTES", ""),
	}
}

func getEnv(key, defaultVal string) string {
	if val, exists := os.LookupEnv(key); exists && val != "" {
		return val
	}
	return defaultVal
}
