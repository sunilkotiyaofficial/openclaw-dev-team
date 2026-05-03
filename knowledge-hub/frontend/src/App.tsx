import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { AuthProvider } from '@/auth/AuthContext';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { Layout } from '@/components/Layout';
import { LoginPage } from '@/auth/LoginPage';
import { RegisterPage } from '@/auth/RegisterPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { TopicsPage } from '@/pages/TopicsPage';
import { NotesPage } from '@/pages/NotesPage';

/**
 * App entry — wires up:
 *  - QueryClientProvider (TanStack Query — server state)
 *  - AuthProvider (auth context)
 *  - BrowserRouter (React Router v6 routing)
 *  - Protected route wrapper for authenticated pages
 */

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,                   // Don't retry too aggressively
      refetchOnWindowFocus: false, // Avoid spurious refetches
      staleTime: 30_000,           // 30s default
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forbidden" element={<ForbiddenPage />} />

            {/* Protected — wrapped in Layout */}
            <Route
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route path="/" element={<DashboardPage />} />
              <Route path="/topics" element={<TopicsPage />} />
              <Route path="/notes" element={<NotesPage />} />

              {/* ADMIN-only sub-route */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute roles={['ADMIN']}>
                    <AdminPage />
                  </ProtectedRoute>
                }
              />
            </Route>

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
      {/* React Query devtools — visible in dev only */}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}

function ForbiddenPage() {
  return (
    <div className="min-h-screen flex items-center justify-center text-center">
      <div>
        <h1 className="text-4xl font-bold text-red-500 mb-2">403</h1>
        <p className="text-gray-500">You don't have access to this page.</p>
      </div>
    </div>
  );
}

function AdminPage() {
  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold">Admin Panel</h1>
      <p className="text-gray-500 mt-2">Only visible to ADMIN role.</p>
      <ul className="mt-6 space-y-2">
        <li>• User management (POST /api/auth/users/&#123;id&#125;/roles/&#123;role&#125;)</li>
        <li>• Bulk operations</li>
        <li>• System metrics view</li>
      </ul>
    </div>
  );
}
