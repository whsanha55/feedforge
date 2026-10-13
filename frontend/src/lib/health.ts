import { apiRequest } from './api-client';

export interface Health {
  status: string;
}

export function fetchHealth(signal?: AbortSignal): Promise<Health> {
  return apiRequest('/actuator/health', {
    signal,
    parseResponse: parseHealth,
  });
}

function parseHealth(data: unknown): Health {
  if (isRecord(data) && typeof data.status === 'string' && data.status !== '') {
    return { status: data.status };
  }
  throw new Error('서버 상태 응답 형식이 올바르지 않습니다.');
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
