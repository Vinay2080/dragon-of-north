import React, {useEffect, useState} from 'react';
import {apiService} from '../services/apiService';

const RateLimitInfo = () => {
  const [rateLimitInfo, setRateLimitInfo] = useState({remaining: null, capacity: null, retryAfter: null});
  const [countdown, setCountdown] = useState(null);

  useEffect(() => {
    const unsubscribe = apiService.onRateLimitUpdate((info) => {
      setRateLimitInfo(info);
      if (info.retryAfter) setCountdown(info.retryAfter);
    });
    const timeoutId = setTimeout(() => setRateLimitInfo(apiService.getRateLimitInfo()), 0);
    return () => { unsubscribe(); clearTimeout(timeoutId); };
  }, []);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setInterval(() => setCountdown((prev) => prev <= 1 ? null : prev - 1), 1000);
      return () => clearInterval(timer);
    }
  }, [countdown]);

  if (rateLimitInfo.remaining === null || rateLimitInfo.capacity === null) return null;
  const percentage = (rateLimitInfo.remaining / rateLimitInfo.capacity) * 100;
  const isLow = percentage < 30;
  const isBlocked = rateLimitInfo.retryAfter && countdown;

  return (
    <div style={{marginTop: '16px'}}>
      {isBlocked ? (
        <div className="card card-session revoked">
          <span className="badge badge-warning">Rate limit exceeded</span>
          <p style={{marginTop: '8px'}}>Please wait <span className="timestamp">{countdown}s</span> before retrying.</p>
        </div>
      ) : (
        <div className="card">
          <div style={{display: 'flex', justifyContent: 'space-between'}}>
            <span className="form-hint">Requests remaining</span>
            <span className={`badge ${isLow ? 'badge-warning' : 'badge-info'}`}>{rateLimitInfo.remaining} / {rateLimitInfo.capacity}</span>
          </div>
          <progress value={percentage} max="100" style={{marginTop: '8px', width: '100%', accentColor: 'var(--don-accent)'}} />
        </div>
      )}
    </div>
  );
};

export default RateLimitInfo;
