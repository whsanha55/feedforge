import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TokenGate } from './TokenGate';
import { TOKEN_STORAGE_KEY } from '@/lib/token';

beforeEach(() => {
  window.localStorage.clear();
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(JSON.stringify({ status: 'UP' }), {
          headers: { 'content-type': 'application/json' },
        }),
    ),
  );
});

afterEach(() => {
  vi.unstubAllGlobals();
});

function renderTokenGate(onTokenSaved: () => void = vi.fn()) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={queryClient}>
      <TokenGate onTokenSaved={onTokenSaved} />
    </QueryClientProvider>,
  );
  return { onTokenSaved };
}

describe('TokenGate', () => {
  it('안내 문구와 라벨 연결된 입력창을 보여준다', () => {
    renderTokenGate();

    expect(
      screen.getByRole('heading', { name: 'FeedForge' }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('액세스 토큰')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '저장' })).toBeInTheDocument();
  });

  it('입력값이 비어 있으면 저장 버튼을 비활성화한다', () => {
    renderTokenGate();

    expect(screen.getByRole('button', { name: '저장' })).toBeDisabled();
  });

  it('토큰을 입력해 저장하면 localStorage에 저장하고 잠금을 해제한다', async () => {
    const user = userEvent.setup();
    const { onTokenSaved } = renderTokenGate();

    await user.type(screen.getByLabelText('액세스 토큰'), '  token-1234  ');
    await user.click(screen.getByRole('button', { name: '저장' }));

    expect(window.localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('token-1234');
    expect(onTokenSaved).toHaveBeenCalledTimes(1);
  });
});
