import { useState } from 'react';
import { useTopics } from '@/hooks/useTopics';

/**
 * Notes page — placeholder showing the structure for SSE streaming.
 *
 * In a full implementation, this connects to /api/notes/stream/{topicId}
 * via EventSource for live updates from the reactive backend.
 */
export function NotesPage() {
  const { data: topics } = useTopics();
  const [selectedTopicId, setSelectedTopicId] = useState<number | undefined>();

  return (
    <div className="p-8">
      <h1 className="text-3xl font-bold mb-6">Notes</h1>

      <div className="grid grid-cols-3 gap-6">
        {/* Topic picker */}
        <div>
          <h2 className="text-sm font-medium mb-2">Select a topic</h2>
          <ul className="space-y-1">
            {topics?.map(t => (
              <li key={t.id}>
                <button
                  onClick={() => setSelectedTopicId(t.id)}
                  className={`w-full text-left px-3 py-2 rounded text-sm ${
                    selectedTopicId === t.id ? 'bg-primary/10 text-primary' : 'hover:bg-gray-100'
                  }`}
                >
                  {t.name}
                </button>
              </li>
            ))}
          </ul>
        </div>

        {/* Notes content area */}
        <div className="col-span-2">
          {selectedTopicId ? (
            <NotesForTopic topicId={selectedTopicId} />
          ) : (
            <div className="text-gray-500 italic">Select a topic to view notes.</div>
          )}
        </div>
      </div>
    </div>
  );
}

function NotesForTopic({ topicId }: { topicId: number }) {
  // In a full impl: useNotesByTopic(topicId) + SSE hook for live updates
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg p-6 shadow">
      <p className="text-sm text-gray-500 mb-2">Topic ID: {topicId}</p>
      <p className="italic text-gray-400">
        Notes UI is a stub — extend with React Hook Form Markdown editor +
        SSE EventSource on /api/notes/stream/{topicId} for live updates.
      </p>
      <pre className="mt-4 p-3 bg-gray-50 dark:bg-gray-900 rounded text-xs overflow-x-auto">
{`// SSE pattern (recommended implementation):

useEffect(() => {
  const evt = new EventSource(\`/api/notes/stream/\${topicId}\`);
  evt.onmessage = (e) => {
    const note = JSON.parse(e.data);
    setNotes(prev => [note, ...prev]);
  };
  return () => evt.close();
}, [topicId]);`}
      </pre>
    </div>
  );
}
