import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Lock, Mail, Sparkles, UserRound } from 'lucide-react';
import { registerUser, signInUser } from '../lib/userApi';
import styles from './UserAuthPage.module.css';

interface UserAuthPageProps {
  mode: 'login' | 'register';
}

export default function UserAuthPage({ mode }: UserAuthPageProps) {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isRegister = mode === 'register';

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isRegister) {
        await registerUser(email.trim(), password, fullName.trim());
      } else {
        await signInUser(email.trim(), password);
      }
      navigate('/app', { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className={styles.page}>
      <Link to="/" className={styles.backLink}><ArrowLeft size={15} /> Home</Link>
      <section className={styles.panel}>
        <div className={styles.brand}>
          <span><Sparkles size={18} /></span>
          <strong>M2HAI</strong>
        </div>
        <h1>{isRegister ? 'Create your M2HAI account.' : 'Sign in to M2HAI.'}</h1>
        <p>
          {isRegister
            ? 'Use the same account in the website and Android app.'
            : 'Continue your chats, image prompts, and model history from the same Supabase account.'}
        </p>

        <form onSubmit={submit} className={styles.form}>
          {isRegister && (
            <label className={styles.field}>
              <span>Full name</span>
              <div className={styles.inputWrap}>
                <UserRound size={16} />
                <input value={fullName} onChange={event => setFullName(event.target.value)} placeholder="Your name" />
              </div>
            </label>
          )}

          <label className={styles.field}>
            <span>Email</span>
            <div className={styles.inputWrap}>
              <Mail size={16} />
              <input type="email" value={email} onChange={event => setEmail(event.target.value)} placeholder="you@example.com" required />
            </div>
          </label>

          <label className={styles.field}>
            <span>Password</span>
            <div className={styles.inputWrap}>
              <Lock size={16} />
              <input type="password" value={password} onChange={event => setPassword(event.target.value)} minLength={6} placeholder="Minimum 6 characters" required />
            </div>
          </label>

          {error && <div className={styles.error}>{error}</div>}

          <button className={styles.submit} disabled={loading || !email || !password}>
            {loading ? 'Please wait...' : isRegister ? 'Create account' : 'Sign in'}
          </button>
        </form>

        <div className={styles.switcher}>
          {isRegister ? 'Already have an account?' : 'New to M2HAI?'}
          <Link to={isRegister ? '/login' : '/register'}>
            {isRegister ? 'Sign in' : 'Create account'}
          </Link>
        </div>
      </section>
    </main>
  );
}
