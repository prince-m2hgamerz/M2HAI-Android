import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  Bot,
  CheckCircle2,
  FileUp,
  Image,
  MessagesSquare,
  Plus,
  RefreshCw,
  Save,
  Search,
  Send,
  Settings2,
  Trash2,
  Webhook,
  XCircle,
} from 'lucide-react';
import {
  deleteTelegramBot,
  getModels,
  getTelegramBotLogs,
  getTelegramBots,
  setupTelegramWebhook,
  updateTelegramBot,
  upsertTelegramBot,
  type AIModel,
  type TelegramBot,
  type TelegramBotLog,
  type TelegramBotUpsert,
} from '../lib/api';
import styles from './TelegramBotsPage.module.css';

const EMPTY_BOT: TelegramBotUpsert = {
  name: 'M2HAI Assistant',
  username: '',
  token_hint: '',
  is_enabled: true,
  model_id: 'meta/llama-3.1-8b-instruct',
  fallback_model_id: '',
  system_prompt: '',
  personality: 'Helpful, concise, and professional.',
  temperature: 0.5,
  max_tokens: 1024,
  welcome_message: 'Welcome to M2HAI. Send a message to start.',
  commands: ['start', 'help', 'model', 'image'],
  rate_limit_per_minute: 12,
  daily_message_limit: 300,
  enable_voice: false,
  enable_images: true,
  enable_files: false,
  language_mode: 'auto',
  webhook_url: '',
  avatar_url: '',
  banner_url: '',
};

