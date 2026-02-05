# PaddleOCR + Llama 兩段式文字識別系統

## 架構說明

本系統採用兩段式處理流程來識別圖片中的文字資訊：

### 流程圖

```
圖片檔案
  ↓
[第一步] PaddleOCR 提取文字
  ↓
提取的文字內容
  ↓
[第二步] Llama 分析並結構化資料
  ↓
結構化的公司資訊 JSON
```

### 與之前的區別

**之前的方案（單段式）：**

- 使用 LLaVA 視覺模型直接讀取圖片並提取資訊
- 優點：一步到位
- 缺點：LLaVA 對中文圖片識別不夠準確

**現在的方案（兩段式）：**

1. **第一步**：使用 PaddleOCR 提取圖片中的文字
   - PaddleOCR 對中文 OCR 識別準確度高
   - 支援各種角度和排版的文字
2. **第二步**：使用 Llama 分析提取的文字
   - Llama 擅長理解和結構化文字資料
   - 將文字轉換為結構化的 JSON 格式

## 安裝和啟動

### 方式一：使用 Docker（推薦）⭐

這是最簡單的方式，不需要手動安裝相依套件。

#### 選項 A：使用 Makefile（最方便）🎯

```bash
# 查看所有可用指令
make help

# 建構並啟動服務（一鍵完成）
make all

# 其他常用指令
make start    # 啟動服務
make stop     # 停止服務
make restart  # 重啟服務
make logs     # 查看日誌
make test     # 測試服務
make clean    # 清理所有容器和映像檔
```

#### 選項 B：使用 docker-compose

```bash
# 啟動所有服務
docker-compose up -d

# 查看日誌
docker-compose logs -f

# 停止所有服務
docker-compose down
```

#### 選項 C：使用啟動腳本

```bash
# 給予執行權限
chmod +x start_paddleocr_docker.sh

# 啟動服務
./start_paddleocr_docker.sh
```

#### 選項 C：手動 Docker 指令

```bash
# 1. 建構映像檔
docker build -f Dockerfile.paddleocr -t paddleocr-service .

# 2. 啟動容器
docker run -d --name paddleocr-container -p 8866:8866 paddleocr-service

# 3. 查看日誌
docker logs -f paddleocr-container

# 4. 停止服務
docker stop paddleocr-container

# 5. 移除容器
docker rm paddleocr-container
```

### 方式二：本地安裝

如果你不想使用 Docker，可以直接在本機安裝。

### 1. 安裝 PaddleOCR 相依套件

```bash
pip install paddlepaddle paddleocr flask pillow numpy
```

### 2. 啟動 PaddleOCR 服務（本地方式）

```bash
python start_paddleocr_service.py
```

服務將在 `http://localhost:8866` 啟動

### 3. 啟動 Ollama Llama 模型

```bash
# 確保 Llama 模型已下載
ollama pull llama3

# Ollama 預設在 http://localhost:11434 運行
```

### 4. 啟動 Spring Boot 應用程式

```bash
mvn spring-boot:run
```

## 測試 API

### 快速測試

```bash
# 測試 PaddleOCR 服務是否運行
./test_paddleocr.sh

# 或手動測試
curl http://localhost:8866/health
```

### 上傳圖片進行識別

```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@/path/to/your/image.jpg"
```

### 完整測試流程

```bash
# 1. 啟動 PaddleOCR 服務
docker-compose up -d
# 或使用腳本：./start_paddleocr_docker.sh

# 2. 確認 Ollama 服務運行中
ollama list

# 3. 啟動 Spring Boot（如果還沒啟動）
mvn spring-boot:run

# 4. 測試服務
./test_paddleocr.sh

# 5. 上傳測試圖片
curl -X POST http://localhost:8080/api/upload \
  -F "file=@test-image.jpg" \
  | python3 -m json.tool
```

### 處理流程說明

1. 圖片上傳到 `/api/upload` endpoint
2. `DocumentUploadController` 檢測到是圖片檔案
3. 呼叫 `OcrService.extractTextFromImage()` 使用 PaddleOCR 提取文字
4. 將提取的文字傳給 `PromptBuilder` 建構提示詞
5. `AiExtractService.extract()` 使用 Llama 分析文字並返回結構化資料

## 程式碼結構

```
src/main/java/com/example/demo/
├── controller/
│   └── DocumentUploadController.java  # 處理檔案上傳，協調兩段式流程
├── service/
│   ├── OcrService.java                # 新增：呼叫 PaddleOCR 提取文字
│   ├── AiExtractService.java          # 使用 Llama 分析文字（不再使用 LLaVA）
│   ├── PdfParseService.java           # PDF 文字提取
│   └── PromptBuilder.java             # 建構 AI 提示詞
└── model/
    └── CompanyInfo.java               # 公司資訊資料模型
```

## 優勢

1. **更高的中文識別準確度**：PaddleOCR 專門優化了中文 OCR
2. **模組化設計**：OCR 和語義分析分離，易於維護和升級
3. **靈活性**：可以單獨替換 OCR 引擎或 LLM 模型
4. **成本效益**：Llama 文字模型比 LLaVA 視覺模型運行更快，資源消耗更低

## 注意事項

- 確保 PaddleOCR 服務在 `http://localhost:8866` 運行
- 確保 Ollama 服務在 `http://localhost:11434` 運行
- 第一次使用 PaddleOCR 時會自動下載模型檔案（約 8-12 MB）
- 建議使用清晰的圖片以獲得最佳 OCR 效果

## 常見問題與故障排除

### Docker 相關

**Q: Docker 建構映像檔很慢？**

```bash
# 使用國內映像源加速（如果在中國）
docker build --build-arg PIP_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple \
  -f Dockerfile.paddleocr -t paddleocr-service .
```

**Q: 查看 PaddleOCR 容器日誌**

```bash
docker logs -f paddleocr-container
```

**Q: 重新啟動服務**

```bash
# 使用 docker-compose
docker-compose restart paddleocr

# 或手動重啟
docker restart paddleocr-container
```

### 服務連線問題

**Q: 無法連接到 PaddleOCR 服務**

```bash
# 檢查容器是否運行
docker ps | grep paddleocr

# 檢查端口是否被占用
lsof -i :8866

# 測試服務健康狀態
curl http://localhost:8866/health
```

**Q: Spring Boot 無法連接到 PaddleOCR**

- 確認 PaddleOCR 容器正在運行：`docker ps`
- 確認網路連接：`curl http://localhost:8866/health`
- 檢查防火牆設定

### 性能優化

**Q: OCR 處理速度慢？**

- 考慮調整圖片大小（太大的圖片會影響速度）
- 使用 GPU 版本的 PaddleOCR（需修改 Dockerfile）
- 增加 Docker 容器的資源限制

```bash
# 限制 CPU 和記憶體
docker run -d --name paddleocr-container \
  --cpus="2" --memory="2g" \
  -p 8866:8866 paddleocr-service
```

## 進階配置

### 使用 GPU 加速（選用）

修改 `Dockerfile.paddleocr`：

```dockerfile
FROM paddlepaddle/paddle:2.5.1-gpu-cuda11.7-cudnn8.4-trt8.4

# ... 其他配置保持不變
```

### 自訂 PaddleOCR 參數

修改 `start_paddleocr_service.py`：

```python
# 調整語言模型
ocr = PaddleOCR(use_angle_cls=True, lang='ch')  # 'ch', 'en', 'korean', 'japan'...

# 調整識別精度
ocr = PaddleOCR(use_angle_cls=True, lang='ch', det_db_thresh=0.3, det_db_box_thresh=0.5)
```
