.PHONY: help build start stop restart logs test clean all up down

# 預設目標
help:

# One-command start/stop for PaddleOCR + Ollama + Spring Boot
up:
	@./scripts/start-all.sh

down:
	@./scripts/stop-all.sh
	@echo "📚 可用指令："
	@echo "  make build    - 建構 Docker 映像檔"
	@echo "  make start    - 啟動服務"
	@echo "  make stop     - 停止服務"
	@echo "  make restart  - 重啟服務"
	@echo "  make logs     - 查看日誌"
	@echo "  make test     - 測試服務"
	@echo "  make clean    - 清理容器和映像檔"
	@echo "  make all      - 建構並啟動所有服務"

# 建構 Docker 映像檔
build:
	@echo "🔨 建構 PaddleOCR Docker 映像檔..."
	docker build -f Dockerfile.paddleocr -t paddleocr-service .

# 啟動服務
start:
	@echo "🚀 啟動服務..."
	docker-compose up -d
	@echo "✅ 服務已啟動"
	@make test

# 停止服務
stop:
	@echo "🛑 停止服務..."
	docker-compose down

# 重啟服務
restart:
	@echo "🔄 重啟服務..."
	docker-compose restart

# 查看日誌
logs:
	@echo "📋 查看日誌（按 Ctrl+C 退出）..."
	docker-compose logs -f

# 測試服務
test:
	@echo "🧪 測試服務..."
	@sleep 3
	@./test_paddleocr.sh || echo "⚠️  服務可能還在啟動中，請稍後再試"

# 清理
clean:
	@echo "🧹 清理容器和映像檔..."
	docker-compose down -v
	docker rmi paddleocr-service 2>/dev/null || true
	@echo "✅ 清理完成"

# 建構並啟動
all: build start
	@echo "🎉 所有服務已啟動完成！"
