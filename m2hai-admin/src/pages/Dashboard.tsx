import React, { useEffect, useState } from 'react';
import { getDashboardStats, getPlatformOverview, getUserGrowth, subscribeToPlatformOverview, type DashboardStats, type PlatformOverview } from '../lib/api';
import { Users, MessageSquare, Bot, BarChart3, TrendingUp, Activity, UserPlus, MessagesSquare, Send, ServerCog } from 'lucide-react';
import styles from './Dashboard.module.css';

export default function Dashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [overview, setOverview] = useState<PlatformOverview | null>(null);
  const [growth, setGrowth] = useState<{ date: string; users: number; chats: number }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const load = () => Promise.all([getDashboardStats(), getUserGrowth(), getPlatformOverview()])
      .then(([s, g, o]) => { setStats(s); setGrowth(g); setOverview(o); })
      .finally(() => setLoading(false));
    load();
    return subscribeToPlatformOverview(load);
  }, []);

  if (loading) return (
    <div className={styles.loading}>
      <div className={styles.spinner} />
      <span>Loading dashboard...</span>
    </div>
  );

  const maxChats = Math.max(...growth.map(g => g.chats), 1);
  const maxUsers = Math.max(...growth.map(g => g.users), 1);

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Dashboard</h1>
          <p className={styles.subtitle}>Realtime M2HAI platform control center</p>
        </div>
        <div className={styles.badge}>
          <Activity size={12} />
          Live
        </div>
      </div>

      {/* Stat Cards */}
      <div className={styles.statsGrid}>
        <StatCard icon={<Users />} label="Total Users" value={stats?.totalUsers ?? 0} sub={`+${stats?.newUsersToday ?? 0} today`} color="purple" />
        <StatCard icon={<MessageSquare />} label="Total Chats" value={stats?.totalChats ?? 0} sub={`+${stats?.activeChatsToday ?? 0} today`} color="blue" />
        <StatCard icon={<MessagesSquare />} label="Messages Sent" value={stats?.totalMessages ?? 0} sub="All time" color="green" />
        <StatCard icon={<Bot />} label="AI Models" value={stats?.totalModels ?? 0} sub="Registered" color="orange" />
        <StatCard icon={<Activity />} label="AI Requests" value={overview?.aiRequests ?? 0} sub={`${overview?.apiUsage ?? 0} usage logs`} color="green" />
        <StatCard icon={<Send />} label="Telegram Bots" value={overview?.telegramBots ?? 0} sub="Enabled bots" color="blue" />
        <StatCard icon={<ServerCog />} label="System Uptime" value={overview?.systemUptime === 'Live' ? 1 : 0} sub={overview?.systemUptime ?? 'Checking'} color="purple" />
        <StatCard icon={<Bot />} label="Active Models" value={stats?.activeModels ?? 0} sub="Visible to users" color="orange" />
      </div>

      {/* Charts Row */}
      <div className={styles.chartsRow}>
        {/* Bar chart - Chats per day */}
        <div className={styles.chartCard}>
          <div className={styles.chartHeader}>
            <BarChart3 size={16} />
            <span>Chats — Last 7 Days</span>
          </div>
          <div className={styles.barChart}>
            {growth.map((g, i) => (
              <div key={i} className={styles.barGroup}>
                <div className={styles.barWrap}>
                  <div
                    className={styles.bar}
                    style={{ height: `${Math.max((g.chats / maxChats) * 100, 4)}%` }}
                    title={`${g.chats} chats`}
                  />
                </div>
                <span className={styles.barLabel}>{g.date.split(' ')[0]}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Line chart - Users per day */}
        <div className={styles.chartCard}>
          <div className={styles.chartHeader}>
            <TrendingUp size={16} />
            <span>New Users — Last 7 Days</span>
          </div>
          <div className={styles.barChart}>
            {growth.map((g, i) => (
              <div key={i} className={styles.barGroup}>
                <div className={styles.barWrap}>
                  <div
                    className={`${styles.bar} ${styles.barGreen}`}
                    style={{ height: `${Math.max((g.users / maxUsers) * 100, 4)}%` }}
                    title={`${g.users} users`}
                  />
                </div>
                <span className={styles.barLabel}>{g.date.split(' ')[0]}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Today's Summary */}
      <div className={styles.todaySummary}>
        <h3 className={styles.sectionTitle}>Today's Summary</h3>
        <div className={styles.summaryGrid}>
          <SummaryItem icon={<UserPlus size={18} />} label="New Signups" value={stats?.newUsersToday ?? 0} />
          <SummaryItem icon={<MessageSquare size={18} />} label="New Chats" value={stats?.activeChatsToday ?? 0} />
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, sub, color }: {
  icon: React.ReactNode; label: string; value: number; sub: string; color: string;
}) {
  return (
    <div className={`${styles.statCard} ${styles[`statCard_${color}`]}`}>
      <div className={styles.statIcon}>{icon}</div>
      <div className={styles.statInfo}>
        <span className={styles.statValue}>{value.toLocaleString()}</span>
        <span className={styles.statLabel}>{label}</span>
        <span className={styles.statSub}>{sub}</span>
      </div>
    </div>
  );
}

function SummaryItem({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <div className={styles.summaryItem}>
      <div className={styles.summaryIcon}>{icon}</div>
      <div>
        <p className={styles.summaryValue}>{value}</p>
        <p className={styles.summaryLabel}>{label}</p>
      </div>
    </div>
  );
}
