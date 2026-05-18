import { useEffect, useMemo, useState } from 'react';
import {
  activateOnlyModels,
  createModel,
  deleteModel,
  getModels,
  syncProviderModels,
  testModel,
  testModels,
  updateModel,
  type AIModel,
  type ModelTestResult,
} from '../lib/api';
import {
  Bot,
  CheckCircle2,
  Download,
  Pencil,
  Play,
  Plus,
  RefreshCw,
  Save,
  Slash,
  Trash2,
  WandSparkles,
  X,
  XCircle,
} from 'lucide-react';
import styles from './TablePage.module.css';

const EMPTY: AIModel = {
  id: '',
  name: '',
  provider: 'NVIDIA',
  description: '',
  is_free: true,
  is_active: true,
};

const PROVIDERS = ['NVIDIA', 'Google', 'OpenAI', 'Anthropic', 'Meta', 'StabilityAI', 'Groq', 'Other'];

export default function ModelsPage() {
  const [models, setModels] = useState<AIModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<AIModel | null>(null);
  const [form, setForm] = useState<AIModel>(EMPTY);
  const [saving, setSaving] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [testingId, setTestingId] = useState<string | null>(null);
  const [testingAll, setTestingAll] = useState(false);
  const [progress, setProgress] = useState('');
  const [results, setResults] = useState<Record<string, ModelTestResult>>({});
  const [banner, setBanner] = useState<string | null>(null);

  const sortedModels = useMemo(() => {
    return [...models].sort((a, b) => `${a.provider}:${a.name}`.localeCompare(`${b.provider}:${b.name}`));
  }, [models]);

  const passingIds = useMemo(() => {
    return sortedModels.filter(model => results[model.id]?.ok).map(model => model.id);
  }, [results, sortedModels]);

  const load = () => {
    setLoading(true);
    getModels().then(setModels).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm(EMPTY);
    setShowModal(true);
  };

  const openEdit = (model: AIModel) => {
    setEditing(model);
    setForm(model);
    setShowModal(true);
  };

  const handleSave = async () => {
    if (!form.id || !form.name || !form.provider) return;
    setSaving(true);
    try {
      if (editing) {
        await updateModel(editing.id, {
          name: form.name,
          provider: form.provider,
          description: form.description,
          is_free: form.is_free,
          is_active: form.is_active,
        });
      } else {
        await createModel(form);
      }
      setShowModal(false);
      load();
      setBanner(`Saved ${form.name}`);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Delete model "${name}"?`)) return;
    await deleteModel(id);
    setBanner(`Deleted ${name}`);
    load();
  };

  const handleToggleActive = async (model: AIModel) => {
    await updateModel(model.id, { is_active: !model.is_active });
    setBanner(model.is_active ? `Hidden ${model.name}` : `Activated ${model.name}`);
    load();
  };

  const handleSync = async () => {
    setSyncing(true);
    try {
      const count = await syncProviderModels();
      load();
      setBanner(count > 0 ? `Synced ${count} provider models` : 'No models were returned by the provider catalog');
    } finally {
      setSyncing(false);
    }
  };

  const handleTest = async (model: AIModel) => {
    setTestingId(model.id);
    try {
      const reply = await testModel(model.id);
      const result = { id: model.id, ok: true, reply };
      setResults(current => ({ ...current, [model.id]: result }));
      setBanner(`${model.name}: ${reply.slice(0, 120)}`);
    } catch (error) {
      const reply = error instanceof Error ? error.message : String(error);
      const result = { id: model.id, ok: false, reply };
      setResults(current => ({ ...current, [model.id]: result }));
      setBanner(`${model.name} failed: ${reply.slice(0, 120)}`);
    } finally {
      setTestingId(null);
    }
  };

  const handleTestAll = async () => {
    setTestingAll(true);
    setResults({});
    setProgress(`Testing 0 of ${sortedModels.length}`);
    try {
      const allResults = await testModels(sortedModels, (result, index, total) => {
        setResults(current => ({ ...current, [result.id]: result }));
        setProgress(`Testing ${index} of ${total}`);
      });
      const passed = allResults.filter(result => result.ok).length;
      setBanner(`${passed} of ${allResults.length} models replied successfully`);
    } finally {
      setTestingAll(false);
      setProgress('');
    }
  };

  const handleActivatePassing = async () => {
    if (passingIds.length === 0) {
      setBanner('No passing test results to activate');
      return;
    }
    if (!confirm(`Activate only ${passingIds.length} tested working models and hide all other models?`)) return;
    await activateOnlyModels(passingIds);
    setBanner(`Activated ${passingIds.length} working models`);
    load();
  };

  const activeCount = sortedModels.filter(model => model.is_active).length;

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>AI Models</h1>
          <p className={styles.subtitle}>{sortedModels.length} registered models, {activeCount} active in Android</p>
        </div>
        <div className={styles.headerActions}>
          <button className={styles.btnRefresh} onClick={load}><RefreshCw size={14} />Refresh</button>
          <button className={styles.btnRefresh} onClick={handleSync} disabled={syncing || testingAll}>
            <Download size={14} />{syncing ? 'Syncing...' : 'Sync Catalog'}
          </button>
          <button className={styles.btnRefresh} onClick={handleTestAll} disabled={testingAll || sortedModels.length === 0}>
            <Play size={14} />{testingAll ? progress : 'Test All Models'}
          </button>
          <button className={styles.btnRefresh} onClick={handleActivatePassing} disabled={testingAll || passingIds.length === 0}>
            <WandSparkles size={14} />Activate Passing
          </button>
          <button className={styles.btnPrimary} onClick={openCreate}><Plus size={14} />Add Model</button>
        </div>
      </div>

      {banner && <div className={styles.setupNote}>{banner}</div>}

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Model</th>
              <th>Provider</th>
              <th>Description</th>
              <th>Free</th>
              <th>Android</th>
              <th>Test</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className={styles.loadingCell}><div className={styles.spinner} /></td></tr>
            ) : sortedModels.length === 0 ? (
              <tr><td colSpan={7} className={styles.emptyCell}>No models registered</td></tr>
            ) : sortedModels.map(model => {
              const result = results[model.id];
              return (
                <tr key={model.id} className={styles.row}>
                  <td>
                    <div className={styles.modelCell}>
                      <div className={styles.modelTitle}>
                        <Bot size={14} style={{ color: 'var(--primary)' }} />
                        <span className={styles.name}>{model.name}</span>
                      </div>
                      <span className={styles.monoMuted}>{model.id}</span>
                    </div>
                  </td>
                  <td><span className={`${styles.tag} ${styles.tagBlue}`}>{model.provider}</span></td>
                  <td className={styles.descriptionCell}>{model.description || '-'}</td>
                  <td>
                    <span className={`${styles.tag} ${model.is_free ? styles.tagGreen : styles.tagRed}`}>
                      {model.is_free ? 'Free' : 'Paid'}
                    </span>
                  </td>
                  <td>
                    <span className={`${styles.tag} ${model.is_active ? styles.tagGreen : styles.tagRed}`}>
                      {model.is_active ? 'Active' : 'Hidden'}
                    </span>
                  </td>
                  <td>
                    {result ? (
                      <div className={styles.testResult}>
                        <span className={`${styles.tag} ${result.ok ? styles.tagGreen : styles.tagRed}`}>
                          {result.ok ? 'Reply OK' : 'Failed'}
                        </span>
                        <span title={result.reply}>{result.reply.slice(0, 80)}</span>
                      </div>
                    ) : (
                      <span className={styles.muted}>Not tested</span>
                    )}
                  </td>
                  <td>
                    <div className={styles.actionRow}>
                      <button className={styles.btnIcon} onClick={() => openEdit(model)} title="Edit model">
                        <Pencil size={13} />
                      </button>
                      <button className={styles.btnIcon} onClick={() => handleToggleActive(model)} title={model.is_active ? 'Hide from Android' : 'Show in Android'}>
                        {model.is_active ? <Slash size={13} /> : <CheckCircle2 size={13} />}
                      </button>
                      <button className={styles.btnIcon} onClick={() => handleTest(model)} disabled={testingId === model.id || testingAll} title="Test model">
                        {results[model.id]?.ok === false ? <XCircle size={13} /> : <Play size={13} />}
                      </button>
                      <button className={styles.btnDanger} onClick={() => handleDelete(model.id, model.name)} title="Delete model">
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className={styles.modalOverlay} onClick={() => setShowModal(false)}>
          <div className={styles.modal} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>{editing ? 'Edit Model' : 'Add New Model'}</h2>
              <button onClick={() => setShowModal(false)} className={styles.btnDanger} style={{ width: 28, height: 28 }}><X size={13} /></button>
            </div>

            <div className={styles.field}>
              <label className={styles.label}>Model ID *</label>
              <input
                className={styles.input}
                placeholder="e.g. gemini-2.5-flash"
                value={form.id}
                disabled={Boolean(editing)}
                onChange={event => setForm(current => ({ ...current, id: event.target.value }))}
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Display Name *</label>
              <input
                className={styles.input}
                placeholder="e.g. Gemini 2.5 Flash"
                value={form.name}
                onChange={event => setForm(current => ({ ...current, name: event.target.value }))}
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Provider *</label>
              <select className={styles.select} value={form.provider} onChange={event => setForm(current => ({ ...current, provider: event.target.value }))}>
                {PROVIDERS.map(provider => <option key={provider} value={provider}>{provider}</option>)}
              </select>
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Description</label>
              <textarea
                className={styles.textarea}
                placeholder="Short description..."
                value={form.description ?? ''}
                rows={3}
                onChange={event => setForm(current => ({ ...current, description: event.target.value }))}
              />
            </div>
            <div className={styles.field}>
              <label className={styles.checkboxLabel}>
                <input type="checkbox" checked={form.is_free} onChange={event => setForm(current => ({ ...current, is_free: event.target.checked }))} />
                Free model
              </label>
            </div>
            <div className={styles.field}>
              <label className={styles.checkboxLabel}>
                <input type="checkbox" checked={form.is_active} onChange={event => setForm(current => ({ ...current, is_active: event.target.checked }))} />
                Visible to Android users
              </label>
            </div>

            <div className={styles.modalActions}>
              <button className={styles.btnCancel} onClick={() => setShowModal(false)}>Cancel</button>
              <button className={styles.btnPrimary} onClick={handleSave} disabled={saving || !form.id || !form.name || !form.provider}>
                <Save size={14} />{saving ? 'Saving...' : editing ? 'Update Model' : 'Create Model'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
