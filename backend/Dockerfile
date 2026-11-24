# Multi-stage lightweight build for F4F Arena API
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Download dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Compile binary statically
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o f4f-arena-api cmd/api/main.go

# Production scratch/alpine image
FROM alpine:3.19

RUN apk --no-cache add ca-certificates tzdata

WORKDIR /app

COPY --from=builder /app/f4f-arena-api .
COPY --from=builder /app/migrations ./migrations

EXPOSE 8080

CMD ["./f4f-arena-api"]