export default function TelegramBotsPage() {
  const [bots, setBots] = useState<TelegramBot[]>([]);
  const [logs, setLogs] = useState<TelegramBotLog[]>([]);
  const [models, setModels] = useState<AIModel[]>([]);
  const [selectedId, setSelectedId] = useState<string>('new');
  const [form, setForm] = useState<TelegramBotUpsert>(EMPTY_BOT);
  const [botToken, setBotToken] = useState('');
  const [webhookBaseUrl, setWebhookBaseUrl] = useState(import.meta.env.VITE_SUPABASE_URL || '');
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [webhooking, setWebhooking] = useState(false);
  const [banner, setBanner] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const activeModels = useMemo(() => models.filter(model => model.is_active), [models]);
  const selectedBot = bots.find(bot => bot.id === selectedId) ?? null;
  const filteredBots = useMemo(() => {
    const text = query.trim().toLowerCase();
    if (!text) return bots;
    return bots.filter(bot =>
      bot.name.toLowerCase().includes(text) ||
      (bot.username ?? '').toLowerCase().includes(text) ||
      bot.model_id.toLowerCase().includes(text)
    );
  }, [bots, query]);

  const stats = useMemo(() => {
    const enabled = bots.filter(bot => bot.is_enabled).length;
    return {
      total: bots.length,
      enabled,
      images: bots.filter(bot => bot.enable_images).length,
      voice: bots.filter(bot => bot.enable_voice).length,
    };
  }, [bots]);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const [nextBots, nextModels, nextLogs] = await Promise.all([
        getTelegramBots(),
        getModels(),
        getTelegramBotLogs(undefined, 80),
      ]);
      setBots(nextBots);
      setModels(nextModels);
      setLogs(nextLogs);
      if (selectedId !== 'new' && !nextBots.some(bot => bot.id === selectedId)) {
        setSelectedId('new');
        setForm(defaultForm(nextModels));
      }
      if (selectedId === 'new') {
        setForm(current => ({
          ...current,
          model_id: current.model_id || nextModels.find(model => model.is_active)?.id || EMPTY_BOT.model_id,
        }));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  useEffect(() => {
    if (selectedId === 'new') {
      setForm(defaultForm(models));
      setBotToken('');
      return;
    }

    const bot = bots.find(item => item.id === selectedId);
    if (!bot) return;
    setForm({
      ...bot,
      fallback_model_id: bot.fallback_model_id ?? '',
      username: bot.username ?? '',
      token_hint: bot.token_hint ?? '',
      webhook_url: bot.webhook_url ?? '',
      avatar_url: bot.avatar_url ?? '',
      banner_url: bot.banner_url ?? '',
      bot_token: '',
    });
    setBotToken('');
  }, [bots, models, selectedId]);

  const setField = <K extends keyof TelegramBotUpsert>(field: K, value: TelegramBotUpsert[K]) => {
    setForm(current => ({ ...current, [field]: value }));
  };

  const save = async () => {
    if (!form.name.trim()) {
      setError('Bot name is required.');
      return;
    }
    if (!form.model_id) {
      setError('Select an active AI model for this bot.');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const saved = await upsertTelegramBot({
        ...form,
        id: selectedId === 'new' ? undefined : selectedId,
        name: form.name.trim(),
        username: normalizeUsername(form.username ?? ''),
        fallback_model_id: form.fallback_model_id || null,
        token_hint: form.token_hint || null,
        bot_token: botToken.trim() || undefined,
        webhook_url: form.webhook_url || null,
        avatar_url: form.avatar_url || null,
        banner_url: form.banner_url || null,
      });
      setBanner(selectedId === 'new' ? 'Telegram bot created' : 'Telegram bot updated');
      setSelectedId(saved.id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  };

  const toggleEnabled = async (bot: TelegramBot) => {
    await updateTelegramBot(bot.id, { is_enabled: !bot.is_enabled });
    setBanner(bot.is_enabled ? `${bot.name} disabled` : `${bot.name} enabled`);
    await load();
  };

  const remove = async (bot: TelegramBot) => {
    if (!confirm(`Delete Telegram bot "${bot.name}" and its logs?`)) return;
    await deleteTelegramBot(bot.id);
    setBanner(`${bot.name} deleted`);
    setSelectedId('new');
    await load();
  };

  const setupWebhook = async () => {
    if (selectedId === 'new') {
      setError('Save the bot before setting up the webhook.');
      return;
    }
    if (!botToken.trim()) {
      setError('Paste the bot token before setting the webhook.');
      return;
    }
    if (!webhookBaseUrl.trim()) {
      setError('Webhook base URL is required.');
      return;
    }

    setWebhooking(true);
    setError(null);
    try {
      const webhookUrl = await setupTelegramWebhook(selectedId, botToken.trim(), webhookBaseUrl.trim());
      setField('webhook_url', webhookUrl);
      setBanner('Telegram webhook configured');
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setWebhooking(false);
    }
  };

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <div>
          <span className={styles.eyebrow}><Send size={13} /> Telegram AI ecosystem</span>
          <h1 className={styles.title}>Telegram Bot Management</h1>
          <p className={styles.subtitle}>
            Add bots, assign active models, configure personality, control image and file support,
            set rate limits, and monitor webhook events from one admin surface.
          </p>
        </div>
        <div className={styles.headerActions}>
          <button className={styles.secondaryButton} onClick={load} disabled={loading}>
            <RefreshCw size={14} />{loading ? 'Loading...' : 'Refresh'}
          </button>
          <button className={styles.primaryButton} onClick={() => setSelectedId('new')}>
            <Plus size={14} />New Bot
          </button>
        </div>
      </div>

      {banner && <div className={styles.setupNote}>{banner}</div>}
      {error && <div className={styles.errorNote}>{error}</div>}

      <div className={styles.statsGrid}>
        <Stat icon={<Bot size={18} />} label="Total bots" value={stats.total} />
        <Stat icon={<CheckCircle2 size={18} />} label="Enabled" value={stats.enabled} />
        <Stat icon={<Image size={18} />} label="Image enabled" value={stats.images} />
        <Stat icon={<Activity size={18} />} label="Voice enabled" value={stats.voice} />
      </div>

      <div className={styles.layout}>
        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Bots</h2>
              <p>{filteredBots.length} visible from {bots.length} configured</p>
            </div>
          </div>
          <label className={styles.searchBox}>
            <Search size={14} />
            <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search bots or models" />
          </label>

          <div className={styles.botList}>
            {loading ? (
              <div className={styles.emptyState}>Loading bots...</div>
            ) : filteredBots.length === 0 ? (
              <div className={styles.emptyState}>No Telegram bots configured yet.</div>
            ) : filteredBots.map(bot => (
              <article
                key={bot.id}
                className={`${styles.botCard} ${bot.id === selectedId ? styles.botCardActive : ''}`}
              >
                <button className={styles.botTop} onClick={() => setSelectedId(bot.id)}>
                  <span className={styles.botIdentity}>
                    <span className={styles.botAvatar}><Bot size={18} /></span>
                    <span>
                      <span className={styles.botName}>{bot.name}</span>
                      <span className={styles.botUsername}>{bot.username ? `@${bot.username}` : 'Username not set'}</span>
                    </span>
                  </span>
                  <span className={bot.is_enabled ? styles.badgeGreen : styles.badgeRed}>
                    {bot.is_enabled ? 'Enabled' : 'Disabled'}
                  </span>
                </button>
                <div className={styles.botMeta}>
                  <span className={styles.badgeBlue}>{bot.model_id}</span>
                  {bot.enable_images && <span className={styles.badge}><Image size={11} /> Images</span>}
                  {bot.enable_files && <span className={styles.badge}><FileUp size={11} /> Files</span>}
                  {bot.webhook_url ? <span className={styles.badgeGreen}>Webhook</span> : <span className={styles.badgeMuted}>No webhook</span>}
                </div>
                <div className={styles.botActions}>
                  <button className={styles.iconButton} onClick={() => toggleEnabled(bot)} title={bot.is_enabled ? 'Disable bot' : 'Enable bot'}>
                    {bot.is_enabled ? <XCircle size={14} /> : <CheckCircle2 size={14} />}
                  </button>
                  <button className={styles.dangerButton} onClick={() => remove(bot)} title="Delete bot">
                    <Trash2 size={14} />
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>{selectedBot ? `Configure ${selectedBot.name}` : 'Create Telegram Bot'}</h2>
              <p>Tokens are stored server-side and shown only as a masked hint.</p>
            </div>
            <span className={form.is_enabled ? styles.badgeGreen : styles.badgeRed}>
              {form.is_enabled ? 'Live' : 'Disabled'}
            </span>
          </div>

          <div className={styles.form}>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>Bot name</span>
                <input className={styles.input} value={form.name} onChange={event => setField('name', event.target.value)} />
              </label>
              <label className={styles.field}>
                <span>Telegram username</span>
                <input className={styles.input} value={form.username ?? ''} onChange={event => setField('username', event.target.value)} placeholder="@your_bot" />
              </label>
            </div>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>Bot token</span>
                <input
                  className={styles.input}
                  value={botToken}
                  onChange={event => setBotToken(event.target.value)}
                  placeholder={form.token_hint ? `Configured: ${form.token_hint}` : 'Paste token from BotFather'}
                />
              </label>
              <label className={styles.field}>
                <span>Language mode</span>
                <select className={styles.select} value={form.language_mode} onChange={event => setField('language_mode', event.target.value)}>
                  <option value="auto">Auto detect</option>
                  <option value="en">English</option>
                  <option value="hi">Hindi</option>
                  <option value="ur">Urdu</option>
                  <option value="bn">Bengali</option>
                </select>
              </label>
            </div>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>Primary model</span>
                <select className={styles.select} value={form.model_id} onChange={event => setField('model_id', event.target.value)}>
                  {activeModels.length === 0 && <option value={form.model_id}>{form.model_id}</option>}
                  {activeModels.map(model => <option key={model.id} value={model.id}>{model.name} - {model.provider}</option>)}
                </select>
              </label>
              <label className={styles.field}>
                <span>Fallback model</span>
                <select className={styles.select} value={form.fallback_model_id ?? ''} onChange={event => setField('fallback_model_id', event.target.value)}>
                  <option value="">No fallback</option>
                  {activeModels.map(model => <option key={model.id} value={model.id}>{model.name} - {model.provider}</option>)}
                </select>
              </label>
            </div>

            <label className={styles.field}>
              <span>System prompt</span>
              <textarea
                className={styles.textarea}
                value={form.system_prompt}
                onChange={event => setField('system_prompt', event.target.value)}
                placeholder="Bot-specific system instruction..."
              />
            </label>

            <label className={styles.field}>
              <span>Personality</span>
              <textarea
                className={styles.textarea}
                value={form.personality}
                onChange={event => setField('personality', event.target.value)}
              />
            </label>

            <label className={styles.field}>
              <span>Welcome message</span>
              <textarea
                className={styles.textarea}
                value={form.welcome_message}
                onChange={event => setField('welcome_message', event.target.value)}
              />
            </label>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>Commands</span>
                <input
                  className={styles.input}
                  value={(form.commands ?? []).join(', ')}
                  onChange={event => setField('commands', splitCommands(event.target.value))}
                  placeholder="start, help, model, image"
                />
              </label>
              <label className={styles.field}>
                <span>Temperature</span>
                <div className={styles.rangeRow}>
                  <input
                    type="range"
                    min="0"
                    max="2"
                    step="0.1"
                    value={form.temperature}
                    onChange={event => setField('temperature', Number(event.target.value))}
                  />
                  <input
                    className={styles.input}
                    type="number"
                    min="0"
                    max="2"
                    step="0.1"
                    value={form.temperature}
                    onChange={event => setField('temperature', Number(event.target.value))}
                  />
                </div>
              </label>
            </div>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>Max tokens</span>
                <input className={styles.input} type="number" min="64" step="64" value={form.max_tokens} onChange={event => setField('max_tokens', Number(event.target.value))} />
              </label>
              <label className={styles.field}>
                <span>Rate limit per minute</span>
                <input className={styles.input} type="number" min="1" value={form.rate_limit_per_minute} onChange={event => setField('rate_limit_per_minute', Number(event.target.value))} />
              </label>
            </div>

            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>Daily message limit</span>
                <input className={styles.input} type="number" min="1" value={form.daily_message_limit} onChange={event => setField('daily_message_limit', Number(event.target.value))} />
              </label>
              <label className={styles.field}>
                <span>Avatar URL</span>
                <input className={styles.input} value={form.avatar_url ?? ''} onChange={event => setField('avatar_url', event.target.value)} placeholder="https://..." />
              </label>
            </div>

            <div className={styles.featureToggles}>
              <label className={styles.toggleField}>
                <span>Enabled</span>
                <input type="checkbox" checked={form.is_enabled} onChange={event => setField('is_enabled', event.target.checked)} />
              </label>
              <label className={styles.toggleField}>
                <span>Voice</span>
                <input type="checkbox" checked={form.enable_voice} onChange={event => setField('enable_voice', event.target.checked)} />
              </label>
              <label className={styles.toggleField}>
                <span>Images</span>
                <input type="checkbox" checked={form.enable_images} onChange={event => setField('enable_images', event.target.checked)} />
              </label>
              <label className={styles.toggleField}>
                <span>Files</span>
                <input type="checkbox" checked={form.enable_files} onChange={event => setField('enable_files', event.target.checked)} />
              </label>
            </div>

            <div className={styles.webhookBox}>
              <strong><Webhook size={14} /> Webhook setup</strong>
              <p>Save the bot first, paste the token, then set the webhook. The backend function should handle Telegram updates at `/functions/v1/telegram-bot`.</p>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span>Webhook base URL</span>
                  <input className={styles.input} value={webhookBaseUrl} onChange={event => setWebhookBaseUrl(event.target.value)} />
                </label>
                <label className={styles.field}>
                  <span>Current webhook URL</span>
                  <input className={styles.input} value={form.webhook_url ?? ''} onChange={event => setField('webhook_url', event.target.value)} placeholder="Not configured" />
                </label>
              </div>
              <button className={styles.secondaryButton} onClick={setupWebhook} disabled={webhooking || selectedId === 'new'}>
                <Webhook size={14} />{webhooking ? 'Setting webhook...' : 'Set Telegram Webhook'}
              </button>
            </div>

            <div className={styles.formActions}>
              <span className={styles.badge}><Settings2 size={12} /> {activeModels.length} active app models available</span>
              <button className={styles.primaryButton} onClick={save} disabled={saving}>
                <Save size={14} />{saving ? 'Saving...' : 'Save Bot'}
              </button>
            </div>
          </div>
        </section>

        <section className={styles.logPanel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>Bot Logs</h2>
              <p>Webhook, user activity, error tracking, and AI runtime events.</p>
            </div>
            <span className={styles.badge}><MessagesSquare size={12} /> {logs.length} events</span>
          </div>
          <div className={styles.logList}>
            {logs.length === 0 ? (
              <div className={styles.emptyState}>No Telegram bot logs recorded yet.</div>
            ) : logs.map(log => (
              <article className={styles.logItem} key={log.id}>
                <span className={log.level === 'error' ? styles.badgeRed : log.level === 'warning' ? styles.badge : styles.badgeGreen}>
                  {log.level}
                </span>
                <div>
                  <strong>{log.event}</strong>
                  <p>{log.message}</p>
                </div>
                <span className={styles.logTime}>{new Date(log.created_at).toLocaleString()}</span>
              </article>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}

function Stat({ icon, label, value }: { icon: React.ReactNode; label: string; value: number }) {
  return (
    <div className={styles.statCard}>
      <div className={styles.statIcon}>{icon}</div>
      <div>
        <strong>{value.toLocaleString()}</strong>
        <span>{label}</span>
      </div>
    </div>
  );
}

function defaultForm(models: AIModel[]): TelegramBotUpsert {
  const firstActive = models.find(model => model.is_active)?.id;
  return {
    ...EMPTY_BOT,
    model_id: firstActive || EMPTY_BOT.model_id,
  };
}

function normalizeUsername(value: string): string | null {
  const clean = value.trim().replace(/^@/, '');
  return clean || null;
}

function splitCommands(value: string): string[] {
  return value
    .split(',')
    .map(command => command.trim().replace(/^\//, ''))
    .filter(Boolean);
}
