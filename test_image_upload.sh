#!/bin/bash

echo "======================================"
echo "Testing Image Upload with OCR"
echo "======================================"
echo ""

# 检查是否提供了图片文件
if [ -z "$1" ]; then
    echo "Usage: $0 <image_file_path>"
    echo "Example: $0 /path/to/test_image.jpg"
    exit 1
fi

IMAGE_FILE="$1"

# 检查文件是否存在
if [ ! -f "$IMAGE_FILE" ]; then
    echo "Error: File '$IMAGE_FILE' not found!"
    exit 1
fi

echo "Testing with image: $IMAGE_FILE"
echo ""

# 1. 测试 PaddleOCR 服务健康状态
echo "1. Checking PaddleOCR service health..."
HEALTH_RESPONSE=$(curl -s http://localhost:8866/health)
echo "   Response: $HEALTH_RESPONSE"
echo ""

# 2. 直接测试 PaddleOCR API
echo "2. Testing PaddleOCR API directly..."
OCR_RESPONSE=$(curl -s -X POST -F "images=@$IMAGE_FILE" http://localhost:8866/predict/ocr_system)
echo "   OCR Response:"
echo "$OCR_RESPONSE" | jq '.' 2>/dev/null || echo "$OCR_RESPONSE"
echo ""

# 3. 测试完整的上传流程（通过 Spring Boot）
echo "3. Testing full upload through Spring Boot..."
UPLOAD_RESPONSE=$(curl -s -X POST -F "file=@$IMAGE_FILE" http://localhost:8080/api/upload)
echo "   Upload Response:"
echo "$UPLOAD_RESPONSE" | jq '.' 2>/dev/null || echo "$UPLOAD_RESPONSE"
echo ""

echo "======================================"
echo "Test completed!"
echo "======================================"
