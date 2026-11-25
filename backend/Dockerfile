# Multi-stage lightweight build for F4F Arena API & Migrate CLI
FROM golang:1.25-alpine AS builder

WORKDIR /app

# Download dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Compile API and Migrate binaries statically
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o f4f-arena-api cmd/api/main.go && \
    CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o f4f-arena-migrate cmd/migrate/main.go

# Production alpine image
FROM alpine:3.19

RUN apk --no-cache add ca-certificates tzdata wget && \
    adduser -D -H -s /sbin/nologin f4f

WORKDIR /app

COPY --from=builder /app/f4f-arena-api .
COPY --from=builder /app/f4f-arena-migrate .
COPY --from=builder /app/migrations ./migrations

RUN chown -R f4f:f4f /app
USER f4f

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/health || exit 1

CMD ["./f4f-arena-api"]
