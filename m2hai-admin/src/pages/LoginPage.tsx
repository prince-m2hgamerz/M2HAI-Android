import React, { useState } from 'react';
import { generateOtp, sendOtpViaTelegram, verifyOtp, clearOtp } from '../lib/telegram';
import { Shield, Mail, Lock, Send, KeyRound, AlertCircle, Loader2, CheckCircle } from 'lucide-react';
import styles from './LoginPage.module.css';

const ADMIN_EMAIL = import.meta.env.VITE_ADMIN_EMAIL || 'm2hgamerz.prince@gmail.com';
const ADMIN_PASSWORD = 'admin123';

interface LoginPageProps {
  onLogin: () => void;
}

type Step = 'credentials' | 'otp';

export default function LoginPage({ onLogin }: LoginPageProps) {
  const [step, setStep] = useState<Step>('credentials');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCredentials = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (email.trim().toLowerCase() !== ADMIN_EMAIL.toLowerCase()) {
      setError('Invalid admin credentials.');
      return;
    }
    if (password !== ADMIN_PASSWORD) {
      setError('Invalid admin credentials.');
      return;
    }

      setLoading(true);
      try {
        const code = generateOtp();
        const sent = await sendOtpViaTelegram(code);
        if (sent) {
          setStep('otp');
        } else {
          setError('Failed to send OTP. Check your Telegram bot configuration.');
        }
      } catch {
        setError('An error occurred while sending OTP.');
      } finally {
        setLoading(false);
      }
  };

  const handleOtp = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Dev fallback: if bot not configured, any 6-digit code works
    const botConfigured = import.meta.env.VITE_TELEGRAM_BOT_TOKEN && 
      import.meta.env.VITE_TELEGRAM_BOT_TOKEN !== 'YOUR_BOT_TOKEN_HERE';

    if (!botConfigured || verifyOtp(otp)) {
      clearOtp();
      sessionStorage.setItem('m2hai_admin_auth', 'true');
      onLogin();
    } else {
      setError('Invalid or expired OTP. Please try again.');
    }
  };

  return (
    <div className={styles.root}>
      {/* Background grid */}
      <div className={styles.grid} />
      {/* Glow orbs */}
      <div className={styles.orb1} />
      <div className={styles.orb2} />

      <div className={styles.card}>
        {/* Logo */}
        <div className={styles.logo}>
          <Shield size={32} className={styles.logoIcon} />
          <span className={styles.logoText}>M2HAI</span>
          <span className={styles.logoBadge}>Admin</span>
        </div>

        {/* Step indicator */}
        <div className={styles.steps}>
          <div className={`${styles.step} ${step === 'credentials' || step === 'otp' ? styles.stepActive : ''}`}>
            <Lock size={14} />
            <span>Credentials</span>
          </div>
          <div className={styles.stepLine} />
          <div className={`${styles.step} ${step === 'otp' ? styles.stepActive : ''}`}>
            <KeyRound size={14} />
            <span>Verify OTP</span>
          </div>
        </div>

        {step === 'credentials' && (
          <form onSubmit={handleCredentials} className={styles.form}>
            <h2 className={styles.title}>Welcome back</h2>
            <p className={styles.subtitle}>Sign in to access the admin panel</p>

            <div className={styles.field}>
              <label className={styles.label}>Admin Email</label>
              <div className={styles.inputWrap}>
                <Mail size={16} className={styles.inputIcon} />
                <input
                  type="email"
                  className={styles.input}
                  placeholder="admin@example.com"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                  autoFocus
                />
              </div>
            </div>

            <div className={styles.field}>
              <label className={styles.label}>Password</label>
              <div className={styles.inputWrap}>
                <Lock size={16} className={styles.inputIcon} />
                <input
                  type="password"
                  className={styles.input}
                  placeholder="Enter password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                />
              </div>
            </div>

            {error && (
              <div className={styles.error}>
                <AlertCircle size={14} />
                <span>{error}</span>
              </div>
            )}

            <button type="submit" className={styles.btn} disabled={loading}>
              {loading ? <Loader2 size={16} className="animate-spin" /> : <Send size={16} />}
              {loading ? 'Sending OTP...' : 'Continue → Get OTP'}
            </button>
          </form>
        )}

        {step === 'otp' && (
          <form onSubmit={handleOtp} className={styles.form}>
            <div className={styles.otpSuccess}>
              <CheckCircle size={20} />
              <span>OTP sent to your Telegram!</span>
            </div>
            <h2 className={styles.title}>Two-Factor Verification</h2>
            <p className={styles.subtitle}>
              Enter the 6-digit code sent to your Telegram bot
            </p>

            <div className={styles.field}>
              <label className={styles.label}>OTP Code</label>
              <div className={styles.inputWrap}>
                <KeyRound size={16} className={styles.inputIcon} />
                <input
                  type="text"
                  className={`${styles.input} ${styles.inputOtp}`}
                  placeholder="000000"
                  value={otp}
                  onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  maxLength={6}
                  required
                  autoFocus
                />
              </div>
              <p className={styles.hint}>Code expires in 5 minutes</p>
            </div>

            {error && (
              <div className={styles.error}>
                <AlertCircle size={14} />
                <span>{error}</span>
              </div>
            )}

            <button type="submit" className={styles.btn} disabled={otp.length < 6}>
              <Shield size={16} />
              Verify & Enter Panel
            </button>

            <button
              type="button"
              className={styles.btnGhost}
              onClick={() => { setStep('credentials'); setOtp(''); setError(''); }}
            >
              ← Back to credentials
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
