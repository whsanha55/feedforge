import { useQuery } from '@tanstack/react-query';
import { ApiError } from '@/lib/api-client';
import { fetchHealth } from '@/lib/health';

const REFETCH_INTERVAL_MS = 30_000;

export function ServerStatus() {
  const healthQuery = useQuery({
    queryKey: ['health'],
    queryFn: ({ signal }) => fetchHealth(signal),
    retry: 1,
    refetchInterval: REFETCH_INTERVAL_MS,
  });

  if (healthQuery.isPending) {
    return (
      <p role="status" className="text-sm text-muted-foreground">
        서버 상태를 확인하는 중…
      </p>
    );
  }

  if (healthQuery.isError) {
    const detail = describeError(healthQuery.error);
    return (
      <p role="status" className="text-sm text-muted-foreground">
        서버 연결 안 됨 — {detail}
      </p>
    );
  }

  return (
    <p role="status" className="text-sm text-muted-foreground">
      서버 연결됨
    </p>
  );
}

function describeError(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) {
    return '액세스 토큰이 거부되었습니다. 설정에서 토큰을 확인해 주세요.';
  }
  if (error instanceof ApiError && error.requestId !== null) {
    return `서버 오류 (requestId: ${error.requestId})`;
  }
  return '백엔드 서버에 도달할 수 없습니다.';
}
