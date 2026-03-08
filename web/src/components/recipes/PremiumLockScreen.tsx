import Link from 'next/link';

export function PremiumLockScreen() {
  return (
    <section className="lock-screen" aria-live="polite">
      <div className="lock-icon" aria-hidden>
        🔒
      </div>
      <h2>This recipe is part of the premium collection.</h2>
      <p>Upgrade your account to unlock exclusive healthy recipes and wellness meal plans.</p>
      <Link href="/" className="back-home-link">
        Back to recipes
      </Link>
    </section>
  );
}
