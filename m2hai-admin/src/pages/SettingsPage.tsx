import { useEffect, useMemo, useState } from 'react';
import { Bot, Calendar, Download, FileText, Fingerprint, Globe, Image, Key, Megaphone, Package, Save, Shield, SlidersHorizontal, Smartphone, UserPlus } from 'lucide-react';
import {
  DEFAULT_APP_SETTINGS,
  getAppSettings,
  getModels,
  uploadReleaseApk,
  updateAppSettings,
  type AIModel,
  type AppSettings,
} from '../lib/api';
import styles from './SettingsPage.module.css';

export default function SettingsPage() {
  const [settings, setSettings] = useState<AppSettings>(DEFAULT_APP_SETTINGS);
  const [models, setModels] = useState<AIModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [apkFileName, setApkFileName] = useState<string | null>(null);
  const [uploadingApk, setUploadingApk] = useState(false);

  const activeModels = useMemo(() => models.filter(model => model.is_active), [models]);
  const updateReady = settings.update_apk_url.trim().length > 0 && settings.latest_version_code > 0;
  const releaseSummary = settings.update_release_notes
    .split('\n')
    .map(line => line.trim())
    .filter(Boolean)
    .slice(0, 4);
  const imageModels = useMemo(() => {
    const candidates = models.filter(model => {
      const text = `${model.id} ${model.name} ${model.description ?? ''}`.toLowerCase();
      return model.is_active && (
        text.includes('flux') ||
        text.includes('image') ||
        text.includes('diffusion') ||
        text.includes('sdxl') ||
        text.includes('imagen')
      );
    });
    return candidates.length > 0 ? candidates : activeModels;
  }, [activeModels, models]);

  useEffect(() => {
    Promise.all([getAppSettings(), getModels()])
      .then(([remoteSettings, remoteModels]) => {
        setSettings(remoteSettings);
        setModels(remoteModels);
      })
      .finally(() => setLoading(false));
  }, []);

  const save = async () => {
    setSaving(true);
    try {
      await updateAppSettings(settings);
      setStatus('App settings saved');
    } finally {
      setSaving(false);
    }
  };

  const handleApkUpload = async (file?: File | null) => {
    if (!file) return;
    setUploadingApk(true);
    try {
      const result = await uploadReleaseApk(file, settings.latest_version_name, settings.latest_version_code);
      setApkFileName(file.name);
      setSettings(current => ({
        ...current,
        update_apk_url: result.publicUrl,
        update_apk_size_mb: result.sizeMb,
        update_sha256: result.sha256,
      }));
      setStatus(`Uploaded ${file.name} to Supabase Storage`);
    } finally {
      setUploadingApk(false);
    }
  };

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <h1 className={styles.title}>Settings</h1>
        <p className={styles.subtitle}>Admin controls for the Android app and AI runtime</p>
      </div>

      <div className={styles.grid}>
        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <SlidersHorizontal size={18} />
            <span>Android App Control</span>
          </div>

          {loading ? (
            <p className={styles.infoValue}>Loading settings...</p>
          ) : (
            <div className={styles.form}>
              <label className={styles.toggleRow}>
                <input
                  type="checkbox"
                  checked={settings.app_enabled}
                  onChange={event => setSettings(current => ({ ...current, app_enabled: event.target.checked }))}
                />
                <span>App enabled</span>
              </label>

              <label className={styles.toggleRow}>
                <input
                  type="checkbox"
                  checked={settings.signup_enabled}
                  onChange={event => setSettings(current => ({ ...current, signup_enabled: event.target.checked }))}
                />
                <span>New user signup enabled</span>
              </label>

              <div className={styles.field}>
                <label className={styles.label}>Maintenance Message</label>
                <textarea
                  className={styles.textarea}
                  value={settings.maintenance_message}
                  onChange={event => setSettings(current => ({ ...current, maintenance_message: event.target.value }))}
                  rows={3}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Global Announcement</label>
                <textarea
                  className={styles.textarea}
                  value={settings.global_announcement}
                  onChange={event => setSettings(current => ({ ...current, global_announcement: event.target.value }))}
                  rows={3}
                  placeholder="Optional announcement returned with app settings..."
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Default Model</label>
                <select
                  className={styles.select}
                  value={settings.default_model_id}
                  onChange={event => setSettings(current => ({ ...current, default_model_id: event.target.value }))}
                >
                  {activeModels.length === 0 && <option value={settings.default_model_id}>{settings.default_model_id}</option>}
                  {activeModels.map(model => (
                    <option key={model.id} value={model.id}>{model.name} - {model.provider}</option>
                  ))}
                </select>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Image Generation Model</label>
                <select
                  className={styles.select}
                  value={settings.image_model_id}
                  onChange={event => setSettings(current => ({ ...current, image_model_id: event.target.value }))}
                >
                  {imageModels.length === 0 && <option value={settings.image_model_id}>{settings.image_model_id}</option>}
                  {imageModels.map(model => (
                    <option key={model.id} value={model.id}>{model.name} - {model.provider}</option>
                  ))}
                </select>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>System Prompt</label>
                <textarea
                  className={styles.textarea}
                  value={settings.system_prompt}
                  onChange={event => setSettings(current => ({ ...current, system_prompt: event.target.value }))}
                  rows={5}
                  placeholder="Optional global instruction applied to every chat..."
                />
              </div>

              <div className={styles.twoColumn}>
                <div className={styles.field}>
                  <label className={styles.label}>Temperature</label>
                  <input
                    className={styles.input}
                    type="number"
                    min="0"
                    max="2"
                    step="0.1"
                    value={settings.temperature}
                    onChange={event => setSettings(current => ({ ...current, temperature: Number(event.target.value) }))}
                  />
                </div>
                <div className={styles.field}>
                  <label className={styles.label}>Max Tokens</label>
                  <input
                    className={styles.input}
                    type="number"
                    min="64"
                    max="8192"
                    step="64"
                    value={settings.max_tokens}
                    onChange={event => setSettings(current => ({ ...current, max_tokens: Number(event.target.value) }))}
                  />
                </div>
              </div>

              <div className={styles.actions}>
                {status && <span className={styles.statusText}>{status}</span>}
                <button className={styles.saveButton} onClick={save} disabled={saving}>
                  <Save size={15} />{saving ? 'Saving...' : 'Save Controls'}
                </button>
              </div>
            </div>
          )}
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Bot size={18} />
            <span>AI Runtime Status</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Active Models</span>
            <span className={styles.infoValue}>{activeModels.length}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Default Model</span>
            <span className={styles.infoValue}>{settings.default_model_id}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Image Model</span>
            <span className={styles.infoValue}>{settings.image_model_id}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Signup</span>
            <span className={styles.infoValue}>{settings.signup_enabled ? 'Enabled' : 'Disabled'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>NVIDIA Key</span>
            <span className={styles.infoValueMasked}>Supabase Edge Function secret</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Gemini Key</span>
            <span className={styles.infoValueMasked}>Supabase Edge Function secret</span>
          </div>
          <div className={styles.statusBadge}>
            <span className={styles.dot} />
            {settings.app_enabled ? 'App accepting chat requests' : 'Maintenance mode'}
          </div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Image size={18} />
            <span>Image Generation</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Auto Routing</span>
            <span className={styles.infoValue}>Prompt keywords route to image model</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Current Model</span>
            <span className={styles.infoValue}>{settings.image_model_id}</span>
          </div>
          <div className={styles.statusBadge}>
            <span className={styles.dot} />
            Active image prompts use NVIDIA FLUX through Edge Function
          </div>
        </div>

        <div className={styles.cardWide}>
          <div className={styles.cardHeader}>
            <Smartphone size={18} />
            <span>Professional In-App Update</span>
          </div>

          {loading ? (
            <p className={styles.infoValue}>Loading update settings...</p>
          ) : (
            <div className={styles.form}>
              <div className={styles.releaseHeader}>
                <div>
                  <span className={styles.releaseEyebrow}>Release channel</span>
                  <strong>{settings.update_channel || 'stable'}</strong>
                </div>
                <div className={updateReady ? styles.releaseStateReady : styles.releaseStateDraft}>
                  {updateReady ? 'Ready to publish' : 'Draft'}
                </div>
              </div>

              <div className={styles.twoColumn}>
                <div className={styles.field}>
                  <label className={styles.label}>Latest Version Code</label>
                  <input
                    className={styles.input}
                    type="number"
                    min="1"
                    value={settings.latest_version_code}
                    onChange={event => setSettings(current => ({ ...current, latest_version_code: Number(event.target.value) }))}
                  />
                </div>
                <div className={styles.field}>
                  <label className={styles.label}>Latest Version Name</label>
                  <input
                    className={styles.input}
                    value={settings.latest_version_name}
                    onChange={event => setSettings(current => ({ ...current, latest_version_name: event.target.value }))}
                  />
                </div>
              </div>

              <div className={styles.twoColumn}>
                <div className={styles.field}>
                  <label className={styles.label}>Minimum Supported Code</label>
                  <input
                    className={styles.input}
                    type="number"
                    min="1"
                    value={settings.min_supported_version_code}
                    onChange={event => setSettings(current => ({ ...current, min_supported_version_code: Number(event.target.value) }))}
                  />
                </div>
                <label className={styles.toggleRow}>
                  <input
                    type="checkbox"
                    checked={settings.update_required}
                    onChange={event => setSettings(current => ({ ...current, update_required: event.target.checked }))}
                  />
                  <span>Force update</span>
                </label>
              </div>

              <div className={styles.twoColumn}>
                <div className={styles.field}>
                  <label className={styles.label}>Release Channel</label>
                  <select
                    className={styles.select}
                    value={settings.update_channel}
                    onChange={event => setSettings(current => ({ ...current, update_channel: event.target.value }))}
                  >
                    <option value="stable">Stable</option>
                    <option value="beta">Beta</option>
                    <option value="hotfix">Hotfix</option>
                    <option value="security">Security</option>
                  </select>
                </div>

                <div className={styles.field}>
                  <label className={styles.label}>Published Date</label>
                  <input
                    className={styles.input}
                    placeholder="2026-05-16"
                    value={settings.update_published_at}
                    onChange={event => setSettings(current => ({ ...current, update_published_at: event.target.value }))}
                  />
                </div>
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Update Title</label>
                <input
                  className={styles.input}
                  value={settings.update_title}
                  onChange={event => setSettings(current => ({ ...current, update_title: event.target.value }))}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Update Message</label>
                <textarea
                  className={styles.textarea}
                  rows={3}
                  value={settings.update_message}
                  onChange={event => setSettings(current => ({ ...current, update_message: event.target.value }))}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Release Notes</label>
                <textarea
                  className={styles.textarea}
                  rows={6}
                  placeholder="- Fixed streaming stability&#10;- Improved image generation&#10;- Better update installer"
                  value={settings.update_release_notes}
                  onChange={event => setSettings(current => ({ ...current, update_release_notes: event.target.value }))}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>APK Download URL</label>
                <input
                  className={styles.input}
                  placeholder="https://..."
                  value={settings.update_apk_url}
                  onChange={event => setSettings(current => ({ ...current, update_apk_url: event.target.value }))}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>Upload APK Package</label>
                <label className={styles.uploadBox}>
                  <input
                    type="file"
                    accept=".apk"
                    onChange={event => handleApkUpload(event.target.files?.[0] ?? null)}
                  />
                  <span>{uploadingApk ? 'Uploading APK...' : apkFileName ? `Uploaded: ${apkFileName}` : 'Choose release APK'}</span>
                </label>
              </div>

              <div className={styles.twoColumn}>
                <div className={styles.field}>
                  <label className={styles.label}>APK Size (MB)</label>
                  <input
                    className={styles.input}
                    type="number"
                    min="0"
                    step="0.1"
                    value={settings.update_apk_size_mb}
                    onChange={event => setSettings(current => ({ ...current, update_apk_size_mb: Number(event.target.value) }))}
                  />
                </div>

                <div className={styles.field}>
                  <label className={styles.label}>SHA-256 Checksum</label>
                  <input
                    className={styles.input}
                    placeholder="Optional package fingerprint"
                    value={settings.update_sha256}
                    onChange={event => setSettings(current => ({ ...current, update_sha256: event.target.value }))}
                  />
                </div>
              </div>

              <div className={styles.releasePreview}>
                <div className={styles.previewHeader}>
                  <Package size={16} />
                  <span>Android user preview</span>
                </div>
                <div className={styles.previewGrid}>
                  <span><Download size={14} /> {settings.latest_version_name} ({settings.latest_version_code})</span>
                  <span><Calendar size={14} /> {settings.update_published_at || 'No date'}</span>
                  <span><FileText size={14} /> {releaseSummary.length || 0} release notes</span>
                  <span><Fingerprint size={14} /> {settings.update_sha256 ? 'Checksum set' : 'No checksum'}</span>
                </div>
                {releaseSummary.length > 0 && (
                  <ul className={styles.releaseNotesList}>
                    {releaseSummary.map(note => <li key={note}>{note.replace(/^[-*•]\s*/, '')}</li>)}
                  </ul>
                )}
              </div>

              <div className={styles.actions}>
                <span className={styles.infoValueMasked}>
                  Users see version details, progress, release notes, and installer state inside Android.
                </span>
                <button className={styles.saveButton} onClick={save} disabled={saving}>
                  <Download size={15} />{saving ? 'Saving...' : 'Publish Update Settings'}
                </button>
              </div>
            </div>
          )}
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Megaphone size={18} />
            <span>Android Messaging</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Maintenance Text</span>
            <span className={styles.infoValue}>{settings.maintenance_message || 'Not set'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Announcement</span>
            <span className={styles.infoValue}>{settings.global_announcement || 'Not set'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Signup Control</span>
            <span className={styles.infoValue}>{settings.signup_enabled ? 'Users can register' : 'Registration blocked'}</span>
          </div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Globe size={18} />
            <span>Supabase Connection</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Project URL</span>
            <span className={styles.infoValue}>{import.meta.env.VITE_SUPABASE_URL || 'Not configured'}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Service Role Key</span>
            <span className={styles.infoValueMasked}>
              {import.meta.env.VITE_SUPABASE_SERVICE_ROLE
                ? '***************' + import.meta.env.VITE_SUPABASE_SERVICE_ROLE.slice(-8)
                : 'Not configured'}
            </span>
          </div>
          <div className={styles.statusBadge}>
            <span className={styles.dot} />
            Connected
          </div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Shield size={18} />
            <span>Admin Authentication</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Admin Email</span>
            <span className={styles.infoValue}>{import.meta.env.VITE_ADMIN_EMAIL}</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>2-Step Verification</span>
            <span className={styles.infoValue}>Telegram OTP</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Session Storage</span>
            <span className={styles.infoValue}>Browser sessionStorage</span>
          </div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <UserPlus size={18} />
            <span>User Controls</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Disable Users</span>
            <span className={styles.infoValue}>Available in Users</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Private Notes</span>
            <span className={styles.infoValue}>Available in Users</span>
          </div>
          <div className={styles.statusBadge}>
            <span className={styles.dot} />
            User lockout checked by Android app
          </div>
        </div>

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <Key size={18} />
            <span>Telegram Bot</span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Bot Token</span>
            <span className={styles.infoValueMasked}>
              {import.meta.env.VITE_TELEGRAM_BOT_TOKEN && import.meta.env.VITE_TELEGRAM_BOT_TOKEN !== 'YOUR_BOT_TOKEN_HERE'
                ? '********:' + import.meta.env.VITE_TELEGRAM_BOT_TOKEN.slice(-10)
                : 'Not configured'}
            </span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>Admin Chat ID</span>
            <span className={styles.infoValue}>
              {import.meta.env.VITE_TELEGRAM_ADMIN_CHAT_ID && import.meta.env.VITE_TELEGRAM_ADMIN_CHAT_ID !== 'YOUR_CHAT_ID_HERE'
                ? import.meta.env.VITE_TELEGRAM_ADMIN_CHAT_ID
                : 'Not configured'}
            </span>
          </div>
          <div className={styles.infoRow}>
            <span className={styles.infoLabel}>OTP Expiry</span>
            <span className={styles.infoValue}>5 minutes</span>
          </div>
        </div>
      </div>
    </div>
  );
}
