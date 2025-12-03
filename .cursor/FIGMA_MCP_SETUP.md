# Figma MCP 설정 가이드

## 현재 상태
- ✅ Bun 설치 완료
- ✅ figma-mcp 프로젝트 클론 완료
- ✅ .cursor/mcp.json 파일 생성 완료
- ✅ WebSocket 서버 실행 중 (port 3055)

## 다음 단계

### 1. MCP 서버 시작 (별도 터미널에서 실행)
```bash
cd /Users/sdu/sdu/CMPMusicPlayer/swpy/figma-mcp
export PATH="$HOME/.bun/bin:$PATH"
bunx cursor-talk-to-figma-mcp@latest
```

### 2. Figma 플러그인 설치
1. Figma 앱을 엽니다
2. 플러그인 메뉴에서 "Cursor Talk To Figma MCP Plugin" 검색 후 설치
   - 또는 로컬 설치: `Plugins > Development > Import plugin from manifest...`
   - 파일 선택: `figma-mcp/src/cursor_mcp_plugin/manifest.json`

### 3. Cursor 재시작
- MCP 설정이 적용되려면 Cursor를 재시작해야 합니다

### 4. Figma에서 채널 연결
1. Figma에서 플러그인을 실행합니다
2. 채널 이름을 입력합니다: `rj3ktg39`
3. 플러그인이 WebSocket 서버에 연결됩니다

### 5. Cursor에서 채널에 Join
Cursor에서 다음 명령어를 사용하여 채널에 join합니다:
```
join_channel 채널이름은 rj3ktg39
```

## 삼각형 생성하기
채널에 연결된 후, 벡터 경로를 사용하여 삼각형을 만들 수 있습니다.

Figma에서 삼각형을 만들기 위해서는:
1. 벡터 노드를 생성하고
2. vectorPaths를 설정하여 삼각형 경로를 그려야 합니다

현재 MCP 도구에는 직접 삼각형을 만드는 도구가 없으므로, 
벡터 경로를 사용하거나 Figma 플러그인 코드를 실행해야 합니다.

