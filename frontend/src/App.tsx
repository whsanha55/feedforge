import { useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { AppShell } from '@/components/AppShell';
import { TokenGate } from '@/components/TokenGate';
import { FeedPage } from '@/features/feed/FeedPage';
import { LikesPage } from '@/features/likes/LikesPage';
import { SettingsPage } from '@/features/settings/SettingsPage';
import { getStoredToken } from '@/lib/token';

export default function App() {
  const [unlocked, setUnlocked] = useState(() => getStoredToken() !== null);

  return (
    <BrowserRouter>
      {unlocked ? (
        <Routes>
          <Route element={<AppShell />}>
            <Route index element={<FeedPage />} />
            <Route path="likes" element={<LikesPage />} />
            <Route
              path="settings"
              element={<SettingsPage onTokenSaved={() => setUnlocked(true)} />}
            />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      ) : (
        <TokenGate onTokenSaved={() => setUnlocked(true)} />
      )}
    </BrowserRouter>
  );
}
