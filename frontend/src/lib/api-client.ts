import { getStoredToken } from './token';

export const API_BASE_PATH = '/api';

const NETWORK_ERROR_CODE = 'NETWORK_ERROR';
const UNKNOWN_ERROR_CODE = 'UNKNOWN_ERROR';
const NETWORK_ERROR_DETAIL = '서버에 연결할 수 없습니다.';
const UNKNOWN_ERROR_DETAIL =
  '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.';

export interface FieldError {
  field: string;
  message: string;
}

/**
 * 백엔드가 application/problem+json으로 내려주는 오류를 담는 클라이언트 표준 오류.
 * 컴포넌트는 이 타입만 알고 HTTP 응답 구조를 직접 다루지 않는다.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly requestId: string | null;
  readonly errors: readonly FieldError[];

  constructor(
    status: number,
    code: string,
    detail: string,
    requestId: string | null,
    errors: readonly FieldError[],
  ) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.requestId = requestId;
    this.errors = errors;
  }

  get detail(): string {
    return this.message;
  }

  get isNetworkError(): boolean {
    return this.code === NETWORK_ERROR_CODE;
  }
}

export interface ApiRequestOptions<T> {
  method?: string;
  body?: unknown;
  signal?: AbortSignal;
  /** 응답 본문은 외부 입력이므로 호출 쪽에서 검증해 변환한다. */
  parseResponse: (data: unknown) => T;
}

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions<T>,
): Promise<T> {
  const { method = 'GET', body, signal, parseResponse } = options;

  const headers: Record<string, string> = { Accept: 'application/json' };
  const token = getStoredToken();
  if (token !== null) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_PATH}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw new ApiError(0, NETWORK_ERROR_CODE, NETWORK_ERROR_DETAIL, null, []);
  }

  if (!response.ok) {
    throw await toApiError(response);
  }

  const data: unknown =
    response.status === 204 ? undefined : await response.json();
  return parseResponse(data);
}

async function toApiError(response: Response): Promise<ApiError> {
  const fallback = new ApiError(
    response.status,
    UNKNOWN_ERROR_CODE,
    UNKNOWN_ERROR_DETAIL,
    response.headers.get('x-request-id'),
    [],
  );

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/problem+json')) {
    return fallback;
  }

  const problem: unknown = await response.json().catch(() => null);
  if (!isRecord(problem)) {
    return fallback;
  }

  const status =
    typeof problem.status === 'number' ? problem.status : response.status;
  return new ApiError(
    status,
    typeof problem.code === 'string' && problem.code !== ''
      ? problem.code
      : UNKNOWN_ERROR_CODE,
    typeof problem.detail === 'string' && problem.detail !== ''
      ? problem.detail
      : UNKNOWN_ERROR_DETAIL,
    typeof problem.requestId === 'string' && problem.requestId !== ''
      ? problem.requestId
      : fallback.requestId,
    parseFieldErrors(problem.errors),
  );
}

function parseFieldErrors(value: unknown): readonly FieldError[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.flatMap((item): FieldError[] => {
    if (!isRecord(item)) {
      return [];
    }
    if (typeof item.field !== 'string' || typeof item.message !== 'string') {
      return [];
    }
    return [{ field: item.field, message: item.message }];
  });
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
