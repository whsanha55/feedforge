export const TOKEN_STORAGE_KEY = 'feedforge-token';

export function getStoredToken(): string | null {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function saveToken(token: string): void {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
}
