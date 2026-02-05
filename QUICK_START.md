# 🚀 快速啟動指南

## 問題排查記錄

### 問題：無法連接到 PaddleOCR 服務

**原因：**

1. Docker 映像檔缺少 `libGL.so.1` 函式庫（OpenCV 需要）
2. 初次建構時檔案複製錯誤

**解決方案：**
已在 Dockerfile.paddleocr 中添加 `libgl1` 套件

## 正確的啟動步驟

### 1. 建構 Docker 映像檔（第一次）

```bash
docker build --no-cache -f Dockerfile.paddleocr -t paddleocr-service .
```

⏱️ **注意：** 第一次建構需要 2-3 分鐘，會下載約 150MB 的套件

### 2. 啟動容器

```bash
# 方式 A: 使用 docker run
docker run -d --name paddleocr-container -p 8866:8866 paddleocr-service

# 方式 B: 使用 docker-compose
docker-compose up -d
```

### 3. 檢查服務狀態

```bash
# 查看容器狀態
docker ps | grep paddleocr

# 查看日誌（確認服務啟動成功）
docker logs -f paddleocr-container

# 應該看到類似這樣的輸出：
# Starting PaddleOCR service on http://localhost:8866
# Endpoint: POST http://localhost:8866/predict/ocr_system
```

### 4. 測試服務

```bash
# 健康檢查
curl http://localhost:8866/health

# 應該返回：
# {"status":"healthy"}
```

### 5. 啟動 Spring Boot（如果還沒啟動）

```bash
mvn spring-boot:run
```

### 6. 測試完整流程

```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@your-image.jpg" \
  | python3 -m json.tool
```

## 常見錯誤

### 錯誤 1: Container keeps restarting

```bash
# 查看日誌找出原因
docker logs paddleocr-container

# 如果是 ImportError: libGL.so.1
# 表示 Dockerfile 需要更新（已修復）
```

### 錯誤 2: Port 8866 already in use

```bash
# 查找占用端口的進程
lsof -i :8866

# 停止舊容器
docker stop paddleocr-container
docker rm paddleocr-container
```

### 錯誤 3: Image not found

```bash
# 確認映像檔存在
docker images | grep paddleocr

# 重新建構
docker build -f Dockerfile.paddleocr -t paddleocr-service .
```

## 完整清理並重新開始

```bash
# 停止並移除所有相關容器
docker stop paddleocr-container 2>/dev/null
docker rm paddleocr-container 2>/dev/null

# 移除映像檔
docker rmi paddleocr-service 2>/dev/null

# 重新建構
docker build --no-cache -f Dockerfile.paddleocr -t paddleocr-service .

# 啟動
docker run -d --name paddleocr-container -p 8866:8866 paddleocr-service

# 等待 5 秒後測試
sleep 5 && curl http://localhost:8866/health
```

## 性能提示

- 第一次使用 PaddleOCR 時會下載模型檔案（約 8-12 MB）
- 建議給 Docker 至少分配 2GB RAM
- 如果使用 Apple Silicon (M1/M2/M3)，已經是 ARM64 架構，性能很好

## 下一步

一旦 PaddleOCR 服務啟動成功，你就可以：

1. 使用 Spring Boot API 上傳圖片
2. 圖片會先經過 PaddleOCR 提取文字
3. 再由 Llama 分析並結構化資料
4. 返回 JSON 格式的公司資訊
