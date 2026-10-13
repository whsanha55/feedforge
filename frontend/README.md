# FeedForge UI (frontend)

Vite + React 19 + TypeScript 기반 FeedForge 웹 클라이언트.

## 개발

```bash
npm install
npm run dev
```

개발 서버는 `http://localhost:5173`에서 뜨고 `/api/*` 요청을
`http://localhost:8080`으로 프록시한다(접두사 `/api` 제거).
백엔드 앱을 함께 띄워야 헬스체크가 성공한다.

액세스 토큰이 없으면 앱 전체가 토큰 입력 화면으로 잠긴다.
토큰은 localStorage(`feedforge-token`)에 저장되며 설정 화면에서 변경할 수 있다.

## 검증

```bash
npm run typecheck
npm run lint
npm run format:check
npm run test
npm run build
```

## Docker 빌드

```bash
docker build -t feedforge-ui:dev .
docker run --rm -p 8081:80 feedforge-ui:dev
```

컨테이너는 nginx로 빌드 산출물을 제공하고 `/api/*`를
`http://app:8080/`으로 프록시한다. 운영 compose에서는 앱 컨테이너를
`app` 서비스명으로 연결한다.
