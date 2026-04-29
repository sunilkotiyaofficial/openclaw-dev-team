# Knowledge Hub Frontend (React 18 + TypeScript + Vite)

**Stub** — minimal frontend setup. Extend with components as needed.

## What's Designed

- Vite + React 18 + TypeScript (strict mode)
- TanStack Query for server state
- Tailwind CSS v4 + CSS variables for theming
- React Hook Form + Zod for forms
- React Router v7 for navigation
- API layer with Axios alternative (native fetch wrapper)

## Pages (To Build)

| Route | Page | Backend API |
|---|---|---|
| `/` | Dashboard (stats from TopicService) | `GET /api/topics`, `GET /api/topics/stats` |
| `/topics` | Topics list (CRUD) | `GET /api/topics`, `POST/PUT/DELETE` |
| `/topics/:id` | Topic detail + resources | `GET /api/topics/{id}`, `GET /api/resources?topicId=` |
| `/notes` | Notes list (reactive — uses `/stream` endpoint) | `GET /api/notes/stream/{topicId}` (SSE) |
| `/notes/:id` | Note editor (Markdown) | `GET/PUT /api/notes/{id}` |

## Setup

```bash
cd frontend
npm create vite@latest . -- --template react-ts
npm install @tanstack/react-query react-router-dom @hookform/resolvers zod react-hook-form
npm install -D tailwindcss @tailwindcss/postcss
```

## API Client Pattern (To Implement)

```typescript
// src/lib/api.ts
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!res.ok) throw new Error(`API ${res.status}: ${path}`);
  return res.json();
}

export const api = {
  topics: {
    list: () => request<Topic[]>('/api/topics'),
    get: (id: number) => request<Topic>(`/api/topics/${id}`),
    create: (topic: TopicInput) => request<Topic>('/api/topics', {
      method: 'POST',
      body: JSON.stringify(topic),
    }),
    update: (id: number, topic: TopicInput) => request<Topic>(`/api/topics/${id}`, {
      method: 'PUT',
      body: JSON.stringify(topic),
    }),
    delete: (id: number) => request<void>(`/api/topics/${id}`, { method: 'DELETE' }),
  },
};
```

## TanStack Query Hook Pattern

```typescript
// src/hooks/useTopics.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';

export function useTopics() {
  return useQuery({
    queryKey: ['topics'],
    queryFn: api.topics.list,
    staleTime: 30_000,  // 30 sec — don't re-fetch on every mount
  });
}

export function useCreateTopic() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.topics.create,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['topics'] }),
  });
}
```

## SSE (Server-Sent Events) Hook for Notes

```typescript
// src/hooks/useNotesStream.ts
import { useEffect, useState } from 'react';

export function useNotesStream(topicId: number) {
  const [notes, setNotes] = useState<Note[]>([]);

  useEffect(() => {
    const eventSource = new EventSource(
      `${BASE_URL}/api/notes/stream/${topicId}`
    );
    eventSource.onmessage = (event) => {
      const note: Note = JSON.parse(event.data);
      setNotes(prev => [note, ...prev]);
    };
    return () => eventSource.close();
  }, [topicId]);

  return notes;
}
```

## Why This Frontend Approach

For an interview, you'd say:

> "TanStack Query handles server state — caching, deduplication, refetching. React state hooks for UI state only. Forms via React Hook Form + Zod for type-safe validation. Tailwind for styling. The API layer is a thin fetch wrapper — typed, tested, easy to swap for Axios if needed. SSE hooks consume the reactive backend's streaming endpoints natively."

## Building It Out

When you have time after interviews, build:

1. `Dashboard.tsx` — stats cards using `useTopics`
2. `TopicForm.tsx` — Create/Edit with React Hook Form + Zod
3. `NoteEditor.tsx` — Markdown editor (use `react-markdown`)
4. `useResources.ts` — TanStack Query hook
5. Resilience UI — show "Service degraded — using cached data" when fallback fires
