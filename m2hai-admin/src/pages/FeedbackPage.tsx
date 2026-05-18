import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { ChevronLeft, ChevronRight, Copy, RefreshCw, Share2, ThumbsDown, ThumbsUp } from 'lucide-react';
import { getFeedbackStats, getMessageFeedback, type FeedbackStats, type MessageFeedback } from '../lib/api';
import styles from './TablePage.module.css';

const PAGE_SIZE = 20;

const actionIcon = {
  copy: <Copy size={13} />,
  like: <ThumbsUp size={13} />,
  unlike: <ThumbsDown size={13} />,
  share: <Share2 size={13} />,
};

export default function FeedbackPage() {
  const [feedback, setFeedback] = useState<MessageFeedback[]>([]);
  const [stats, setStats] = useState<FeedbackStats | null>(null);
  const [count, setCount] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [banner, setBanner] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    Promise.all([getFeedbackStats(), getMessageFeedback(page, PAGE_SIZE)])
      .then(([nextStats, result]) => {
        setStats(nextStats);
        setFeedback(result.data);
        setCount(result.count);
        setBanner(null);
      })
      .catch(error => {
        setBanner(error instanceof Error ? error.message : String(error));
        setFeedback([]);
        setCount(0);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [page]);

  const start = count === 0 ? 0 : page * PAGE_SIZE + 1;
  const end = Math.min((page + 1) * PAGE_SIZE, count);

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Response Feedback</h1>
          <p className={styles.subtitle}>Copy, like, unlike, and share actions from Android assistant responses</p>
        </div>
        <button className={styles.btnRefresh} onClick={load} disabled={loading}>
          <RefreshCw size={14} />{loading ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      {banner && <div className={styles.setupNote}>{banner}</div>}

      <div className={styles.feedbackStats}>
        <FeedbackStat icon={<Copy size={16} />} label="Copied" value={stats?.copy ?? 0} />
        <FeedbackStat icon={<ThumbsUp size={16} />} label="Liked" value={stats?.like ?? 0} />
        <FeedbackStat icon={<ThumbsDown size={16} />} label="Unliked" value={stats?.unlike ?? 0} />
        <FeedbackStat icon={<Share2 size={16} />} label="Shared" value={stats?.share ?? 0} />
      </div>

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Action</th>
              <th>Model</th>
              <th>User ID</th>
              <th>Chat ID</th>
              <th>Message ID</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className={styles.loadingCell}><div className={styles.spinner} /></td></tr>
            ) : feedback.length === 0 ? (
              <tr><td colSpan={6} className={styles.emptyCell}>No response feedback recorded yet</td></tr>
            ) : feedback.map(item => (
              <tr key={item.id} className={styles.row}>
                <td>
                  <span className={`${styles.tag} ${actionClass(item.action)}`}>
                    <span className={styles.tagIcon}>{actionIcon[item.action]}</span>
                    {item.action}
                  </span>
                </td>
                <td className={styles.monoMuted}>{item.model_id || 'Unknown'}</td>
                <td className={styles.monoMuted}>{item.user_id.slice(0, 8)}...</td>
                <td className={styles.monoMuted}>{item.chat_id.slice(0, 8)}...</td>
                <td className={styles.monoMuted}>{item.message_id.slice(0, 8)}...</td>
                <td className={styles.muted}>{new Date(item.created_at).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className={styles.pagination}>
        <span className={styles.pageInfo}>Showing {start}-{end} of {count}</span>
        <div className={styles.pageButtons}>
          <button className={styles.pageBtn} disabled={page === 0} onClick={() => setPage(current => current - 1)}>
            <ChevronLeft size={16} />
          </button>
          <span className={styles.pageCurrent}>{page + 1}</span>
          <button className={styles.pageBtn} disabled={(page + 1) * PAGE_SIZE >= count} onClick={() => setPage(current => current + 1)}>
            <ChevronRight size={16} />
          </button>
        </div>
      </div>
    </div>
  );
}

function FeedbackStat({ icon, label, value }: { icon: ReactNode; label: string; value: number }) {
  return (
    <div className={styles.feedbackStat}>
      <div className={styles.feedbackStatIcon}>{icon}</div>
      <div>
        <span>{value.toLocaleString()}</span>
        <p>{label}</p>
      </div>
    </div>
  );
}

function actionClass(action: MessageFeedback['action']) {
  if (action === 'like') return styles.tagGreen;
  if (action === 'unlike') return styles.tagRed;
  if (action === 'share') return styles.tagBlue;
  return styles.tagPurple;
}
