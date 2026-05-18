import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { motion, type Variants } from 'framer-motion';
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  CheckCircle2,
  Clock3,
  Download,
  FileText,
  Fingerprint,
  Package,
  ShieldCheck,
  Smartphone,
  Sparkles,
} from 'lucide-react';
import { getAppReleases, type AppRelease } from '../lib/api';
import styles from './DownloadPage.module.css';

const pageMotion: Variants = {
  hidden: { opacity: 0, y: 18 },
  show: { opacity: 1, y: 0, transition: { duration: 0.44, ease: 'easeOut' } },
};

const listMotion: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.06, delayChildren: 0.08 } },
};

const itemMotion: Variants = {
  hidden: { opacity: 0, y: 14 },
  show: { opacity: 1, y: 0, transition: { duration: 0.32, ease: 'easeOut' } },
};

export default function DownloadPage() {
  const [releases, setReleases] = useState<AppRelease[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;

    getAppReleases()
      .then(data => {
        if (!cancelled) {
          setReleases(data);
          setError('');
        }
      })
      .catch(err => {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const latest = useMemo(() => releases.find(release => release.isLatest) ?? releases[0] ?? null, [releases]);
  const archives = useMemo(() => releases.filter(release => release.id !== latest?.id), [latest?.id, releases]);
  const releaseCount = releases.length;

  return (
    <div className={styles.page}>
      <header className={styles.nav}>
        <Link to="/" className={styles.brand} aria-label="M2HAI home">
          <span className={styles.brandMark}><Sparkles size={16} /></span>
          <span>M2HAI</span>
        </Link>
        <nav className={styles.navLinks} aria-label="Download page navigation">
          <Link to="/app">Open app</Link>
          <Link to="/login">Sign in</Link>
        </nav>
      </header>

      <main>
        <motion.section
          className={styles.hero}
          variants={pageMotion}
          initial="hidden"
          animate="show"
        >
          <div className={styles.heroBackdrop} />
          <div className={styles.heroContent}>
            <Link to="/" className={styles.backLink}>
              <ArrowLeft size={16} /> Back to home
            </Link>
            <div className={styles.heroBadge}>
              <Smartphone size={15} /> Android APK downloads
            </div>
            <h1>Download M2HAI for Android.</h1>
            <p>
              Install the latest M2HAI Android release or keep an older build when your device needs it.
              Releases come from the same update system used inside the app.
            </p>
          </div>
        </motion.section>

        <section className={styles.downloadShell}>
          {loading ? (
            <DownloadSkeleton />
          ) : error ? (
            <motion.div className={styles.errorCard} variants={pageMotion} initial="hidden" animate="show">
              <ShieldCheck size={24} />
              <h2>Release list is unavailable.</h2>
              <p>{error}</p>
              <button type="button" onClick={() => window.location.reload()}>Try again</button>
            </motion.div>
          ) : latest ? (
            <>
              <motion.div className={styles.latestGrid} variants={listMotion} initial="hidden" animate="show">
                <motion.article className={styles.latestCard} variants={itemMotion}>
                  <div className={styles.latestHeader}>
                    <span className={styles.releaseKicker}><BadgeCheck size={15} /> Latest release</span>
                    <span className={styles.channel}>{latest.channel}</span>
                  </div>
                  <div className={styles.versionBlock}>
                    <h2>{latest.versionName}</h2>
                    <span>Version code {latest.versionCode || 'latest'}</span>
                  </div>
                  <p className={styles.releaseMessage}>
                    {latest.releaseNotes || 'The current Android build for M2HAI with chat, image tools, app updates, and account sync.'}
                  </p>
                  <div className={styles.releaseMetaGrid}>
                    <MetaItem icon={<Package size={16} />} label="Size" value={latest.sizeMb ? `${latest.sizeMb} MB` : 'APK file'} />
                    <MetaItem icon={<Clock3 size={16} />} label="Published" value={formatDate(latest.publishedAt)} />
                    <MetaItem icon={<ShieldCheck size={16} />} label="Update type" value={latest.required ? 'Required' : 'Optional'} />
                    <MetaItem icon={<Fingerprint size={16} />} label="SHA-256" value={latest.sha256 ? 'Available' : 'Not provided'} />
                  </div>
                  <div className={styles.latestActions}>
                    <a className={styles.primaryDownload} href={latest.apkUrl} download>
                      <Download size={18} /> Download latest APK
                    </a>
                    <Link className={styles.secondaryAction} to="/app">
                      Try web app <ArrowRight size={16} />
                    </Link>
                  </div>
                </motion.article>

                <motion.aside className={styles.installCard} variants={itemMotion}>
                  <div className={styles.installIcon}><Smartphone size={22} /></div>
                  <h2>Install on Android.</h2>
                  <div className={styles.steps}>
                    <span><CheckCircle2 size={16} /> Download the APK on your Android device.</span>
                    <span><CheckCircle2 size={16} /> Open the downloaded file from the notification or downloads folder.</span>
                    <span><CheckCircle2 size={16} /> Allow install permission if Android asks for it.</span>
                    <span><CheckCircle2 size={16} /> Open M2HAI and sign in with the same account.</span>
                  </div>
                </motion.aside>
              </motion.div>

              <motion.section className={styles.archiveSection} variants={pageMotion} initial="hidden" animate="show">
                <div className={styles.sectionHeader}>
                  <div>
                    <span className={styles.eyebrow}>Release archive</span>
                    <h2>Older Android versions.</h2>
                  </div>
                  <span className={styles.countPill}>{releaseCount} release{releaseCount === 1 ? '' : 's'}</span>
                </div>

                {archives.length > 0 ? (
                  <motion.div className={styles.releaseList} variants={listMotion} initial="hidden" animate="show">
                    {archives.map(release => (
                      <ReleaseRow release={release} key={release.id} />
                    ))}
                  </motion.div>
                ) : (
                  <div className={styles.emptyCard}>
                    <FileText size={22} />
                    <h3>No older APKs yet.</h3>
                    <p>When older builds are available in the release storage, they will appear here automatically.</p>
                  </div>
                )}
              </motion.section>
            </>
          ) : (
            <motion.div className={styles.emptyCardLarge} variants={pageMotion} initial="hidden" animate="show">
              <Package size={28} />
              <h2>No Android release is published yet.</h2>
              <p>Once an APK release is uploaded, users will be able to download it from this page.</p>
            </motion.div>
          )}
        </section>
      </main>
    </div>
  );
}

function ReleaseRow({ release }: { release: AppRelease }) {
  return (
    <motion.article className={styles.releaseRow} variants={itemMotion}>
      <div className={styles.releaseIcon}><Package size={18} /></div>
      <div className={styles.releaseInfo}>
        <div className={styles.releaseTitle}>
          <h3>{release.versionName}</h3>
          <span>{release.channel}</span>
        </div>
        <p>
          Version code {release.versionCode || 'unknown'} · {release.sizeMb ? `${release.sizeMb} MB` : release.fileName} · {formatDate(release.publishedAt)}
        </p>
      </div>
      <a className={styles.rowDownload} href={release.apkUrl} download aria-label={`Download M2HAI ${release.versionName}`}>
        <Download size={16} /> Download
      </a>
    </motion.article>
  );
}

function MetaItem({ icon, label, value }: { icon: ReactNode; label: string; value: string }) {
  return (
    <div className={styles.metaItem}>
      <span>{icon}</span>
      <div>
        <small>{label}</small>
        <strong>{value}</strong>
      </div>
    </div>
  );
}

function DownloadSkeleton() {
  return (
    <div className={styles.skeletonGrid} aria-label="Loading releases">
      <div className={styles.skeletonCard}>
        <span />
        <strong />
        <p />
        <p />
        <div />
      </div>
      <div className={styles.skeletonSide}>
        <span />
        <p />
        <p />
        <p />
      </div>
    </div>
  );
}

function formatDate(value: string): string {
  if (!value) return 'Not published';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Not published';
  return date.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}
