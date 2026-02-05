# Docker 使用指南

## 快速啟動

### 方法 1: 使用 Makefile（最簡單）

```bash
make all
```

### 方法 2: 使用 docker-compose

```bash
docker-compose up -d
```

### 方法 3: 手動 Docker 指令

```bash
# 建構映像檔
docker build -f Dockerfile.paddleocr -t paddleocr-service .

# 啟動容器
docker run -d --name paddleocr-container -p 8866:8866 paddleocr-service

# 查看日誌
docker logs -f paddleocr-container

# 停止服務
docker stop paddleocr-container
```

## 檔案說明

- `Dockerfile.paddleocr` - PaddleOCR 服務的 Docker 映像檔定義
- `start_paddleocr_service.py` - PaddleOCR Flask 服務程式（會被複製到容器內）
- `docker-compose.yml` - Docker Compose 配置檔
- `Makefile` - 簡化的指令集

## 注意事項

1. **第一次建構會較慢**：需要下載約 88MB 的 PaddlePaddle 套件
2. **確保檔案位置正確**：`start_paddleocr_service.py` 必須在專案根目錄
3. **端口 8866**：確保此端口未被占用

## 常見問題

### 建構失敗？

```bash
# 清理後重新建構
docker system prune -f
docker build -f Dockerfile.paddleocr -t paddleocr-service .
```

### 容器無法啟動？

```bash
# 查看詳細日誌
docker logs paddleocr-container

# 檢查端口
lsof -i :8866
```

### 想進入容器調試？

```bash
docker exec -it paddleocr-container /bin/bash
```
