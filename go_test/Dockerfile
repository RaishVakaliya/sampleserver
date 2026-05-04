# Use the official Golang image as the build stage
FROM golang:1.21-alpine AS builder

# Set the working directory
WORKDIR /app

# Copy the go.mod file from the root
COPY go.mod ./

# Download dependencies (if any)
RUN go mod download

# Copy the entire project
COPY . .

# Build the Go worker binary
# We point to the specific file in go-worker directory
RUN go build -o pulse-worker go-worker/ping_worker.go

# Use a tiny alpine image for the final run stage to keep it small
FROM alpine:latest

WORKDIR /root/

# Copy the binary from the builder stage
COPY --from=builder /app/pulse-worker .

# Run the worker
CMD ["./pulse-worker"]
