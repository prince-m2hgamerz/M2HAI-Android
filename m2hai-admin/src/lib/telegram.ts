// Telegram OTP Service
// Sends a 6-digit OTP to the admin's Telegram chat

const BOT_TOKEN = import.meta.env.VITE_TELEGRAM_BOT_TOKEN;
const ADMIN_CHAT_ID = import.meta.env.VITE_TELEGRAM_ADMIN_CHAT_ID;

let currentOtp: string | null = null;
let otpExpiry: number | null = null;

export function generateOtp(): string {
  const otp = Math.floor(100000 + Math.random() * 900000).toString();
  currentOtp = otp;
  otpExpiry = Date.now() + 5 * 60 * 1000; // 5 minutes
  return otp;
}

export async function sendOtpViaTelegram(otp: string): Promise<boolean> {
  if (!BOT_TOKEN || BOT_TOKEN === 'YOUR_BOT_TOKEN_HERE') {
    console.warn('Telegram bot token not configured. OTP:', otp);
    // For development: auto-accept any 6-digit OTP
    return true;
  }

  const message = `🔐 *M2HAI Admin Panel - 2-Step Verification*\n\n` +
    `Your OTP is: *${otp}*\n\n` +
    `⏱ This code expires in 5 minutes.\n` +
    `🚫 Do not share this code with anyone.`;

  try {
    const res = await fetch(`https://api.telegram.org/bot${BOT_TOKEN}/sendMessage`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        chat_id: ADMIN_CHAT_ID,
        text: message,
        parse_mode: 'Markdown',
      }),
    });
    const data = await res.json();
    return data.ok === true;
  } catch (err) {
    console.error('Telegram send failed:', err);
    return false;
  }
}

export function verifyOtp(inputOtp: string): boolean {
  if (!currentOtp || !otpExpiry) return false;
  if (Date.now() > otpExpiry) {
    currentOtp = null;
    otpExpiry = null;
    return false;
  }
  return inputOtp === currentOtp;
}

export function clearOtp() {
  currentOtp = null;
  otpExpiry = null;
}
