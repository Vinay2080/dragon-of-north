import React from 'react';

const SummarySection = ({totalSessions, activeSessions, lastLogin}) => (
    <section className="session-summary-panel" aria-label="Session overview">
        <article className="session-summary-card">
            <p className="session-summary-label">Total Sessions</p>
            <p className="session-summary-value">{totalSessions}</p>
        </article>
        <article className="session-summary-card">
            <p className="session-summary-label">Active Sessions</p>
            <p className="session-summary-value">{activeSessions}</p>
        </article>
        <article className="session-summary-card">
            <p className="session-summary-label">Last Login</p>
            <p className="session-summary-value session-summary-value--small">{lastLogin}</p>
        </article>
    </section>
);

export default SummarySection;

