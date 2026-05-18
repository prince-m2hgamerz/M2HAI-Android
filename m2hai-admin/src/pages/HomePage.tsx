import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { motion, type Variants } from 'framer-motion';
import {
  Activity,
  ArrowRight,
  Bot,
  CheckCircle2,
  ChevronDown,
  CircleUserRound,
  Download,
  Image,
  Menu,
  MessageSquare,
  Send,
  Shield,
  Smartphone,
  Sparkles,
  Users,
  X,
  Zap,
} from 'lucide-react';
import { getAppSettings, getModels, getPlatformOverview, subscribeToPlatformOverview, type AppSettings, type PlatformOverview } from '../lib/api';
import styles from './HomePage.module.css';

type NavItem = {
  label: string;
  href: string;
  text: string;
  icon: ReactNode;
};

type NavGroup = {
  label: string;
  items: NavItem[];
};

const navGroups: NavGroup[] = [
  {
    label: 'Product',
    items: [
      { label: 'AI Workspace', href: '#workspace', text: 'ChatGPT-style web chat for users', icon: <MessageSquare size={16} /> },
      { label: 'Android App', href: '#android-app', text: 'Mobile AI chat, images, updates', icon: <Smartphone size={16} /> },
      { label: 'Download APK', href: '/download', text: 'Latest and older Android releases', icon: <Download size={16} /> },
      { label: 'Image AI', href: '#workspace', text: 'Generate images from user prompts', icon: <Image size={16} /> },
    ],
  },
  {
    label: 'Purpose',
    items: [
      { label: 'Everyday AI', href: '#purpose', text: 'Writing, learning, planning, and images', icon: <Sparkles size={16} /> },
      { label: 'Live Product', href: '#live-data', text: 'Real usage signals from the platform', icon: <Activity size={16} /> },
      { label: 'Telegram AI', href: '#products', text: 'AI replies inside Telegram bots', icon: <Send size={16} /> },
    ],
  },
  {
    label: 'Company',
    items: [
      { label: 'Launch App', href: '/app', text: 'Start the web AI workspace', icon: <Sparkles size={16} /> },
      { label: 'Products', href: '#products', text: 'Web app, Android app, images, bots', icon: <Bot size={16} /> },
      { label: 'Sign In', href: '/login', text: 'Same account on web and Android', icon: <CircleUserRound size={16} /> },
    ],
  },
];

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.42, ease: 'easeOut' } },
};

const stagger: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.07, delayChildren: 0.08 } },
};

