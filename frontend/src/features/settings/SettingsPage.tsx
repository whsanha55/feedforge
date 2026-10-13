import { useState } from 'react';
import { ServerStatus } from '@/components/ServerStatus';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { saveToken } from '@/lib/token';

interface SettingsPageProps {
  onTokenSaved: () => void;
}

export function SettingsPage({ onTokenSaved }: SettingsPageProps) {
  const [token, setToken] = useState('');
  const [saved, setSaved] = useState(false);

  const trimmed = token.trim();
  const submittable = trimmed !== '';

  function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!submittable) {
      return;
    }
    saveToken(trimmed);
    setToken('');
    setSaved(true);
    onTokenSaved();
  }

  return (
    <div className="space-y-6">
      <section aria-labelledby="settings-heading">
        <h1
          id="settings-heading"
          className="text-xl font-semibold tracking-tight"
        >
          설정
        </h1>
      </section>

      <Card>
        <CardHeader>
          <CardTitle>액세스 토큰</CardTitle>
          <CardDescription>
            새 액세스 토큰을 입력하면 저장 즉시 모든 요청에 적용됩니다.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-3">
            <div className="space-y-2">
              <Label htmlFor="new-access-token">새 액세스 토큰</Label>
              <Input
                id="new-access-token"
                name="new-access-token"
                type="password"
                autoComplete="off"
                value={token}
                onChange={(event) => {
                  setToken(event.target.value);
                  setSaved(false);
                }}
              />
            </div>
            <div className="flex items-center gap-3">
              <Button type="submit" disabled={!submittable}>
                토큰 변경
              </Button>
              {saved ? (
                <p role="status" className="text-sm text-muted-foreground">
                  저장했습니다.
                </p>
              ) : null}
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>서버 상태</CardTitle>
          <CardDescription>백엔드 서버 헬스체크 결과입니다.</CardDescription>
        </CardHeader>
        <CardContent>
          <ServerStatus />
        </CardContent>
      </Card>
    </div>
  );
}
