#!/usr/bin/env python3
"""
PaddleOCR 服務啟動腳本
使用 PaddleOCR 提供 HTTP API 服務

安裝依賴:
pip install paddlepaddle paddleocr flask pillow numpy

運行:
python start_paddleocr_service.py
"""

from paddleocr import PaddleOCR
from flask import Flask, request, jsonify
import numpy as np
from PIL import Image
import io

app = Flask(__name__)

# 初始化 PaddleOCR
# use_angle_cls=True 表示使用方向分類器
# lang='ch' 表示中文，也支援英文（'en'）和其他語言
ocr = PaddleOCR(use_angle_cls=True, lang='ch')

@app.route('/predict/ocr_system', methods=['POST'])
def ocr_predict():
    try:
        # 檢查是否有上傳的檔案
        if 'images' not in request.files:
            return jsonify({'error': 'No image file provided'}), 400
        
        file = request.files['images']
        
        if not file or file.filename == '':
            return jsonify({'error': 'No file selected'}), 400
        
        # 讀取圖片
        image_bytes = file.read()
        
        if not image_bytes or len(image_bytes) == 0:
            return jsonify({'error': 'Empty file uploaded'}), 400
        
        # 打開並驗證圖片
        try:
            image = Image.open(io.BytesIO(image_bytes))
            # 確保圖片是 RGB 模式
            if image.mode != 'RGB':
                image = image.convert('RGB')
        except Exception as e:
            return jsonify({'error': f'Invalid image file: {str(e)}'}), 400
        
        # 轉換為 numpy array
        img_array = np.array(image)
        
        print(f"Processing image with shape: {img_array.shape}")
        
        # 執行 OCR (不傳遞 cls 參數，使用初始化時的 use_angle_cls 設定)
        result = ocr.ocr(img_array)
        
        print(f"OCR result type: {type(result)}")
        print(f"OCR result length: {len(result) if result else 0}")
        if result:
            print(f"OCR result content: {result}")
        
        # 格式化結果
        formatted_result = {
            'results': [{
                'data': []
            }]
        }
        
        # 處理 OCR 結果 - 更安全的方式
        if result is None:
            print("OCR returned None")
            return jsonify(formatted_result)
        
        if not isinstance(result, list):
            print(f"Unexpected result type: {type(result)}")
            return jsonify(formatted_result)
        
        if len(result) == 0:
            print("OCR returned empty list")
            return jsonify(formatted_result)
        
        # 獲取第一個結果（通常對應一個圖片）
        first_result = result[0]
        
        if first_result is None:
            print("First result is None - no text detected in image")
            return jsonify(formatted_result)
        
        # 檢查是否為新版本的字典格式（包含 rec_texts 和 rec_scores）
        if isinstance(first_result, dict):
            print("Detected new PaddleOCR dictionary format")
            rec_texts = first_result.get('rec_texts', [])
            rec_scores = first_result.get('rec_scores', [])
            rec_boxes = first_result.get('rec_boxes', [])
            
            if not rec_texts:
                print("No text detected in image (rec_texts is empty)")
                return jsonify(formatted_result)
            
            # 處理檢測到的文字
            for idx, text in enumerate(rec_texts):
                try:
                    confidence = rec_scores[idx] if idx < len(rec_scores) else 0.0
                    box = rec_boxes[idx].tolist() if idx < len(rec_boxes) and hasattr(rec_boxes[idx], 'tolist') else []
                    
                    formatted_result['results'][0]['data'].append({
                        'text': str(text),
                        'confidence': float(confidence),
                        'box': box
                    })
                    print(f"Line {idx}: {text} (confidence: {confidence:.2f})")
                except Exception as line_error:
                    print(f"Error processing line {idx}: {str(line_error)}")
                    continue
        
        # 處理舊版本的列表格式
        elif isinstance(first_result, list):
            print("Detected old PaddleOCR list format")
            # 處理檢測到的文字行
            for idx, line in enumerate(first_result):
                try:
                    if not line or not isinstance(line, (list, tuple)):
                        print(f"Skipping invalid line {idx}: {line}")
                        continue
                    
                    if len(line) < 2:
                        print(f"Skipping line {idx} with insufficient data: {line}")
                        continue
                    
                    # line 的格式: [box, (text, confidence)]
                    box = line[0]
                    text_info = line[1]
                    
                    # 檢查 text_info 格式
                    if isinstance(text_info, (list, tuple)) and len(text_info) >= 2:
                        text = str(text_info[0])
                        confidence = float(text_info[1])
                        
                        formatted_result['results'][0]['data'].append({
                            'text': text,
                            'confidence': confidence,
                            'box': box.tolist() if hasattr(box, 'tolist') else box
                        })
                    else:
                        # 如果格式不符，跳過這一行
                        print(f"Unexpected text_info format at line {idx}: {text_info}")
                        continue
                except Exception as line_error:
                    print(f"Error processing line {idx}: {str(line_error)}")
                    continue
        else:
            print(f"Unexpected first_result type: {type(first_result)}")
            return jsonify(formatted_result)
        
        print(f"Extracted {len(formatted_result['results'][0]['data'])} text lines")
        return jsonify(formatted_result)
    
    except Exception as e:
        import traceback
        error_details = traceback.format_exc()
        print(f"OCR prediction error: {error_details}")
        return jsonify({'error': str(e), 'details': error_details}), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'healthy'})

if __name__ == '__main__':
    print("Starting PaddleOCR service on http://localhost:8866")
    print("Endpoint: POST http://localhost:8866/predict/ocr_system")
    app.run(host='0.0.0.0', port=8866, debug=False)
