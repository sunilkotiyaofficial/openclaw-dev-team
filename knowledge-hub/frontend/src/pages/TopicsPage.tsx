import { useState } from 'react';
import { useTopics, useCreateTopic, useDeleteTopic } from '@/hooks/useTopics';
import { useAuth } from '@/auth/AuthContext';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { TopicInput, Topic } from '@/types';
import { Plus, Trash2 } from 'lucide-react';

/**
 * Topics CRUD page.
 *
 * Demonstrates:
 *  - List + create + delete in one screen
 *  - Role-based UI (only EDITOR/ADMIN see create button; only ADMIN sees delete)
 *  - Form validation with React Hook Form + Zod
 *  - Mutations with TanStack Query
 */

const topicSchema = z.object({
  name: z.string().min(2).max(200),
  description: z.string().max(1000).optional(),
  category: z.enum([
    'JAVA', 'SPRING', 'DATABASE', 'KAFKA', 'CLOUD',
    'ALGORITHMS', 'SYSTEM_DESIGN', 'AI_ML', 'INTEGRATION',
    'OBSERVABILITY', 'TESTING', 'BEHAVIORAL',
  ]),
  priority: z.enum(['P0', 'P1', 'P2']),
  status: z.enum(['NOT_STARTED', 'IN_PROGRESS', 'STUDIED', 'QUIZ_READY', 'MASTERED']),
});

export function TopicsPage() {
  const [showForm, setShowForm] = useState(false);
  const { hasAnyRole, hasRole } = useAuth();
  const { data: topics, isLoading } = useTopics();
  const createTopic = useCreateTopic();
  const deleteTopic = useDeleteTopic();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<TopicInput>({ resolver: zodResolver(topicSchema) });

  const onSubmit = async (data: TopicInput) => {
    await createTopic.mutateAsync(data);
    reset();
    setShowForm(false);
  };

  if (isLoading) return <div className="p-8">Loading topics...</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold">Topics</h1>
        {/* Role-conditional UI — only EDITOR/ADMIN can create */}
        {hasAnyRole('EDITOR', 'ADMIN') && (
          <button
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded hover:opacity-90"
          >
            <Plus className="w-4 h-4" />
            New Topic
          </button>
        )}
      </div>

      {/* Create form */}
      {showForm && (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow mb-6 space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium">Name</label>
              <input
                {...register('name')}
                className="w-full px-3 py-2 border rounded mt-1"
                placeholder="e.g., Saga Pattern"
              />
              {errors.name && <p className="text-red-500 text-sm">{errors.name.message}</p>}
            </div>

            <div>
              <label className="text-sm font-medium">Category</label>
              <select {...register('category')} className="w-full px-3 py-2 border rounded mt-1">
                {['JAVA','SPRING','KAFKA','CLOUD','AI_ML','SYSTEM_DESIGN','ALGORITHMS','INTEGRATION'].map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-sm font-medium">Priority</label>
              <select {...register('priority')} className="w-full px-3 py-2 border rounded mt-1">
                <option value="P0">P0 — Must know</option>
                <option value="P1">P1 — Should know</option>
                <option value="P2">P2 — Shows depth</option>
              </select>
            </div>

            <div>
              <label className="text-sm font-medium">Status</label>
              <select {...register('status')} className="w-full px-3 py-2 border rounded mt-1">
                <option value="NOT_STARTED">Not started</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="STUDIED">Studied</option>
                <option value="QUIZ_READY">Quiz ready</option>
                <option value="MASTERED">Mastered</option>
              </select>
            </div>
          </div>

          <div>
            <label className="text-sm font-medium">Description</label>
            <textarea
              {...register('description')}
              rows={2}
              className="w-full px-3 py-2 border rounded mt-1"
              placeholder="Optional description"
            />
          </div>

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-4 py-2 bg-primary text-white rounded disabled:opacity-50"
            >
              {isSubmitting ? 'Creating...' : 'Create'}
            </button>
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="px-4 py-2 border rounded"
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      {/* Topics list */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50 dark:bg-gray-700 text-left text-sm">
            <tr>
              <th className="p-3">Name</th>
              <th className="p-3">Category</th>
              <th className="p-3">Priority</th>
              <th className="p-3">Status</th>
              {hasRole('ADMIN') && <th className="p-3">Actions</th>}
            </tr>
          </thead>
          <tbody>
            {topics?.map(topic => (
              <TopicRow
                key={topic.id}
                topic={topic}
                isAdmin={hasRole('ADMIN')}
                onDelete={(id) => deleteTopic.mutate(id)}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function TopicRow({
  topic,
  isAdmin,
  onDelete,
}: {
  topic: Topic;
  isAdmin: boolean;
  onDelete: (id: number) => void;
}) {
  return (
    <tr className="border-t hover:bg-gray-50 dark:hover:bg-gray-700">
      <td className="p-3 font-medium">{topic.name}</td>
      <td className="p-3 text-sm text-gray-500">{topic.category}</td>
      <td className="p-3">
        <span className={`px-2 py-0.5 rounded text-xs ${
          topic.priority === 'P0' ? 'bg-red-100 text-red-700' :
          topic.priority === 'P1' ? 'bg-yellow-100 text-yellow-700' :
          'bg-gray-100 text-gray-700'
        }`}>
          {topic.priority}
        </span>
      </td>
      <td className="p-3 text-sm">{topic.status.replace('_', ' ')}</td>
      {isAdmin && (
        <td className="p-3">
          <button
            onClick={() => onDelete(topic.id)}
            className="text-red-500 hover:text-red-700"
            aria-label="Delete topic"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        </td>
      )}
    </tr>
  );
}
