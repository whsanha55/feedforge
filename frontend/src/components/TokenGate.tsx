import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ServerStatus } from '@/components/ServerStatus';
import { saveToken } from '@/lib/token';

interface TokenGateProps {
  onTokenSaved: () => void;
}

export function TokenGate({ onTokenSaved }: TokenGateProps) {
  const [token, setToken] = useState('');

  const trimmed = token.trim();
  const submittable = trimmed !== '';

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!submittable) {
      return;
    }
    saveToken(trimmed);
    onTokenSaved();
  }

  return (
    <main className="mx-auto flex min-h-svh w-full max-w-sm flex-col justify-center px-4">
      <h1 className="text-2xl font-semibold tracking-tight">FeedForge</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        서비스 이용을 위해 액세스 토큰을 입력하세요. 토큰은 이 브라우저에만
        저장됩니다.
      </p>
      <form onSubmit={handleSubmit} className="mt-6 space-y-3">
        <div className="space-y-2">
          <Label htmlFor="access-token">액세스 토큰</Label>
          <Input
            id="access-token"
            name="access-token"
            type="password"
            autoComplete="off"
            value={token}
            onChange={(event) => setToken(event.target.value)}
          />
        </div>
        <Button type="submit" disabled={!submittable} className="w-full">
          저장
        </Button>
      </form>
      <div className="mt-6 border-t pt-4">
        <ServerStatus />
      </div>
    </main>
  );
}
