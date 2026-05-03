import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';
import type { Role } from '@/types';
import type { ReactNode } from 'react';

/**
 * ProtectedRoute — gate UI based on auth + role.
 *
 * Usage:
 *   <ProtectedRoute>             — any authenticated user
 *   <ProtectedRoute roles={['ADMIN']}>  — only ADMIN
 *   <ProtectedRoute roles={['EDITOR', 'ADMIN']}>  — either role
 *
 * If unauthenticated → redirect to /login with `from` location for redirect-back.
 * If authenticated but lacking role → redirect to /forbidden.
 */
interface Props {
  children: ReactNode;
  roles?: Role[];
}

export function ProtectedRoute({ children, roles }: Props) {
  const { isAuthenticated, isLoading, hasAnyRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div className="p-8 text-center">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && !hasAnyRole(...roles)) {
    return <Navigate to="/forbidden" replace />;
  }

  return <>{children}</>;
}
