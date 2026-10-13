import { beforeEach, describe, expect, it } from 'vitest';
import { getStoredToken, saveToken, TOKEN_STORAGE_KEY } from './token';

beforeEach(() => {
  window.localStorage.clear();
});

describe('토큰 저장소', () => {
  it('저장한 토큰을 localStorage에서 조회한다', () => {
    saveToken('token-1234');

    expect(getStoredToken()).toBe('token-1234');
    expect(window.localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('token-1234');
  });

  it('토큰이 없으면 null을 반환한다', () => {
    expect(getStoredToken()).toBeNull();
  });

  it('다시 저장하면 값을 덮어쓴다', () => {
    saveToken('old-token');
    saveToken('new-token');

    expect(getStoredToken()).toBe('new-token');
  });
});
