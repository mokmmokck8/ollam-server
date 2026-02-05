#!/bin/bash
# 測試 PaddleOCR 服務是否正常運行

echo "🧪 測試 PaddleOCR 服務..."

# 檢查服務是否運行
echo "📡 檢查服務連線..."
if curl -s http://localhost:8866/health > /dev/null 2>&1; then
    echo "✅ PaddleOCR 服務運行正常"
    curl -s http://localhost:8866/health | python3 -m json.tool
else
    echo "❌ 無法連接到 PaddleOCR 服務"
    echo "請確認服務已啟動：docker ps | grep paddleocr"
    exit 1
fi

echo ""
echo "💡 提示：上傳圖片測試完整流程"
echo "curl -X POST http://localhost:8080/api/upload -F 'file=@your-image.jpg'"
