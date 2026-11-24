package auth

import (
	"context"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

type contextKey string

const AdminContextKey contextKey = "admin_claims"

type Claims struct {
	Username string `json:"username"`
	Role     string `json:"role"`
	jwt.RegisteredClaims
}

type TokenService struct {
	secret []byte
	expiry time.Duration
}

func NewTokenService(secret string, expiry time.Duration) *TokenService {
	return &TokenService{
		secret: []byte(secret),
		expiry: expiry,
	}
}

func (s *TokenService) GenerateToken(username string) (string, time.Time, error) {
	expiresAt := time.Now().Add(s.expiry)
	claims := &Claims{
		Username: username,
		Role:     "ADMIN",
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(expiresAt),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
			Issuer:    "f4f-arena-api",
			Subject:   username,
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString(s.secret)
	if err != nil {
		return "", time.Time{}, err
	}

	return tokenString, expiresAt, nil
}

func (s *TokenService) ValidateToken(tokenString string) (*Claims, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return s.secret, nil
	})

	if err != nil {
		return nil, err
	}

	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid {
		return nil, errors.New("invalid token")
	}

	return claims, nil
}

func (s *TokenService) Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			http.Error(w, `{"error":{"code":"UNAUTHORIZED","message":"Authorization header required"}}`, http.StatusUnauthorized)
			return
		}

		parts := strings.Split(authHeader, " ")
		if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
			http.Error(w, `{"error":{"code":"UNAUTHORIZED","message":"Invalid token format. Expected 'Bearer <token>'"}}`, http.StatusUnauthorized)
			return
		}

		claims, err := s.ValidateToken(parts[1])
		if err != nil {
			http.Error(w, `{"error":{"code":"UNAUTHORIZED","message":"Token expired or invalid"}}`, http.StatusUnauthorized)
			return
		}

		ctx := context.WithValue(r.Context(), AdminContextKey, claims)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
