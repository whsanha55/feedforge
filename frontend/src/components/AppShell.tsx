import { Link, NavLink, Outlet } from 'react-router';

const NAV_ITEMS = [
  { to: '/', label: '피드', end: true },
  { to: '/likes', label: '좋아요', end: false },
  { to: '/settings', label: '설정', end: false },
] as const;

export function AppShell() {
  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b bg-background">
        <div className="mx-auto flex h-14 w-full max-w-3xl items-center justify-between gap-2 px-4">
          <Link to="/" className="text-base font-semibold tracking-tight">
            FeedForge
          </Link>
          <nav aria-label="주요 메뉴">
            <ul className="flex items-center gap-1 sm:gap-2">
              {NAV_ITEMS.map((item) => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `inline-flex min-h-11 items-center rounded-md px-3 text-sm ${
                        isActive
                          ? 'font-medium text-foreground'
                          : 'text-muted-foreground hover:text-foreground'
                      }`
                    }
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-3xl flex-1 px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
