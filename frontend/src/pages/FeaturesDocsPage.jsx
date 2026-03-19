import {useEffect, useMemo, useRef, useState} from 'react';
import DocsLayout from '../components/DocsLayout';
import './FeaturesDocsPage.css';

const FEATURE_LIST = [
    {
        id: 'jwt',
        name: 'JWT-Based Authentication',
        problem: 'Server-side memory sessions break across replicas and load balancers.',
        why: 'Stateless tokens allow any API node to authenticate without shared session memory.',
        steps: [{label: 'User logs in'}, {label: 'Server verifies credentials'}, {label: 'Server issues JWT'}, {label: 'Client sends token on each request'}],
    },
    {
        id: 'rsa',
        name: 'RSA Token Signing',
        problem: 'Symmetric secrets are risky to distribute to many services.',
        why: 'Private key signs once; public keys verify anywhere without exposing signer secret.',
        steps: [{label: 'Private key signs JWT'}, {label: 'Gateway/API uses public key'}, {label: 'Tampered token fails verification'}],
    },
    {
        id: 'refresh-rotation',
        name: 'Refresh Token Rotation',
        problem: 'Static refresh tokens can be replayed after theft.',
        why: 'Every refresh invalidates previous token, making replay detectable and blockable.',
        steps: [{label: 'Client sends refresh token'}, {label: 'Server validates token family'}, {label: 'New access + refresh token issued'}, {label: 'Old refresh token revoked'}],
    },
    {
        id: 'access-expiration',
        name: 'Access Token Expiration',
        problem: 'Long-lived bearer tokens extend attacker access window.',
        why: '15-minute access tokens sharply reduce theft impact.',
        steps: [{label: 'Access token minted'}, {label: 'Token used briefly'}, {label: 'Token expires quickly'}, {label: 'Refresh path required'}],
    },
    {
        id: 'refresh-revocation',
        name: 'Refresh Token Revocation',
        problem: 'Compromised long-lived sessions persist unseen.',
        why: 'Immediate revocation cuts off compromised sessions in real-time.',
        steps: [{label: 'Threat detected'}, {label: 'Token marked revoked'}, {label: 'Next refresh denied'}, {label: 'User re-auth required'}],
    },
    {
        id: 'device-sessions',
        name: 'Device-Aware Sessions',
        problem: 'Users cannot see suspicious active sessions.',
        why: 'Session inventory enables remote device sign-out.',
        steps: [{label: 'Device fingerprint stored'}, {label: 'Session listed in dashboard'}, {label: 'User revokes unknown device'}],
    },
    {
        id: 'multi-identifier',
        name: 'Multi-Identifier Login',
        problem: 'Rigid login identifiers increase account recovery friction.',
        why: 'Email, username, or phone improve accessibility without lowering security.',
        steps: [{label: 'User provides identifier'}, {label: 'Backend resolves account'}, {label: 'Password/OTP challenge continues'}],
    },
    {
        id: 'oauth2',
        name: 'OAuth2 Integration',
        problem: 'Password-only login increases credential handling burden.',
        why: 'Federated login delegates identity proofing to trusted providers (e.g., Google).',
        steps: [{label: 'User chooses provider'}, {label: 'OAuth consent flow'}, {label: 'Backend validates provider token'}, {label: 'Local session created'}],
    },
    {
        id: 'otp',
        name: 'OTP Verification',
        problem: 'Automated signups and fake accounts abuse resources.',
        why: 'OTP confirms control of email/phone and deters scripted abuse.',
        steps: [{label: 'Signup initiated'}, {label: 'OTP generated + sent'}, {label: 'User submits OTP'}, {label: 'Account marked verified'}],
    },
    {
        id: 'rbac',
        name: 'Role-Based Access Control (RBAC)',
        problem: 'All authenticated users gain equal privileges.',
        why: 'Roles and permissions enforce least-privilege access.',
        steps: [{label: 'User authenticated'}, {label: 'Roles loaded'}, {label: 'Policy check executed'}, {label: 'Request allowed or denied'}],
    },
    {
        id: 'rate-limit',
        name: 'Redis Rate Limiting',
        problem: 'Attackers can brute-force login endpoints at scale.',
        why: 'Distributed counters throttle abusive clients before credential stuffing succeeds.',
        steps: [{label: 'Request arrives'}, {label: 'Redis counter incremented'}, {label: 'Threshold exceeded?'}, {label: 'Block or continue'}],
    },
    {
        id: 'audit-logging',
        name: 'Security Audit Logging',
        problem: 'No reliable trail for incident response and forensics.',
        why: 'Structured auth-event logs reveal anomalies and support investigations.',
        steps: [{label: 'Auth event occurs'}, {label: 'Context metadata attached'}, {label: 'Event persisted'}, {label: 'Alerting/analysis pipeline reads logs'}],
    },
    {
        id: 'db-migration',
        name: 'Database Migration',
        problem: 'Manual schema updates cause drift and deployment risk.',
        why: 'Versioned migrations provide deterministic, rollback-safe schema changes.',
        steps: [{label: 'Migration script committed'}, {label: 'CI validates'}, {label: 'Deploy applies migration'}, {label: 'Schema version recorded'}],
    },
    {
        id: 'db-indexing',
        name: 'Database Indexing',
        problem: 'Auth lookups degrade under load without targeted indexes.',
        why: 'Indexes on user identifiers and token hashes maintain low-latency auth checks.',
        steps: [{label: 'Query login identifier'}, {label: 'Index seek'}, {label: 'Fast row retrieval'}, {label: 'Auth decision returned'}],
    },
    {
        id: 'token-hashing',
        name: 'Token Hashing in Database',
        problem: 'Plain refresh tokens in DB become bearer secrets on breach.',
        why: 'Hashing stored refresh tokens prevents direct token reuse after data exposure.',
        steps: [{label: 'Token generated'}, {label: 'Hash persisted'}, {label: 'Presented token hashed for compare'}, {label: 'Match grants refresh'}],
    },
];

