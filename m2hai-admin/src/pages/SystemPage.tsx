import { useEffect, useState } from 'react';
import { Activity, Bot, CheckCircle2, Database, RefreshCw, ServerCog, XCircle } from 'lucide-react';
import { getSystemHealth } from '../lib/api';
import styles from './SettingsPage.module.css';

type SystemHealth = Awaited<ReturnType<typeof getSystemHealth>>;

export default function SystemPage() {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    getSystemHealth()
      .then(setHealth)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  return (
    <div className={styles.root}>
      <div className={styles.headerRow}>
        <div>
          <h1 className={styles.title}>System</h1>
          <p className={styles.subtitle}>Supabase, Edge Function, model runtime, and Android control health</p>
        </div>
        <button className={styles.saveButton} onClick={load} disabled={loading}>
          <RefreshCw size={15} />{loading ? 'Checking...' : 'Run Health Check'}
        </button>
      </div>

      <div className={styles.grid}>
        <StatusCard
          icon={<Database size={18} />}
          label="Database"
          ok={health?.databaseOk ?? false}
          value={loading ? 'Checking...' : health?.databaseOk ? 'Connected' : 'Failed'}
        />
        <StatusCard
          icon={<ServerCog size={18} />}
          label="Edge Catalog"
          ok={health?.edgeCatalogOk ?? false}
          value={loading ? 'Checking...' : health?.edgeCatalogOk ? 'Catalog available' : 'Catalog failed'}
        />
        <StatusCard
          icon={<Activity size={18} />}
          label="Chat Runtime"
          ok={health?.edgeChatOk ?? false}
          value={loading ? 'Checking...' : health?.edgeChatOk ? 'Default model replies' : 'Chat test failed'}
        />
        <StatusCard
          icon={<Bot size={18} />}
          label="Active Android Models"
          ok={(health?.activeModels ?? 0) > 0}
          value={loading ? 'Checking...' : `${health?.activeModels ?? 0} active`}
        />

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Bot size={18} />
            <span>Default AI Model</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Model ID</span>
            <span className={styles.infoValue}>{health?.defaultModel ?? 'Checking...'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Android Source</span>
            <span className={styles.infoValue}>app_settings.default_model_id</span>
          </div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <ServerCog size={18} />
            <span>Health Details</span>
          </div>
          <p className={styles.setupNote}>{health?.details ?? 'Checking services...'}</p>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Activity size={18} />
            <span>Admin Control Coverage</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Users</span>
            <span className={styles.infoValue}>Enable, disable, notes, delete</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Chats</span>
            <span className={styles.infoValue}>View, rename, pin, archive, delete</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>AI Models</span>
            <span className={styles.infoValue}>Test, activate passing, sync, edit</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function StatusCard({ icon, label, ok, value }: {
  icon: React.ReactNode;
  label: string;
  ok: boolean;
  value: string;
}) {
  return (
    <div className={styles.card}>
      <div className={styles.cardHeader}>
        {icon}
        <span>{label}</span>
      </div>
      <div className={styles.statusLine}>
        {ok ? <CheckCircle2 size={18} /> : <XCircle size={18} />}
        <span className={ok ? styles.statusOk : styles.statusBad}>{value}</span>
      </div>
    </div>
  );
}
