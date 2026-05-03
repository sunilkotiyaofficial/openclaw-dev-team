import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import type { Topic, TopicInput } from '@/types';

/**
 * TanStack Query hooks for Topics.
 *
 * Patterns demonstrated:
 *  - useQuery for reads (auto cache + dedup + refetch on focus)
 *  - useMutation for writes (with optimistic updates + rollback)
 *  - queryClient.invalidateQueries to trigger refetch after mutation
 *
 * Interview talking point:
 *   "TanStack Query handles ALL server state: caching, deduplication,
 *   stale-while-revalidate, request retry on network failure, optimistic
 *   updates. We never write loading/error/data state in components manually."
 */

const KEY = ['topics'] as const;

export function useTopics() {
  return useQuery({
    queryKey: KEY,
    queryFn: api.topics.list,
    staleTime: 30_000, // consider fresh for 30 sec
  });
}

export function useTopic(id: number | undefined) {
  return useQuery({
    queryKey: ['topic', id],
    queryFn: () => api.topics.get(id!),
    enabled: id !== undefined,  // skip query if no id yet
  });
}

export function useCreateTopic() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.topics.create,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: KEY });
    },
  });
}

export function useUpdateTopic() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: number; input: TopicInput }) =>
      api.topics.update(id, input),
    // Optimistic update — UI updates immediately, rolls back on error
    onMutate: async ({ id, input }) => {
      await qc.cancelQueries({ queryKey: KEY });
      const previous = qc.getQueryData<Topic[]>(KEY);
      qc.setQueryData<Topic[]>(KEY, (old) =>
        old?.map(t => t.id === id ? { ...t, ...input } : t)
      );
      return { previous };
    },
    onError: (_err, _vars, ctx) => {
      // Rollback on failure
      if (ctx?.previous) qc.setQueryData(KEY, ctx.previous);
    },
    onSettled: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}

export function useDeleteTopic() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: api.topics.delete,
    onSuccess: () => qc.invalidateQueries({ queryKey: KEY }),
  });
}
