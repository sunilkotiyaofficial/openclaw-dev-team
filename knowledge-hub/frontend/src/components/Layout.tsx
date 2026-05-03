import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import { BookOpen, FileText, Library, LogOut, LayoutDashboard, Settings } from 'lucide-react';
import clsx from 'clsx';

/**
 * Main app layout — sidebar navigation + auth-aware header.
 *
 * Demonstrates:
 *  - Conditional rendering by role (Admin link only for ADMIN role)
 *  - React Router v6 nested routes via <Outlet>
 *  - Active link highlighting via NavLink
 */
export function Layout() {
  const { user, logout, hasAnyRole } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex bg-[rgb(var(--surface))]">
      {/* ─── Sidebar ──────────────────────────────────────────────── */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col shadow-sm">
        {/* Brand */}
        <div className="p-6 border-b border-gray-100">
          <h1 className="text-lg font-bold flex items-center gap-2 text-gray-900">
            <span className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center">
              <BookOpen className="w-5 h-5 text-white" />
            </span>
            Knowledge Hub
          </h1>
          <p className="text-xs text-gray-500 mt-1 ml-10">Interview Prep Tracker</p>
        </div>

        {/* Nav */}
        <nav className="flex-1 p-3 space-y-1">
          <NavItem to="/" icon={<LayoutDashboard className="w-4 h-4" />}>
            Dashboard
          </NavItem>
          <NavItem to="/topics" icon={<Library className="w-4 h-4" />}>
            Topics
          </NavItem>
          <NavItem to="/notes" icon={<FileText className="w-4 h-4" />}>
            Notes
          </NavItem>

          {hasAnyRole('ADMIN') && (
            <>
              <div className="pt-4 pb-1 px-3 text-[11px] uppercase tracking-wider text-gray-400 font-semibold">
                Admin
              </div>
              <NavItem to="/admin" icon={<Settings className="w-4 h-4" />}>
                User Management
              </NavItem>
            </>
          )}
        </nav>

        {/* User card */}
        {user && (
          <div className="p-4 border-t border-gray-100 bg-gray-50">
            <div className="flex items-center gap-3 mb-3">
              <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-emerald-500 flex items-center justify-center text-white text-sm font-semibold">
                {user.username.slice(0, 2).toUpperCase()}
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-sm font-medium text-gray-900 truncate">
                  {user.username}
                </div>
                <div className="text-xs text-gray-500 truncate">{user.email}</div>
              </div>
            </div>
            <div className="flex flex-wrap gap-1 mb-3">
              {user.roles.map(r => (
                <span
                  key={r}
                  className={clsx(
                    'px-2 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wider',
                    r === 'ADMIN' && 'bg-red-100 text-red-700',
                    r === 'EDITOR' && 'bg-amber-100 text-amber-700',
                    r === 'USER' && 'bg-blue-100 text-blue-700',
                  )}
                >
                  {r}
                </span>
              ))}
            </div>
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-2 text-sm text-gray-700 hover:text-red-600 hover:bg-red-50 px-2.5 py-1.5 rounded-md transition-colors border border-gray-200 bg-white"
            >
              <LogOut className="w-4 h-4" />
              Sign out
            </button>
          </div>
        )}
      </aside>

      {/* ─── Main content ─────────────────────────────────────────── */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
}

function NavItem({
  to,
  icon,
  children,
}: {
  to: string;
  icon: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <NavLink
      to={to}
      end={to === '/'}
      className={({ isActive }) =>
        clsx(
          'flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors',
          isActive
            ? 'bg-blue-50 text-blue-700 font-semibold'
            : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
        )
      }
    >
      {icon}
      {children}
    </NavLink>
  );
}