const SPEED_MS = {
    slow: 1100,
    normal: 700,
    fast: 380,
};

const getNodeState = (status, activeStep, index) => {
    if (status === 'idle') return 'idle';
    if (status === 'running') {
        if (index < activeStep) return 'completed';
        if (index === activeStep) return 'active';
        return 'idle';
    }
    if (status === 'success') return index <= activeStep ? 'completed' : 'idle';
    if (status === 'error') {
        if (index < activeStep) return 'completed';
        if (index === activeStep) return 'error';
    }
    return 'idle';
};

const getConnectorState = (status, activeStep, index) => {
    if (status === 'running') {
        if (index < activeStep - 1) return 'completed';
        if (index === activeStep - 1) return 'active';
        return 'idle';
    }
    if (status === 'success') return 'completed';
    if (status === 'error') {
        if (index < activeStep - 1) return 'completed';
        if (index === activeStep - 1) return 'error';
    }
    return 'idle';
};

const StepNode = ({step, index, status, activeStep, nodeRef}) => {
    const state = getNodeState(status, activeStep, index);
    return (
        <div ref={nodeRef} className={`security-node security-node--${state}`}>
            <span className="security-node__index">{index + 1}</span>
            <span className="security-node__label">{step.label}</span>
        </div>
    );
};

const StepConnector = ({state, mobile = false}) => {
    const pathClass = `security-connector__path security-connector__path--${state}`;
    return (
        <svg
            className={`security-connector ${mobile ? 'security-connector--mobile' : 'security-connector--desktop'}`}
            viewBox={mobile ? '0 0 40 36' : '0 0 52 40'}
            fill="none"
            aria-hidden="true"
        >
            <path className={pathClass} d={mobile ? 'M20 4 L20 32' : 'M4 20 L48 20'}/>
        </svg>
    );
};

