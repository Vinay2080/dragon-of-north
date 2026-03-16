import React, {useEffect, useMemo, useRef, useState} from 'react';

const Toast = ({title, message, variant = 'info', duration = 4000, onClose}) => {
  const [isPaused, setIsPaused] = useState(false);
  const [remaining, setRemaining] = useState(duration);
  const timerRef = useRef(null);
  const startRef = useRef(0);

  const progressPct = useMemo(() => {
    if (!duration || duration <= 0) return 0;
    return Math.max(0, Math.min(100, (remaining / duration) * 100));
  }, [duration, remaining]);

  useEffect(() => {
    if (duration <= 0 || isPaused) return undefined;
    startRef.current = Date.now();
    timerRef.current = window.setTimeout(() => onClose(), remaining);
    return () => {
      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
        const elapsed = Date.now() - startRef.current;
        setRemaining((prev) => Math.max(0, prev - elapsed));
      }
    };
  }, [duration, isPaused, onClose, remaining]);

  return (
    <div role="alert" aria-live="assertive" onMouseEnter={() => setIsPaused(true)} onMouseLeave={() => setIsPaused(false)} className={`toast ${variant}`}>
      <div className="toast-title">{title}</div>
      {message && <div className="toast-body">{message}</div>}
      <div className="toast-meta">{variant.toUpperCase()}</div>
      {duration > 0 && <div style={{height: '4px', marginTop: '8px', background: 'var(--don-border-subtle)'}}><div style={{height: '4px', width: `${progressPct}%`, background: 'var(--don-accent)'}} /></div>}
    </div>
  );
};

export default Toast;
