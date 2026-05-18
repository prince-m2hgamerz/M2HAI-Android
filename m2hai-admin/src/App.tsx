import { useEffect, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Moon, Sun } from 'lucide-react';
import HomePage from './pages/HomePage';
import DownloadPage from './pages/DownloadPage';
import LoginPage from './pages/LoginPage';
import UserAuthPage from './pages/UserAuthPage';
import UserAppPage from './pages/UserAppPage';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import UsersPage from './pages/UsersPage';
import ChatsPage from './pages/ChatsPage';
import ModelsPage from './pages/ModelsPage';
import SettingsPage from './pages/SettingsPage';
import SystemPage from './pages/SystemPage';
import FeedbackPage from './pages/FeedbackPage';
import TelegramBotsPage from './pages/TelegramBotsPage';
import { getCurrentSession, onUserSessionChange } from './lib/userApi';

const ADMIN_HOME = '/admin';

function isAuthenticated() {
  return sessionStorage.getItem('m2hai_admin_auth') === 'true';
}

export default function App() {
  const [authed, setAuthed] = useState(isAuthenticated);
  const [userSession, setUserSession] = useState<Session | null>(null);
  const [userSessionReady, setUserSessionReady] = useState(false);
  const [theme, setTheme] = useState(() => localStorage.getItem('m2hai_theme') || 'light');
  const location = useLocation();
  const navigate = useNavigate();
  const showThemeToggle = !location.pathname.startsWith('/app') && !location.pathname.startsWith('/admin');

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem('m2hai_theme', theme);
  }, [theme]);

  useEffect(() => {
    getCurrentSession()
      .then(setUserSession)
      .finally(() => setUserSessionReady(true));
    const { data } = onUserSessionChange(session => {
      setUserSession(session);
      setUserSessionReady(true);
    });
    return () => data.subscription.unsubscribe();
  }, []);

  const handleLogin = () => {
    const state = location.state as { from?: string } | null;
    setAuthed(true);
    navigate(state?.from ?? ADMIN_HOME, { replace: true });
  };

  const handleLogout = () => {
    sessionStorage.removeItem('m2hai_admin_auth');
    setAuthed(false);
    navigate('/', { replace: true });
  };

  return (
    <>
      {showThemeToggle && (
        <button
          className="global-theme-toggle"
          onClick={() => setTheme(current => current === 'dark' ? 'light' : 'dark')}
          aria-label="Toggle color theme"
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          {theme === 'dark' ? <Sun size={16} /> : <Moon size={16} />}
        </button>
      )}
      <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/download" element={<DownloadPage />} />
      <Route
        path="/login"
        element={userSessionReady && userSession ? <Navigate to="/app" replace /> : <UserAuthPage mode="login" />}
      />
      <Route
        path="/register"
        element={userSessionReady && userSession ? <Navigate to="/app" replace /> : <UserAuthPage mode="register" />}
      />
      <Route
        path="/app/*"
        element={
          !userSessionReady
            ? <div style={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>Loading...</div>
            : userSession
              ? <UserAppPage session={userSession} />
              : <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
        }
      />
      <Route path="/admin/login" element={authed ? <Navigate to={ADMIN_HOME} replace /> : <LoginPage onLogin={handleLogin} />} />
      <Route
        path="/admin/*"
        element={
          authed
            ? <AdminShell onLogout={handleLogout} />
            : <Navigate to="/admin/login" replace state={{ from: `${location.pathname}${location.search}` }} />
        }
      />
      <Route path="/users" element={<Navigate to="/admin/users" replace />} />
      <Route path="/chats" element={<Navigate to="/admin/chats" replace />} />
      <Route path="/models" element={<Navigate to="/admin/models" replace />} />
      <Route path="/settings" element={<Navigate to="/admin/settings" replace />} />
      <Route path="/system" element={<Navigate to="/admin/system" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

function AdminShell({ onLogout }: { onLogout: () => void }) {
  return (
    <div className="admin-shell">
      <Sidebar onLogout={onLogout} />
      <main className="admin-main">
        <Routes>
          <Route index element={<Dashboard />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="chats" element={<ChatsPage />} />
          <Route path="models" element={<ModelsPage />} />
          <Route path="telegram" element={<TelegramBotsPage />} />
          <Route path="feedback" element={<FeedbackPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="system" element={<SystemPage />} />
          <Route path="*" element={<Navigate to={ADMIN_HOME} replace />} />
        </Routes>
      </main>
    </div>
  );
}