export default function HomePage() {
  const [stats, setStats] = useState<PlatformOverview | null>(null);
  const [settings, setSettings] = useState<AppSettings | null>(null);
  const [modelCount, setModelCount] = useState(0);
  const [openGroup, setOpenGroup] = useState<string | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [mobileGroups, setMobileGroups] = useState<Record<string, boolean>>({
    Product: true,
    Purpose: true,
  });

  useEffect(() => {
    const load = () => Promise.allSettled([getPlatformOverview(), getAppSettings(), getModels()]).then(([statsResult, settingsResult, modelsResult]) => {
      if (statsResult.status === 'fulfilled') setStats(statsResult.value);
      if (settingsResult.status === 'fulfilled') setSettings(settingsResult.value);
      if (modelsResult.status === 'fulfilled') setModelCount(modelsResult.value.filter(model => model.is_active).length);
    });
    load();
    return subscribeToPlatformOverview(load);
  }, []);

  useEffect(() => {
    if (!mobileOpen) return undefined;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMobileOpen(false);
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [mobileOpen]);

  useEffect(() => {
    if (!openGroup) return undefined;
    const onPointerDown = (event: PointerEvent) => {
      const target = event.target as HTMLElement | null;
      if (!target?.closest(`.${styles.navDropdown}`)) setOpenGroup(null);
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpenGroup(null);
    };
    window.addEventListener('pointerdown', onPointerDown);
    window.addEventListener('keydown', onKeyDown);
    return () => {
      window.removeEventListener('pointerdown', onPointerDown);
      window.removeEventListener('keydown', onKeyDown);
    };
  }, [openGroup]);

  const liveStats = useMemo(() => ([
    { label: 'Users', value: stats?.totalUsers ?? 0, icon: <Users size={18} /> },
    { label: 'Active chats', value: stats?.activeChats ?? 0, icon: <MessageSquare size={18} /> },
    { label: 'AI requests', value: stats?.aiRequests ?? 0, icon: <Activity size={18} /> },
    { label: 'Active models', value: modelCount || stats?.activeModels || 0, icon: <Bot size={18} /> },
  ]), [modelCount, stats]);

  const toggleMobileGroup = (group: string) => {
    setMobileGroups(current => ({ ...current, [group]: !current[group] }));
  };

  return (
    <div className={styles.page}>
      <header className={styles.nav}>
        <Link to="/" className={styles.brand} aria-label="M2HAI home">
          <span className={styles.brandMark}><Sparkles size={16} /></span>
          <span>M2HAI</span>
        </Link>

        <nav className={styles.desktopNav} aria-label="Main navigation">
          {navGroups.map(group => (
            <div
              className={styles.navDropdown}
              key={group.label}
              onMouseEnter={() => setOpenGroup(group.label)}
            >
              <button
                className={styles.navDropdownTrigger}
                type="button"
                aria-expanded={openGroup === group.label}
                onClick={() => setOpenGroup(current => current === group.label ? null : group.label)}
              >
                {group.label}
                <ChevronDown size={14} />
              </button>
              {openGroup === group.label && (
                <div className={styles.navDropdownMenu}>
                  {group.items.map(item => (
                    <NavLinkItem item={item} key={item.label} onSelect={() => setOpenGroup(null)} />
                  ))}
                </div>
              )}
            </div>
          ))}
        </nav>

        <div className={styles.navCtas}>
          <Link to="/login" className={styles.navSecondary}><CircleUserRound size={15} /> Sign in</Link>
          <Link to="/app" className={styles.navPrimary}>Open app</Link>
          <button
            className={styles.mobileMenuButton}
            type="button"
            aria-label="Open menu"
            onClick={() => setMobileOpen(true)}
          >
            <Menu size={18} />
          </button>
        </div>
      </header>

      <div className={`${styles.mobileOverlay} ${mobileOpen ? styles.mobileOverlayOpen : ''}`} onClick={() => setMobileOpen(false)} />
      <aside className={`${styles.mobileSidebar} ${mobileOpen ? styles.mobileSidebarOpen : ''}`} aria-hidden={!mobileOpen}>
        <div className={styles.mobileSidebarHeader}>
          <Link to="/" className={styles.brand} onClick={() => setMobileOpen(false)}>
            <span className={styles.brandMark}><Sparkles size={16} /></span>
            <span>M2HAI</span>
          </Link>
          <button className={styles.mobileClose} type="button" aria-label="Close menu" onClick={() => setMobileOpen(false)}>
            <X size={18} />
          </button>
        </div>
        <div className={styles.mobileNavGroups}>
          {navGroups.map(group => (
            <div className={styles.mobileNavGroup} key={group.label}>
              <button
                type="button"
                className={styles.mobileNavGroupTrigger}
                aria-expanded={Boolean(mobileGroups[group.label])}
                onClick={() => toggleMobileGroup(group.label)}
              >
                {group.label}
                <ChevronDown size={15} />
              </button>
              {mobileGroups[group.label] && (
                <div className={styles.mobileNavItems}>
                  {group.items.map(item => (
                    <NavLinkItem item={item} key={item.label} onSelect={() => setMobileOpen(false)} />
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
        <div className={styles.mobileSidebarActions}>
          <Link to="/app" className={styles.primaryCta} onClick={() => setMobileOpen(false)}>Launch app <ArrowRight size={16} /></Link>
          <Link to="/register" className={styles.secondaryCta} onClick={() => setMobileOpen(false)}>Create account</Link>
        </div>
      </aside>

      <main>
        <section className={styles.hero}>
          <div className={styles.shaderLayer} />
          <div className={styles.heroCenter}>
            <motion.div className={styles.heroBadge} variants={fadeUp} initial="hidden" animate="show"><Sparkles size={15} /> AI chat for Android and web</motion.div>
            <motion.h1 variants={fadeUp} initial="hidden" animate="show">M2HAI</motion.h1>
            <motion.p variants={fadeUp} initial="hidden" animate="show">
              Chat with AI models, generate images, continue conversations on Android, and keep the app experience synced across devices.
            </motion.p>

            <motion.div className={styles.promptComposer} aria-label="M2HAI AI prompt preview" variants={fadeUp} initial="hidden" animate="show">
              <div className={styles.promptModes}>
                <span><MessageSquare size={14} /> Ask</span>
                <span><Image size={14} /> Image</span>
                <span><Bot size={14} /> Models</span>
              </div>
              <div className={styles.promptText}>Ask M2HAI anything...</div>
              <div className={styles.promptBottom}>
                <div className={styles.promptTools}>
                  <span><Smartphone size={14} /> Android synced</span>
                  <span><Zap size={14} /> Streaming</span>
                </div>
                <Link to="/app" className={styles.sendButton} aria-label="Open M2HAI app">
                  <ArrowRight size={18} />
                </Link>
              </div>
            </motion.div>

            <motion.div className={styles.quickPrompts} variants={stagger} initial="hidden" animate="show">
              <Link to="/app">Write with AI</Link>
              <Link to="/app">Generate image</Link>
              <Link to="/download">Download Android</Link>
              <Link to="/app">Open chat history</Link>
              <a href="#android-app">Android app info</a>
            </motion.div>
          </div>
        </section>

        <section className={styles.liveBand} id="live-data" aria-label="Live platform data">
          <motion.div className={styles.liveInner} variants={stagger} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.25 }}>
            {liveStats.map(item => (
              <motion.div className={styles.liveStat} key={item.label} variants={fadeUp}>
                <div className={styles.liveIcon}>{item.icon}</div>
                <strong>{item.value.toLocaleString()}</strong>
                <span>{item.label}</span>
              </motion.div>
            ))}
          </motion.div>
        </section>

        <section className={styles.assistantSection} id="workspace">
          <motion.div className={styles.workspacePreview} variants={fadeUp} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.22 }}>
            <div className={styles.workspaceTop}>
              <div>
                <span>M2HAI</span>
                <strong>{settings?.default_model_id || 'Default model'}</strong>
              </div>
              <b>Live</b>
            </div>
            <div className={styles.workspaceBody}>
              <aside className={styles.workspaceRail}>
                <span />
                <span />
                <span />
              </aside>
              <div className={styles.workspaceChat}>
                <div className={styles.userBubble}>Make an image prompt and product caption for my Android app.</div>
                <div className={styles.aiBubble}>
                  <Sparkles size={15} />
                  <span>I can route image requests to {settings?.image_model_id || 'the configured image model'} and keep the chat saved to your account.</span>
                </div>
                <div className={styles.responseActions}>
                  <span>Copy</span>
                  <span>Like</span>
                  <span>Share</span>
                </div>
                <div className={styles.miniComposer}>
                  <span>Message M2HAI</span>
                  <ArrowRight size={15} />
                </div>
              </div>
            </div>
          </motion.div>

          <motion.div className={styles.assistantCopy} variants={fadeUp} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.22 }}>
            <span className={styles.eyebrow}>AI app experience</span>
            <h2>A clean assistant interface for real users.</h2>
            <p>M2HAI should feel like a modern AI app: focused input, clear responses, useful model controls, saved chats, and image generation.</p>
            <div className={styles.capabilityGrid}>
              <ProductCard icon={<MessageSquare size={18} />} label="Chat" title="Streaming replies" text="Users get a fast AI conversation flow on website and Android." />
              <ProductCard icon={<Image size={18} />} label="Images" title="Image prompts" text="Image generation is routed by the configured model settings." />
            </div>
          </motion.div>
        </section>

        <section className={styles.productsSection} id="products">
          <div className={styles.sectionIntro}>
            <span className={styles.eyebrow}>Products</span>
            <h2>One AI experience across the places users already work.</h2>
            <p>M2HAI brings fast conversations, image generation, Android access, and Telegram replies into one connected product.</p>
          </div>
          <motion.div className={styles.controlGrid} variants={stagger} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.18 }}>
            <ProductCard icon={<MessageSquare size={18} />} label="Web" title="AI workspace" text="A focused chat surface for writing, learning, summarizing, and planning." />
            <ProductCard icon={<Smartphone size={18} />} label="Android" title="Mobile companion" text="The same account, chats, models, and generated images on Android." />
            <ProductCard icon={<Send size={18} />} label="Telegram" title="Bot replies" text="AI assistance inside Telegram for quick questions and everyday tasks." />
          </motion.div>
        </section>

        <section className={styles.androidBand} id="android-app">
          <motion.div className={styles.phoneShell} variants={fadeUp} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.24 }}>
            <div className={styles.phoneTop}>
              <span>M2HAI Android</span>
              <b>{settings?.latest_version_name || 'v1.0'}</b>
            </div>
            <div className={styles.phoneScreen}>
              <div className={styles.phoneModel}><Bot size={14} /> {settings?.default_model_id || 'Active model'}</div>
              <div className={styles.phoneBubbleUser}>Explain this in simple words.</div>
              <div className={styles.phoneBubbleAi}>Sure. I will answer clearly and save this chat to your account.</div>
              <div className={styles.phoneUpdate}><Download size={15} /> In-app release updates</div>
            </div>
          </motion.div>

          <motion.div className={styles.androidCopy} variants={fadeUp} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.24 }}>
            <span className={styles.eyebrow}>Android app information</span>
            <h2>Your Android users get the same AI platform.</h2>
            <div className={styles.checkList}>
              <span><CheckCircle2 size={16} /> Same login across website and Android</span>
              <span><CheckCircle2 size={16} /> Chat, images, models, and history stay connected</span>
              <span><CheckCircle2 size={16} /> Active AI and image models stay available across devices</span>
              <span><CheckCircle2 size={16} /> New APK releases appear inside the app update card</span>
            </div>
            <Link to="/download" className={styles.downloadCta}><Download size={16} /> Download Android APK</Link>
          </motion.div>
        </section>

        <section className={styles.controlSection} id="purpose">
          <div className={styles.sectionIntro}>
            <span className={styles.eyebrow}>Purpose</span>
            <h2>Built to make AI useful on every screen.</h2>
            <p>M2HAI is for practical daily work: ask questions, generate images, write better, plan faster, and continue from web to Android without losing context.</p>
          </div>
          <motion.div className={styles.controlGrid} variants={stagger} initial="hidden" whileInView="show" viewport={{ once: true, amount: 0.18 }}>
            <ProductCard icon={<Shield size={18} />} label="Trust" title="Account continuity" text="Users keep the same identity, history, and workspace across web and Android." />
            <ProductCard icon={<Zap size={18} />} label="Speed" title="Fast responses" text="Streaming chat keeps answers visible as the model writes." />
            <ProductCard icon={<Image size={18} />} label="Creative" title="Text and images" text="Users can move from conversation to generated visuals in the same product." />
          </motion.div>
        </section>

        <section className={styles.ctaBand}>
          <h2>Open M2HAI.</h2>
          <p>Use the website like a modern AI assistant and keep the Android app connected to the same backend.</p>
          <div className={styles.ctas}>
            <Link to="/app" className={styles.primaryCta}>Launch app <ArrowRight size={16} /></Link>
            <Link to="/download" className={styles.secondaryCta}>Download Android</Link>
            <Link to="/register" className={styles.secondaryCta}>Create account</Link>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <span>M2HAI</span>
        <span>AI chat, images, Android access, and Telegram replies.</span>
      </footer>
    </div>
  );
}

function NavLinkItem({ item, onSelect }: { item: NavItem; onSelect: () => void }) {
  const content = (
    <>
      <span className={styles.navItemIcon}>{item.icon}</span>
      <span>
        <strong>{item.label}</strong>
        <small>{item.text}</small>
      </span>
    </>
  );

  if (item.href.startsWith('/')) {
    return (
      <Link to={item.href} className={styles.navItem} onClick={onSelect}>
        {content}
      </Link>
    );
  }

  return (
    <a href={item.href} className={styles.navItem} onClick={onSelect}>
      {content}
    </a>
  );
}

function ProductCard({ icon, label, title, text }: { icon: ReactNode; label: string; title: string; text: string }) {
  return (
    <motion.article className={styles.productCard} variants={fadeUp}>
      <div className={styles.productIcon}>{icon}</div>
      <span>{label}</span>
      <h3>{title}</h3>
      <p>{text}</p>
    </motion.article>
  );
}
