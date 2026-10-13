import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest } from './api-client';
import { saveToken } from './token';

function jsonResponse(
  body: unknown,
  init: { status?: number; contentType?: string } = {},
): Response {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    headers: { 'content-type': init.contentType ?? 'application/json' },
  });
}

function problemResponse(overrides: Record<string, unknown> = {}): Response {
  return jsonResponse(
    {
      type: 'about:blank',
      title: 'Unauthorized',
      status: 401,
      detail: '액세스 토큰이 유효하지 않습니다.',
      instance: '/feedforge/feed',
      code: 'UNAUTHORIZED',
      requestId: '7f3c9a1e-0000-0000-0000-000000000001',
      ...overrides,
    },
    {
      status: typeof overrides.status === 'number' ? overrides.status : 401,
      contentType: 'application/problem+json',
    },
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

beforeEach(() => {
  window.localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('apiRequest', () => {
  it('토큰이 저장되어 있으면 Authorization Bearer 헤더를 붙인다', async () => {
    saveToken('token-1234');
    const fetchMock = vi.fn(async () => jsonResponse({ status: 'UP' }));
    vi.stubGlobal('fetch', fetchMock);

    await apiRequest('/actuator/health', { parseResponse: parseStatus });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/actuator/health',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer token-1234',
        }),
      }),
    );
  });

  it('토큰이 없으면 Authorization 헤더를 붙이지 않는다', async () => {
    const fetchMock = vi.fn<typeof fetch>(async () =>
      jsonResponse({ status: 'UP' }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await apiRequest('/actuator/health', { parseResponse: parseStatus });

    const init = fetchMock.mock.calls[0]?.[1];
    expect(init?.headers).not.toHaveProperty('Authorization');
  });

  it('성공 응답 본문을 parseResponse에 넘겨 변환한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse({ status: 'UP' })),
    );

    const result = await apiRequest('/actuator/health', {
      parseResponse: parseStatus,
    });

    expect(result).toBe('UP');
  });

  it('problem+json 오류를 ApiError 필드로 변환한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        problemResponse({
          status: 400,
          code: 'INVALID_REQUEST',
          detail: '키워드는 비워둘 수 없습니다.',
          errors: [
            { field: 'keyword', message: '한 글자 이상 입력하세요.' },
            { field: 'weight', message: '0 이상이어야 합니다.' },
          ],
        }),
      ),
    );

    const error = await captureApiError();

    expect(error.status).toBe(400);
    expect(error.code).toBe('INVALID_REQUEST');
    expect(error.detail).toBe('키워드는 비워둘 수 없습니다.');
    expect(error.requestId).toBe('7f3c9a1e-0000-0000-0000-000000000001');
    expect(error.errors).toEqual([
      { field: 'keyword', message: '한 글자 이상 입력하세요.' },
      { field: 'weight', message: '0 이상이어야 합니다.' },
    ]);
  });

  it('401 응답을 상태 코드 401의 ApiError로 던진다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => problemResponse()),
    );

    const error = await captureApiError();

    expect(error.status).toBe(401);
    expect(error.code).toBe('UNAUTHORIZED');
  });

  it('problem+json이 아닌 오류는 기본 안내의 ApiError로 던진다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response('<html>Bad Gateway</html>', {
            status: 502,
            headers: { 'content-type': 'text/html' },
          }),
      ),
    );

    const error = await captureApiError();

    expect(error.status).toBe(502);
    expect(error.code).toBe('UNKNOWN_ERROR');
    expect(error.detail).not.toBe('');
  });

  it('네트워크 오류는 연결 실패 ApiError로 던진다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))),
    );

    const error = await captureApiError();

    expect(error.isNetworkError).toBe(true);
    expect(error.status).toBe(0);
    expect(error.detail).not.toBe('');
  });

  it('취소로 인한 AbortError는 그대로 전파한다', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        Promise.reject(new DOMException('aborted', 'AbortError')),
      ),
    );

    const error = await captureRawError();

    expect(error).toBeInstanceOf(DOMException);
    expect(error.name).toBe('AbortError');
  });
});

function parseStatus(data: unknown): string {
  if (isRecord(data) && typeof data.status === 'string') {
    return data.status;
  }
  throw new Error('응답 형식이 올바르지 않습니다.');
}

async function captureApiError(): Promise<ApiError> {
  const error = await captureRawError();
  if (error instanceof ApiError) {
    return error;
  }
  throw new Error(`ApiError가 아닌 오류가 발생했다: ${error.name}`);
}

async function captureRawError(): Promise<Error | DOMException> {
  try {
    await apiRequest('/actuator/health', { parseResponse: parseStatus });
  } catch (error) {
    // jsdom/Node의 DOMException은 Error를 상속하지 않는다.
    if (error instanceof Error || error instanceof DOMException) {
      return error;
    }
    throw new Error(`Error가 아닌 값이 던져졌다: ${String(error)}`, {
      cause: error,
    });
  }
  throw new Error('오류가 발생하지 않았다.');
}
