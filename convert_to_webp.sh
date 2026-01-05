#!/bin/bash

# PNG를 WebP로 변환하는 스크립트
# 사용법: ./convert_to_webp.sh [품질] [입력디렉토리]

QUALITY=${1:-80}
INPUT_DIR=${2:-"app/src/main/res"}

echo "🔄 PNG → WebP 변환 시작"
echo "📊 품질: $QUALITY"
echo "📁 대상: $INPUT_DIR"

# drawable 폴더의 PNG 파일들 찾기 (mipmap 제외)
find "$INPUT_DIR" -name "*.png" -not -path "*/mipmap*" | while read png_file; do
    # WebP 파일 경로 생성
    webp_file="${png_file%.png}.webp"
    
    # 변환 실행
    if cwebp -q "$QUALITY" "$png_file" -o "$webp_file" 2>/dev/null; then
        # 용량 비교
        png_size=$(stat -f%z "$png_file" 2>/dev/null || stat -c%s "$png_file" 2>/dev/null)
        webp_size=$(stat -f%z "$webp_file" 2>/dev/null || stat -c%s "$webp_file" 2>/dev/null)
        
        if [ "$png_size" -gt 0 ] && [ "$webp_size" -gt 0 ]; then
            ratio=$(( (png_size - webp_size) * 100 / png_size ))
            echo "✅ ${png_file#$INPUT_DIR/} ($png_size → $webp_size bytes, $ratio% 절약)"
        else
            echo "✅ ${png_file#$INPUT_DIR/} 변환 완료"
        fi
    else
        echo "❌ ${png_file#$INPUT_DIR/} 변환 실패"
    fi
done

echo "🎉 변환 완료!"
