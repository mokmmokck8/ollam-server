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
        
        # 讀取圖片
        image_bytes = file.read()
        image = Image.open(io.BytesIO(image_bytes))
        
        # 轉換為 numpy array
        img_array = np.array(image)
        
        # 執行 OCR (不傳遞 cls 參數，使用初始化時的 use_angle_cls 設定)
        result = ocr.ocr(img_array)
        
        print(f"OCR result type: {type(result)}")
        print(f"OCR result: {result}")
        
        # 格式化結果
        formatted_result = {
            'results': [{
                'data': []
            }]
        }
        
        # 處理 OCR 結果
        if result and len(result) > 0 and result[0]:
            for line in result[0]:
                if line and len(line) >= 2:
                    # line 的格式: [box, (text, confidence)]
                    box = line[0]
                    text_info = line[1]
                    
                    # 檢查 text_info 格式
                    if isinstance(text_info, (list, tuple)) and len(text_info) >= 2:
                        text = text_info[0]
                        confidence = text_info[1]
                    else:
                        # 如果格式不符，跳過這一行
                        print(f"Unexpected text_info format: {text_info}")
                        continue
                    
                    formatted_result['results'][0]['data'].append({
                        'text': text,
                        'confidence': confidence,
                        'box': box.tolist() if hasattr(box, 'tolist') else box
                    })
        
        return jsonify(formatted_result)
    
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'healthy'})

if __name__ == '__main__':
    print("Starting PaddleOCR service on http://localhost:8866")
    print("Endpoint: POST http://localhost:8866/predict/ocr_system")
    app.run(host='0.0.0.0', port=8866, debug=False)
