import type {
  AuthResponse, LoginRequest, RegisterRequest,
  Topic, TopicInput, Note, Resource, ApiError,
} from '@/types';

/**
 * JWT-aware fetch wrapper.
 *
 * Key responsibilities:
 *  - Attach Authorization: Bearer <token> header on every request
 *  - Handle 401 by clearing auth + redirecting to /login
 *  - Parse error responses into a typed ApiError
 *  - JSON serialize/deserialize automatically
 *
 * Token storage: in-memory + httpOnly cookie for refresh token
 * (in this demo, we use localStorage for simplicity — comment in code below
 *  explains the production trade-off).
 */

const BASE = '';  // proxy through Vite dev server (vite.config.ts)

class ApiClient {
  // In-memory access token (lost on page refresh — refreshed via /api/auth/refresh)
  // Production: store refresh token in httpOnly cookie via Set-Cookie from /login response
  private accessToken: string | null = null;

  setAccessToken(token: string | null) {
    this.accessToken = token;
    // Also persist to sessionStorage so refresh survives reload
    // (sessionStorage is per-tab, less risky than localStorage for tokens)
    if (token) {
      sessionStorage.setItem('access_token', token);
    } else {
      sessionStorage.removeItem('access_token');
    }
  }

  loadFromStorage() {
    this.accessToken = sessionStorage.getItem('access_token');
  }

  getAccessToken(): string | null {
    return this.accessToken ?? sessionStorage.getItem('access_token');
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const token = this.getAccessToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(init.headers as Record<string, string>),
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(`${BASE}${path}`, { ...init, headers });

    if (response.status === 204) {
      return undefined as T;
    }

    if (!response.ok) {
      // Auto-logout on 401 (token expired / invalid)
      if (response.status === 401) {
        this.setAccessToken(null);
        if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
      // Try to parse structured error body
      let error: ApiError;
      try {
        error = await response.json();
      } catch {
        error = {
          timestamp: new Date().toISOString(),
          status: response.status,
          message: response.statusText,
        };
      }
      throw error;
    }

    return response.json() as Promise<T>;
  }

  // ─── Auth API ─────────────────────────────────────────────────────
  auth = {
    login: (req: LoginRequest) =>
      this.request<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(req),
      }),

    register: (req: RegisterRequest) =>
      this.request<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(req),
      }),

    refresh: (refreshToken: string) =>
      this.request<AuthResponse>('/api/auth/refresh', {
        method: 'POST',
        body: JSON.stringify({ refreshToken }),
      }),
  };

  // ─── Topics API ───────────────────────────────────────────────────
  topics = {
    list: () => this.request<Topic[]>('/api/topics'),
    get: (id: number) => this.request<Topic>(`/api/topics/${id}`),
    create: (input: TopicInput) =>
      this.request<Topic>('/api/topics', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    update: (id: number, input: TopicInput) =>
      this.request<Topic>(`/api/topics/${id}`, {
        method: 'PUT',
        body: JSON.stringify(input),
      }),
    delete: (id: number) =>
      this.request<void>(`/api/topics/${id}`, { method: 'DELETE' }),
  };

  // ─── Notes API (reactive backend) ─────────────────────────────────
  notes = {
    byTopic: (topicId: number) =>
      this.request<Note[]>(`/api/notes/by-topic/${topicId}`),
    get: (id: string) => this.request<Note>(`/api/notes/${id}`),
    create: (note: Partial<Note>) =>
      this.request<Note>('/api/notes', {
        method: 'POST',
        body: JSON.stringify(note),
      }),
    update: (id: string, note: Partial<Note>) =>
      this.request<Note>(`/api/notes/${id}`, {
        method: 'PUT',
        body: JSON.stringify(note),
      }),
    delete: (id: string) =>
      this.request<void>(`/api/notes/${id}`, { method: 'DELETE' }),
  };

  // ─── Resources API ────────────────────────────────────────────────
  resources = {
    list: () => this.request<Resource[]>('/api/resources'),
  };
}

export const api = new ApiClient();
api.loadFromStorage();
