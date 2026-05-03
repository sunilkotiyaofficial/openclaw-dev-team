import { useTopics } from '@/hooks/useTopics';
import { useAuth } from '@/auth/AuthContext';
import { Library, CheckCircle2, Clock, Flame, TrendingUp } from 'lucide-react';
import clsx from 'clsx';

/**
 * Dashboard — interview prep stats overview.
 *
 * Demonstrates:
 *  - useTopics hook (TanStack Query — cached, auto-refetch)
 *  - JS reduce/filter mirroring Java Stream patterns
 *  - Loading + error states from useQuery
 *  - Tailwind layout primitives (grid, flex, gradients)
 */
export function DashboardPage() {
  const { user } = useAuth();
  const { data: topics, isLoading, error } = useTopics();

  if (isLoading) {
    return (
      <div className="p-10">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-64" />
          <div className="h-4 bg-gray-200 rounded w-96" />
          <div className="grid grid-cols-4 gap-4 mt-8">
            {[0, 1, 2, 3].map(i => (
              <div key={i} className="h-28 bg-gray-200 rounded-xl" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-10">
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-4">
          Failed to load topics. Please refresh.
        </div>
      </div>
    );
  }

  const list = topics ?? [];
  const total = list.length;
  const mastered = list.filter(t => t.status === 'MASTERED').length;
  const inProgress = list.filter(t => t.status === 'IN_PROGRESS').length;
  const p0 = list.filter(t => t.priority === 'P0').length;

  const masteryPct = total === 0 ? 0 : Math.round((mastered / total) * 100);

  const byCategory = list.reduce<Record<string, number>>((acc, t) => {
    acc[t.category] = (acc[t.category] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <div className="p-10 max-w-7xl">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Welcome back, {user?.username} 👋
        </h1>
        <p className="text-gray-600 mt-1">Here's your interview prep overview.</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <StatCard
          icon={<Library className="w-5 h-5" />}
          accent="blue"
          label="Total Topics"
          value={total}
          hint="across all categories"
        />
        <StatCard
          icon={<CheckCircle2 className="w-5 h-5" />}
          accent="emerald"
          label="Mastered"
          value={mastered}
          hint={total ? `${masteryPct}% of total` : 'getting started'}
        />
        <StatCard
          icon={<Clock className="w-5 h-5" />}
          accent="amber"
          label="In Progress"
          value={inProgress}
          hint="actively studying"
        />
        <StatCard
          icon={<Flame className="w-5 h-5" />}
          accent="red"
          label="P0 Topics"
          value={p0}
          hint="must-know for interview"
        />
      </div>

      {/* Category breakdown + mastery progress */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Category breakdown */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-6 shadow-sm">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-bold text-gray-900">Topics by Category</h2>
            <span className="text-xs text-gray-500">
              {Object.keys(byCategory).length} categories
            </span>
          </div>

          {total === 0 ? (
            <EmptyState message="No topics yet — head to the Topics page to add your first one." />
          ) : (
            <div className="space-y-3.5">
              {Object.entries(byCategory)
                .sort(([, a], [, b]) => b - a)
                .map(([category, count]) => (
                  <div key={category}>
                    <div className="flex items-center justify-between mb-1.5">
                      <span className="text-sm font-medium text-gray-800">
                        {category.replace(/_/g, ' ')}
                      </span>
                      <span className="text-xs text-gray-500">
                        {count} {count === 1 ? 'topic' : 'topics'}
                      </span>
                    </div>
                    <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-blue-500 to-emerald-500 rounded-full transition-all"
                        style={{ width: `${(count / total) * 100}%` }}
                      />
                    </div>
                  </div>
                ))}
            </div>
          )}
        </div>

        {/* Mastery progress card */}
        <div className="bg-gradient-to-br from-blue-600 to-emerald-600 rounded-xl p-6 shadow-sm text-white">
          <div className="flex items-center gap-2 mb-1">
            <TrendingUp className="w-5 h-5" />
            <span className="text-sm font-semibold">Mastery Progress</span>
          </div>
          <div className="text-5xl font-bold mt-3">{masteryPct}%</div>
          <p className="text-sm text-blue-50 mt-1">
            {mastered} of {total} topics mastered
          </p>
          <div className="mt-5 h-2 bg-white/20 rounded-full overflow-hidden">
            <div
              className="h-full bg-white rounded-full transition-all duration-500"
              style={{ width: `${masteryPct}%` }}
            />
          </div>
          <p className="text-xs text-blue-50 mt-4 leading-relaxed">
            {masteryPct >= 80
              ? 'Excellent — you\'re interview-ready.'
              : masteryPct >= 50
              ? 'Good progress. Focus on P0 topics.'
              : 'Just getting started. Add topics and mark progress.'}
          </p>
        </div>
      </div>
    </div>
  );
}

// ─── Local helper components ────────────────────────────────────────

const ACCENT_STYLES = {
  blue:    { iconBg: 'bg-blue-100',    iconText: 'text-blue-600' },
  emerald: { iconBg: 'bg-emerald-100', iconText: 'text-emerald-600' },
  amber:   { iconBg: 'bg-amber-100',   iconText: 'text-amber-600' },
  red:     { iconBg: 'bg-red-100',     iconText: 'text-red-600' },
} as const;

type AccentKey = keyof typeof ACCENT_STYLES;

function StatCard({
  icon,
  label,
  value,
  hint,
  accent,
}: {
  icon: React.ReactNode;
  label: string;
  value: number;
  hint?: string;
  accent: AccentKey;
}) {
  const style = ACCENT_STYLES[accent];
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between mb-3">
        <span className="text-sm font-medium text-gray-600">{label}</span>
        <div className={clsx('w-9 h-9 rounded-lg flex items-center justify-center', style.iconBg, style.iconText)}>
          {icon}
        </div>
      </div>
      <div className="text-3xl font-bold text-gray-900">{value}</div>
      {hint && <div className="text-xs text-gray-500 mt-1">{hint}</div>}
    </div>
  );
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="text-center py-8 text-sm text-gray-500">{message}</div>
  );
}
