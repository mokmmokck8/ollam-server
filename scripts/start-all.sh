#!/usr/bin/env bash
set -euo pipefail

# Starts:
# 1) PaddleOCR via docker-compose
# 2) Ollama locally (macOS app/service) OR via `ollama serve` fallback if CLI exists
# 3) Spring Boot via Maven Wrapper

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() { printf "[%s] %s\n" "$(date +"%H:%M:%S")" "$*"; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1
}

wait_http() {
  local url="$1"
  local name="$2"
  local timeout_sec="${3:-60}"

  local start_ts
  start_ts="$(date +%s)"

  while true; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "OK: $name is reachable ($url)"
      return 0
    fi

    local now
    now="$(date +%s)"
    if (( now - start_ts >= timeout_sec )); then
      log "ERROR: Timed out waiting for $name ($url)"
      return 1
    fi

    sleep 2
  done
}

log "Starting PaddleOCR (docker-compose)..."
if ! require_cmd docker; then
  log "ERROR: docker is not installed"
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  log "ERROR: Docker daemon not running. Start Docker Desktop first."
  exit 1
fi

docker-compose up -d --build

# PaddleOCR container may take a while to be ready.
wait_http "http://localhost:8866/health" "PaddleOCR" 120

log "Ensuring Ollama is running on http://localhost:11434 ..."
if curl -fsS "http://localhost:11434/api/tags" >/dev/null 2>&1; then
  log "OK: Ollama already reachable"
else
  if require_cmd ollama; then
    log "Ollama not reachable yet; starting 'ollama serve' in background..."
    mkdir -p "$ROOT_DIR/.run"
    # Start Ollama server in background and record PID
    (nohup ollama serve >"$ROOT_DIR/.run/ollama.log" 2>&1 & echo $! >"$ROOT_DIR/.run/ollama.pid")

    # give it a moment
    wait_http "http://localhost:11434/api/tags" "Ollama" 60
  else
    log "ERROR: Ollama is not reachable and 'ollama' CLI isn't installed."
    log "Install/start Ollama (macOS app) so it listens on http://localhost:11434, then re-run."
    exit 1
  fi
fi

log "Starting Spring Boot (./mvnw spring-boot:run) in background..."
mkdir -p "$ROOT_DIR/.run"

# If already running, don't start a second copy
if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
  log "Port 8080 is already in use. Assuming Spring Boot is already running; skipping start."
else
  (nohup ./mvnw spring-boot:run >"$ROOT_DIR/.run/spring.log" 2>&1 & echo $! >"$ROOT_DIR/.run/spring.pid")
fi

# Basic readiness: spring actuator isn't configured, so just check TCP port.
# (curling / may 404 depending on controller mappings)
log "Waiting for Spring Boot to listen on :8080 ..."
start_ts="$(date +%s)"
while true; do
  if lsof -nP -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
    log "OK: Spring Boot is listening on :8080"
    break
  fi
  now="$(date +%s)"
  if (( now - start_ts >= 120 )); then
    log "ERROR: Timed out waiting for Spring Boot to listen on :8080"
    log "Check logs: tail -n 200 .run/spring.log"
    exit 1
  fi
  sleep 2
done

log "All services started."
log "- PaddleOCR: http://localhost:8866"
log "- Ollama:    http://localhost:11434"
log "- Spring:    http://localhost:8080"
log "Logs:"
log "- .run/spring.log"
log "- .run/ollama.log (only if started by this script)"
