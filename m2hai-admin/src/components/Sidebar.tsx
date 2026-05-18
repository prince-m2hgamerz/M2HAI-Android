import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { LayoutDashboard, Users, MessageSquare, Bot, LogOut, Shield, Settings, ServerCog, ThumbsUp, Send, Menu, X } from 'lucide-react';
import styles from './Sidebar.module.css';

interface SidebarProps { onLogout: () => void; }

const navItems = [
  { to: '/admin', icon: <LayoutDashboard size={18} />, label: 'Dashboard' },
  { to: '/admin/users', icon: <Users size={18} />, label: 'Users' },
  { to: '/admin/chats', icon: <MessageSquare size={18} />, label: 'Chats' },
  { to: '/admin/models', icon: <Bot size={18} />, label: 'AI Models' },
  { to: '/admin/telegram', icon: <Send size={18} />, label: 'Telegram' },
  { to: '/admin/feedback', icon: <ThumbsUp size={18} />, label: 'Feedback' },
  { to: '/admin/settings', icon: <Settings size={18} />, label: 'Settings' },
  { to: '/admin/system', icon: <ServerCog size={18} />, label: 'System' },
];

export default function Sidebar({ onLogout }: SidebarProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const activeItem = navItems.find(item => item.to === '/admin'
    ? location.pathname === '/admin'
    : location.pathname.startsWith(item.to));

  return (
    <>
    <header className={styles.mobileHeader}>
      <button className={styles.mobileMenuButton} onClick={() => setMobileOpen(true)} aria-label="Open navigation">
        <Menu size={18} />
      </button>
      <div className={styles.mobileHeaderBrand}>
        <Shield size={16} />
        <span>M2HAI</span>
      </div>
      <span className={styles.mobileHeaderTitle}>{activeItem?.label ?? 'Dashboard'}</span>
    </header>

    {mobileOpen && <button className={styles.mobileBackdrop} onClick={() => setMobileOpen(false)} aria-label="Close navigation" />}

    <aside className={`${styles.sidebar} ${mobileOpen ? styles.sidebarOpen : ''}`}>
      {/* Logo */}
      <div className={styles.logo}>
        <div className={styles.logoIcon}><Shield size={20} /></div>
        <div>
          <span className={styles.logoTitle}>M2HAI</span>
          <span className={styles.logoSub}>Admin Panel</span>
        </div>
        <button className={styles.mobileClose} onClick={() => setMobileOpen(false)} aria-label="Close navigation">
          <X size={16} />
        </button>
      </div>

      {/* Nav */}
      <nav className={styles.nav}>
        {navItems.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/admin'}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) => `${styles.link} ${isActive ? styles.linkActive : ''}`}
          >
            <span className={styles.linkIcon}>{item.icon}</span>
            <span className={styles.linkLabel}>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className={styles.footer}>
        <div className={styles.adminInfo}>
          <div className={styles.adminAvatar}>
            <Shield size={14} />
          </div>
          <div className={styles.adminDetails}>
            <span className={styles.adminName}>Prince</span>
            <span className={styles.adminRole}>Super Admin</span>
          </div>
        </div>
        <button className={styles.logoutBtn} onClick={onLogout} title="Logout">
          <LogOut size={16} />
        </button>
      </div>
    </aside>
    </>
  );
}
