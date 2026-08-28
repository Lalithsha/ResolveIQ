#!/usr/bin/env bash
set -euo pipefail

echo "========================================================"
echo " Starting ResolveIQ Local Development Environment"
echo "========================================================"

# Check for Docker
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is required but not installed." >&2
    exit 1
fi

echo "1. Starting core infrastructure containers (PostgreSQL, Kafka, MinIO)..."
docker compose up -d postgres kafka minio

echo "2. Building Java microservices..."
./mvnw clean compile

echo "3. Starting backend services..."
echo "Run individual services or use Docker profiles:"
echo "  ./mvnw spring-boot:run -pl discovery-service"
echo "  ./mvnw spring-boot:run -pl api-gateway"
echo "  ./mvnw spring-boot:run -pl auth-service"
echo "  ./mvnw spring-boot:run -pl ticket-service"
echo "  ./mvnw spring-boot:run -pl ai-orchestration-service"
echo "  ./mvnw spring-boot:run -pl ai-analysis-service"
echo "  ./mvnw spring-boot:run -pl routing-service"
echo "  ./mvnw spring-boot:run -pl rag-service"

echo ""
echo "4. Frontend is ready in ./frontend"
echo "  npm --prefix frontend run dev"
echo "========================================================"