const FeaturesDocsPage = () => {
    const [activeFeature, setActiveFeature] = useState('jwt');
    const [activeStep, setActiveStep] = useState(0);
    const [status, setStatus] = useState('idle');
    const [speed, setSpeed] = useState('normal');
    const [isPaused, setIsPaused] = useState(false);
    const [isMobile, setIsMobile] = useState(() => window.innerWidth < 960);

    const sidebarItemRefs = useRef({});
    const stepNodeRefs = useRef([]);

    const currentFeature = useMemo(
        () => FEATURE_LIST.find((feature) => feature.id === activeFeature) ?? FEATURE_LIST[0],
        [activeFeature]
    );

    useEffect(() => {
        const onResize = () => setIsMobile(window.innerWidth < 960);
        window.addEventListener('resize', onResize);
        return () => window.removeEventListener('resize', onResize);
    }, []);

    useEffect(() => {
        if (status !== 'running' || isPaused) return undefined;

        const timeoutId = window.setTimeout(() => {
            if (activeStep < currentFeature.steps.length - 1) {
                setActiveStep((prev) => prev + 1);
                return;
            }
            setStatus('success');
            setIsPaused(false);
        }, SPEED_MS[speed] ?? SPEED_MS.normal);

        return () => window.clearTimeout(timeoutId);
    }, [status, isPaused, activeStep, speed, currentFeature.steps.length]);

    useEffect(() => {
        if (status === 'idle') return;
        const node = stepNodeRefs.current[activeStep];
        if (!node) return;
        node.scrollIntoView({behavior: 'smooth', block: 'nearest', inline: 'center'});
    }, [activeStep, status, activeFeature]);

    useEffect(() => {
        const item = sidebarItemRefs.current[activeFeature];
        if (!item) return;
        item.scrollIntoView({behavior: 'smooth', block: 'nearest'});
    }, [activeFeature]);

    const handleFeatureSelect = (featureId) => {
        setActiveFeature(featureId);
        setActiveStep(0);
        setStatus('idle');
        setIsPaused(false);
    };

    const handlePlay = () => {
        if (status === 'success' || status === 'error') {
            setActiveStep(0);
        }
        setStatus('running');
        setIsPaused(false);
    };

    const handlePause = () => {
        if (status !== 'running') return;
        setIsPaused(true);
    };

    const handleReset = () => {
        setActiveStep(0);
        setStatus('idle');
        setIsPaused(false);
    };

    const statusText = isPaused ? 'paused' : status;
    const visibleStep = status === 'idle' ? 0 : Math.min(activeStep, currentFeature.steps.length - 1);
    const currentStepText = currentFeature.steps[visibleStep]?.label ?? currentFeature.steps[0]?.label;

    return (
        <DocsLayout title="Security Features"
                    subtitle="Each feature exists to close a specific failure mode in authentication systems.">
            <section className="security-explorer">
                <aside className="security-nav" aria-label="Security feature navigator">
                    {FEATURE_LIST.map((feature) => (
                        <button
                            key={feature.id}
                            ref={(node) => {
                                sidebarItemRefs.current[feature.id] = node;
                            }}
                            type="button"
                            onClick={() => handleFeatureSelect(feature.id)}
                            className={`security-nav__item ${activeFeature === feature.id ? 'security-nav__item--active' : ''}`}
                        >
                            {feature.name}
                        </button>
                    ))}
                </aside>

                <main className="security-panel">
                    <header className="security-panel__header">
                        <h3 className="security-panel__title">{currentFeature.name}</h3>
                        <div className="security-panel__tags">
                            <p className="security-panel__tagline">
                                <span className="security-panel__tag security-panel__tag--problem">Problem</span>
                                {currentFeature.problem}
                            </p>
                            <p className="security-panel__tagline">
                                <span className="security-panel__tag security-panel__tag--why">Why</span>
                                {currentFeature.why}
                            </p>
                        </div>
                    </header>

                    <section className="security-controls" aria-label="Flow controls">
                        <button type="button" className="security-btn security-btn--primary" onClick={handlePlay}>
                            ▶ Play Flow
                        </button>
                        <button type="button" className="security-btn" onClick={handlePause}
                                disabled={status !== 'running' || isPaused}>
                            ⏸ Pause
                        </button>
                        <button type="button" className="security-btn" onClick={handleReset}>
                            ↺ Reset
                        </button>

                        <div className="security-speed" role="group" aria-label="Flow speed">
                            {['slow', 'normal', 'fast'].map((value) => (
                                <button
                                    key={value}
                                    type="button"
                                    className={`security-speed__item ${speed === value ? 'security-speed__item--active' : ''}`}
                                    onClick={() => setSpeed(value)}
                                >
                                    {value[0].toUpperCase() + value.slice(1)}
                                </button>
                            ))}
                        </div>
                    </section>

                    <div className="security-flow-meta">
                        <span
                            className="security-flow-meta__badge">Step {visibleStep + 1}/{currentFeature.steps.length}</span>
                        <span
                            className={`security-flow-meta__badge security-flow-meta__badge--${status === 'running' && !isPaused ? 'running' : statusText}`}>
                            {statusText}
                        </span>
                    </div>

                    <section
                        className={`security-pipeline ${isMobile ? 'security-pipeline--mobile' : 'security-pipeline--desktop'}`}
                        aria-label="Pipeline flow">
                        {currentFeature.steps.map((step, index) => {
                            const connectorState = getConnectorState(status, activeStep, index);
                            return (
                                <div key={`${currentFeature.id}-${step.label}`} className="security-pipeline__segment">
                                    <StepNode
                                        step={step}
                                        index={index}
                                        status={status}
                                        activeStep={activeStep}
                                        nodeRef={(node) => {
                                            stepNodeRefs.current[index] = node;
                                        }}
                                    />
                                    {index < currentFeature.steps.length - 1 &&
                                        <StepConnector state={connectorState} mobile={isMobile}/>}
                                </div>
                            );
                        })}
                    </section>

                    <div className="security-step-detail">
                        <p className="security-step-detail__label">Current step</p>
                        <p className="security-step-detail__text">{currentStepText}</p>
                    </div>
                </main>
            </section>
        </DocsLayout>
    );
};

export default FeaturesDocsPage;
