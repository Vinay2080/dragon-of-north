import {useState} from 'react';
import {Link} from 'react-router-dom';

const HomeDocsPage = () => {
    const [activeFlowStepId, setActiveFlowStepId] = useState('login');
    const [expandedTopicId, setExpandedTopicId] = useState('jwt-lifecycle');

    const deepDiveTopics = [
        {
            id: 'jwt-lifecycle',
            title: 'JWT Lifecycle',
            summary: 'Short-lived access tokens keep each request fast and limit blast radius.',
            detail: 'After authentication, the platform issues a signed access token with minimal claims. API calls validate the signature locally, reducing database pressure. As expiry approaches, the refresh flow rotates credentials and invalidates older material.',
            cta: {text: 'View flow', link: '/architecture'}
        },
        {
            id: 'refresh-rotation',
            title: 'Refresh Token Rotation',
            summary: 'Every refresh replaces old credentials to reduce replay risk.',
            detail: 'The server stores hashed refresh material and rotates it at each renewal. If replay is detected, the active chain can be revoked immediately. Users keep seamless sessions while the backend enforces strict token lineage.',
            cta: {text: 'Learn more', link: '/features'}
        },
        {
            id: 'session-tracking',
            title: 'Session Tracking',
            summary: 'Track active devices, locations, and revoke suspicious access quickly.',
            detail: 'Each login creates a session record with device metadata and activity timestamps. Users and teams can review active sessions, remove stale devices, and respond to unusual behavior without waiting for token expiry.',
            cta: {text: 'Explore sessions', link: '/sessions'}
        },
        {
            id: 'token-expiry',
            title: 'Token Expiry',
            summary: 'Tight access expiry with controlled refresh windows for safer defaults.',
            detail: 'Access tokens expire quickly and refresh validity is bounded by policy. High-risk events can enforce stricter limits in real time. Expiry events are audit-friendly, helping teams monitor security posture over time.',
            cta: {text: 'Learn more', link: '/security-demo'}
        }
    ];

    const howItWorksSteps = [
        {
            id: 'login',
            title: 'Secure Login',
            short: 'Identity and trust checks run at the edge.',
            detail: 'Users authenticate with email or phone. The platform validates credentials, policy constraints, and account state before creating an authenticated session context.'
        },
        {
            id: 'access-token',
            title: 'Access Token Issued',
            short: 'A short-lived JWT is generated for API access.',
            detail: 'The access token carries minimal claims and strict expiry. Services verify signatures locally for speed while reducing persistent credential exposure.'
        },
        {
            id: 'expiry',
            title: 'Token Expires',
            short: 'Expiration limits the impact of leaked tokens.',
            detail: 'Access tokens are intentionally short-lived. If compromised, their validity window is narrow, which lowers attack persistence and simplifies incident handling.'
        },
        {
            id: 'refresh',
            title: 'Token Refresh',
            short: 'Refresh rotation renews credentials safely.',
            detail: 'On renewal, the backend rotates refresh credentials and invalidates old pairs. This flow maintains continuity while reducing replay opportunities.'
        },
        {
            id: 'session',
            title: 'Session Tracked',
            short: 'Every device session remains observable and revocable.',
            detail: 'Session records include device, network, and activity context. Teams can inspect active devices and revoke suspicious sessions in one action.'
        }
    ];

    const activeFlowStep = howItWorksSteps.find((step) => step.id === activeFlowStepId) || howItWorksSteps[0];

    const toggleTopic = (topicId) => {
        setExpandedTopicId((prev) => (prev === topicId ? null : topicId));
    };

    return (
        <div className="home-story">
            <div className="home-story__container">
                <section className="story-section story-section--hero">
                    <p className="story-kicker">Authentication Platform</p>
                    <h1 className="story-title">Control Sessions. Not Just Logins.</h1>
                    <p className="story-subtitle">
                        A compact authentication journey with short-lived access tokens, rotation-aware refresh flow,
                        and fully visible sessions.
                    </p>
                    <div className="story-actions">
                        <Link to="/sessions" className="story-btn story-btn--primary">Explore sessions</Link>
                        <Link to="/architecture" className="story-btn story-btn--ghost">View flow</Link>
                    </div>
                </section>

                <section className="story-section story-section--how">
                    <div className="story-section__header">
                        <h2 className="story-section__title">How It Works</h2>
                        <p className="story-section__description">Select a step to inspect the flow without leaving the
                            page.</p>
                    </div>

                    <div className="story-step-grid">
                        {howItWorksSteps.map((step, index) => (
                            <button
                                key={step.id}
                                type="button"
                                onClick={() => setActiveFlowStepId(step.id)}
                                className={`story-step-card ${activeFlowStepId === step.id ? 'is-active' : ''}`}
                                aria-pressed={activeFlowStepId === step.id}
                            >
                                <span className="story-step-card__index">Step {index + 1}</span>
                                <span className="story-step-card__title">{step.title}</span>
                                <span className="story-step-card__summary">{step.short}</span>
                            </button>
                        ))}
                    </div>

                    <div className="story-step-detail" role="region" aria-live="polite">
                        <p className="story-step-detail__title">{activeFlowStep.title}</p>
                        <p className="story-step-detail__body">{activeFlowStep.detail}</p>
                        <div className="story-actions story-actions--inline">
                            <Link to="/features" className="story-btn story-btn--ghost">Learn more</Link>
                            <Link to="/identifier-flow" className="story-btn story-btn--ghost">View flow</Link>
                            <Link to="/sessions" className="story-btn story-btn--primary">Explore sessions</Link>
                        </div>
                    </div>
                </section>

                <section className="story-section story-section--deep">
                    <div className="story-section__header">
                        <h2 className="story-section__title">Deep Dive</h2>
                        <p className="story-section__description">Open a topic for details, keep the page concise by
                            default.</p>
                    </div>

                    <div className="story-deep-grid">
                        {deepDiveTopics.map((topic) => {
                            const isExpanded = expandedTopicId === topic.id;
                            return (
                                <article key={topic.id} className={`story-deep-card ${isExpanded ? 'is-open' : ''}`}>
                                    <button
                                        type="button"
                                        onClick={() => toggleTopic(topic.id)}
                                        className="story-deep-card__trigger"
                                        aria-expanded={isExpanded}
                                    >
                                        <span className="story-deep-card__title">{topic.title}</span>
                                        <span className="story-deep-card__summary">{topic.summary}</span>
                                    </button>

                                    <div className={`story-deep-card__details ${isExpanded ? 'is-visible' : ''}`}>
                                        <p className="story-deep-card__body">{topic.detail}</p>
                                        <div className="story-actions story-actions--inline">
                                            <Link to={topic.cta.link}
                                                  className="story-btn story-btn--ghost">{topic.cta.text}</Link>
                                            <Link to="/sessions" className="story-btn story-btn--primary">Explore
                                                sessions</Link>
                                        </div>
                                    </div>
                                </article>
                            );
                        })}
                    </div>
                </section>

                <section className="story-section story-section--cta">
                    <h2 className="story-section__title">Build a Security Story Users Can Trust</h2>
                    <p className="story-section__description">
                        Keep the experience simple while enforcing strong defaults for tokens, rotation, and session
                        control.
                    </p>
                    <div className="story-actions">
                        <Link to="/features" className="story-btn story-btn--ghost">Learn more</Link>
                        <Link to="/architecture" className="story-btn story-btn--ghost">View flow</Link>
                        <Link to="/sessions" className="story-btn story-btn--primary">Explore sessions</Link>
                    </div>
                </section>
            </div>
        </div>
    );
};

export default HomeDocsPage;
