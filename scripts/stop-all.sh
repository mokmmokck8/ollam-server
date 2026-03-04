#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() { printf "[%s] %s\n" "$(date +"%H:%M:%S")" "$*"; }

stop_pid_file() {
  local pid_file="$1"
  local name="$2"

  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file" || true)"
    if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
      log "Stopping $name (pid=$pid)..."
      kill "$pid" || true
    fi
    rm -f "$pid_file"
  fi
}

log "Stopping Spring Boot + Ollama (if started by scripts)..."
stop_pid_file "$ROOT_DIR/.run/spring.pid" "Spring Boot"
stop_pid_file "$ROOT_DIR/.run/ollama.pid" "Ollama"

log "Stopping PaddleOCR (docker-compose down)..."
docker-compose down || true

log "Done."
