import React from 'react';
import * as Icons from '../../shims/lucide-react';

const {Loader2, Trash2} = Icons;

const SessionCard = ({
                         session,
                         deviceLabel,
                         secondaryLabel,
                         lastUsed,
                         expiresAt,
                         isCurrentDevice,
                         isRevoking,
                         onRevoke,
                     }) => {
    const isRevoked = Boolean(session.revoked);
    const revokeDisabled = isRevoked || isCurrentDevice || isRevoking;

    return (
        <article
            className={`session-card ${isCurrentDevice ? 'session-card--current' : ''} ${isRevoked ? 'session-card--revoked' : ''}`}>
            <header className="session-card__header">
                <div>
                    <h3 className="session-card__title">{deviceLabel}</h3>
                    <p className="session-card__subtitle" title={`Device ID: ${session.device_id}`}>{secondaryLabel}</p>
                </div>
                {isCurrentDevice ? <span className="session-chip session-chip--current">This device</span> : null}
            </header>

            <div className="session-card__meta">
                <div>
                    <p className="session-card__meta-label">Last active</p>
                    <p className="session-card__meta-value">{lastUsed}</p>
                </div>
                <div>
                    <p className="session-card__meta-label">Expires</p>
                    <p className="session-card__meta-value">{expiresAt}</p>
                </div>
            </div>

            <footer className="session-card__footer">
                <span className={`session-chip ${isRevoked ? 'session-chip--revoked' : 'session-chip--active'}`}>
                    {isRevoked ? 'Revoked' : 'Active'}
                </span>

                <button
                    type="button"
                    className="session-card__revoke"
                    onClick={() => onRevoke(session.session_id)}
                    disabled={revokeDisabled}
                    aria-label={isCurrentDevice ? 'Cannot revoke current device' : 'Revoke session'}
                >
                    {isRevoking ? <Loader2 size={14} className="db-spin"/> : <Trash2 size={14}/>}
                </button>
            </footer>
        </article>
    );
};

export default SessionCard;

