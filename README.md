# ollam-server (Spring Boot + PaddleOCR + Ollama)

這個專案是一個 Spring Boot API：

- 上傳 **PDF**：用 `PdfParseService` (PDFBox) 抽文字 -> 丟給 **Ollama** 做欄位抽取
- 上傳 **圖片**：先呼叫 **PaddleOCR** 抽文字 -> 再丟給 **Ollama** 做欄位抽取

> 目前程式碼是「OCR 與 LLM 分兩段」：
>
> - OCR：`OcrService` 呼叫 `http://localhost:8866/predict/ocr_system`
> - LLM：`AiExtractService` 呼叫 `${ollama.base-url}`，預設 `http://localhost:11434`

---

## 必要檔案 / 可選檔案（以目前 repo 來看）

### Spring Boot 端（必要）

- `pom.xml`：必要（Maven 依賴、Spring Boot plugin）
- `src/main/java/**`：必要（API、Service、Model）
- `src/main/resources/application.properties`：建議保留（有 `ollama.base-url`、上傳大小限制）
- `mvnw` / `mvnw.cmd`：建議保留（讓沒裝 Maven 的機器也可以用 wrapper build/run）

### PaddleOCR 端（需不需要看你怎麼啟動）

以下是一組「用 Docker 跑 PaddleOCR HTTP service」的檔案：

- `Dockerfile.paddleocr`：**需要**（如果你要自己 build PaddleOCR image）
- `start_paddleocr_service.py`：**需要**（PaddleOCR 的 Flask 服務本體）
- `docker-compose.yml`：**建議**（如果你希望用 compose 一鍵起 PaddleOCR，未來也可以加 Ollama）

以下偏工具脚本（可選）：

- `start_paddleocr_docker.sh`：可選（等同於手動 build+run）
- `Makefile`：可選（只是把 docker-compose / build 指令包成 `make start` 等）

> `Makefile` 目前的 `make test` 會呼叫 `./test_paddleocr.sh`，但 repo 內沒有這個檔案；
> 所以 **`make test` 會失敗或一直 warning**。你可以：
>
> 1. 補上 `test_paddleocr.sh`；或 2) 修改/移除 Makefile 的 test target。

---

## 怎麼把 PaddleOCR 開起來

### 方式 A：用 Docker Compose（最推薦）

此 repo 已有 `docker-compose.yml`，目前只包含 PaddleOCR：

```bash
docker-compose up -d --build
```

起來後：

- PaddleOCR health check：`http://localhost:8866/health`
- OCR endpoint：`POST http://localhost:8866/predict/ocr_system`（multipart form field 名稱是 `images`）

### 方式 B：只用 Docker（不用 compose）

```bash
docker build -f Dockerfile.paddleocr -t paddleocr-service .
docker run -d --name paddleocr-container -p 8866:8866 paddleocr-service
```

### 方式 C：直接用 Python 在本機跑（不推薦，依賴重）

`start_paddleocr_service.py` 的註解有寫需要裝：`paddlepaddle paddleocr flask pillow numpy`。

---

## 怎麼把 Ollama 開起來

Ollama 有兩種常見跑法：

### 方式 A：Ollama 安裝在本機（最簡單）

1. 安裝並啟動 Ollama（macOS）：Ollama app / service 跑起來之後，預設會聽在 `http://localhost:11434`
2. 拉模型：

```bash
ollama pull llama3
```

Spring Boot 預設會用：

- `ollama.base-url=http://localhost:11434`

### 方式 B：用 Docker 跑 Ollama（可行，但要處理模型 volume + GPU）

你可以把 Ollama 加進 `docker-compose.yml`，但 **macOS Docker Desktop 通常沒辦法用 NVIDIA GPU**，效能會比較差。

（這部分如果你要，我可以直接幫你把 compose 補完整，含 volume 與 network 設定。）

---

## Spring Boot 要怎麼跑

### 方式 A：用 VS Code task

工作區已經有 task：`Run Spring Boot Application`（跑 `mvn spring-boot:run`）。

### 方式 B：用 Maven Wrapper

```bash
./mvnw spring-boot:run
```

啟動後，API 在：

- `POST http://localhost:8080/api/upload`

---

## Ollama 跟 PaddleOCR 可以包在同一個 Docker 嗎？

技術上「可以」，但**不建議**。

原因：

1. **一個 container 跑多個 service** 需要 supervisor（例如 `tini`/`supervisord`）去管理多個 process，
   否則其中一個掛了不容易自動復原、log 也會混在一起。
2. **Ollama** 與 **PaddleOCR** 的 runtime/依賴差很多：
   - Ollama：偏向「模型推論 daemon」，需要存模型（volume），還可能需要 GPU / 特定 runtime。
   - PaddleOCR：Python + Paddle 依賴，image size 也會很大。
     合在一起會讓 image 超大、build 慢、升級也麻煩。
3. 你現在的 Spring Boot 其實只需要「能打到兩個 HTTP endpoint」，用 compose 分開更乾淨。

### 建議做法（最佳實務）

- 一個 container 跑一個 service：
  - `paddleocr`：`8866`
  - `ollama`：`11434`
  - `springboot`：`8080`
- 全部用 `docker-compose.yml` 串起來（同一個 network，用 service name 互相呼叫）

> 注意：如果你把 Spring Boot 也放進 docker-compose 裡，
> 你需要把 Java code 裡的 `localhost` 改成環境變數 + service name（例如 `http://paddleocr:8866`、`http://ollama:11434`），
> 否則 container 內的 `localhost` 會指向自己，不是指向別的 container。

---

## 下一步（我可以直接幫你做）

- [ ] 把 `OcrService` 的 `PADDLEOCR_URL` 改成從 `application.properties` 讀（例如 `paddleocr.base-url`），讓它可在 compose 內使用 `http://paddleocr:8866`
- [ ] 把 `docker-compose.yml` 擴充：加入 `ollama` service +（可選）`springboot` service
- [ ] 補上缺的 `test_paddleocr.sh` 或調整 `Makefile` 避免誤導
