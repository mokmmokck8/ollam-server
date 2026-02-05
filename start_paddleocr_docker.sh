#!/bin/bash
# PaddleOCR Docker 啟動腳本

echo "🐳 正在啟動 PaddleOCR Docker 服務..."

# 檢查 Docker 是否運行
if ! docker info > /dev/null 2>&1; then
    echo "❌ 錯誤：Docker 未運行，請先啟動 Docker"
    exit 1
fi

# 建構 Docker 映像檔
echo "📦 建構 PaddleOCR Docker 映像檔..."
docker build -f Dockerfile.paddleocr -t paddleocr-service .

if [ $? -ne 0 ]; then
    echo "❌ Docker 映像檔建構失敗"
    exit 1
fi

# 停止並移除舊容器（如果存在）
echo "🧹 清理舊容器..."
docker stop paddleocr-container 2>/dev/null
docker rm paddleocr-container 2>/dev/null

# 啟動容器
echo "🚀 啟動 PaddleOCR 容器..."
docker run -d \
    --name paddleocr-container \
    -p 8866:8866 \
    paddleocr-service

if [ $? -eq 0 ]; then
    echo "✅ PaddleOCR 服務已成功啟動！"
    echo "📍 服務位址：http://localhost:8866"
    echo "🔍 查看日誌：docker logs -f paddleocr-container"
    echo "🛑 停止服務：docker stop paddleocr-container"
else
    echo "❌ 容器啟動失敗"
    exit 1
fi
