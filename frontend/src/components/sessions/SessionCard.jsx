import React, {useState} from 'react';
import * as Icons from '../../shims/lucide-react';

const {Loader2, Trash2, ChevronDown} = Icons;

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
    const [isExpanded, setIsExpanded] = useState(false);
    const isRevoked = Boolean(session.revoked);
    const revokeDisabled = isRevoked || isCurrentDevice || isRevoking;

    return (
        <article
            className={`session-card ${isCurrentDevice ? 'session-card--current' : ''} ${isRevoked ? 'session-card--revoked' : ''}`}>
            
            <div className="session-card__container">
                {/* LEFT: Device Info */}
                <div className="session-card__left">
                    <div className="session-card__device">
                        <h3 className="session-card__title">{deviceLabel}</h3>
                        <p className="session-card__subtitle" title={`Device ID: ${session.device_id}`}>{secondaryLabel}</p>
                    </div>
                    {isCurrentDevice && <span className="session-chip session-chip--current">This device</span>}
                </div>

                {/* MIDDLE: Metadata (hidden on mobile) */}
                <div className="session-card__middle">
                    <div className="session-card__meta-item">
                        <p className="session-card__meta-label">Last active</p>
                        <p className="session-card__meta-value">{lastUsed}</p>
                    </div>
                    <div className="session-card__meta-item">
                        <p className="session-card__meta-label">Expires</p>
                        <p className="session-card__meta-value">{expiresAt}</p>
                    </div>
                </div>

                {/* RIGHT: Status & Actions */}
                <div className="session-card__right">
                    <span className={`session-chip ${isRevoked ? 'session-chip--revoked' : 'session-chip--active'}`}>
                        {isRevoked ? 'Revoked' : 'Active'}
                    </span>

                    <button
                        type="button"
                        className="session-card__revoke"
                        onClick={() => onRevoke(session.session_id)}
                        disabled={revokeDisabled}
                        aria-label={isCurrentDevice ? 'Cannot revoke current device' : 'Revoke session'}
                        title={isCurrentDevice ? 'Cannot revoke current device' : 'Revoke session'}
                    >
                        {isRevoking ? <Loader2 size={16} className="db-spin"/> : <Trash2 size={16}/>}
                    </button>
                </div>
            </div>

            {/* EXPANDED DETAILS (Mobile) */}
            {isExpanded && (
                <div className="session-card__expanded">
                    <div className="session-card__expanded-item">
                        <p className="session-card__expanded-label">Expires</p>
                        <p className="session-card__expanded-value">{expiresAt}</p>
                    </div>
                    <div className="session-card__expanded-item">
                        <p className="session-card__expanded-label">IP Address</p>
                        <p className="session-card__expanded-value">{session.ip_address || 'Not available'}</p>
                    </div>
                    <div className="session-card__expanded-item">
                        <p className="session-card__expanded-label">Device ID</p>
                        <p className="session-card__expanded-value session-card__expanded-value--mono">{session.device_id}</p>
                    </div>
                </div>
            )}

            {/* EXPAND TOGGLE (Mobile only) */}
            <button
                type="button"
                className="session-card__expand-toggle"
                onClick={() => setIsExpanded(!isExpanded)}
                aria-expanded={isExpanded}
                aria-label="Show more details"
            >
                <ChevronDown size={16} className={`${isExpanded ? 'session-card__expand-icon--open' : ''}`}/>
            </button>
        </article>
    );
};

export default SessionCard;

