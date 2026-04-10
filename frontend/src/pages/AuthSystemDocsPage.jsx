'use client';

import { useState, useRef, useEffect } from 'react';
import { ChevronRight, Sun, Moon } from 'lucide-react';
import '../styles/auth-system-docs.css';

// Data structures for sections
const SECTIONS = [
  { id: 'overview', label: 'Overview', number: '01' },
  { id: 'architecture', label: 'Architecture', number: '02' },
  { id: 'token-lifecycle', label: 'Token Lifecycle', number: '03' },
  { id: 'session-model', label: 'Session Model', number: '04' },
  { id: 'identifier-flow', label: 'Identifier Flow', number: '05' },
  { id: 'otp-engine', label: 'OTP Engine', number: '06' },
  { id: 'concurrent-refresh', label: 'Concurrent Refresh', number: '07' },
  { id: 'attack-surface', label: 'Attack Surface', number: '08' },
  { id: 'design-decisions', label: 'Design Decisions', number: '09' },
];

const DESIGN_DECISIONS = [
  {
    title: 'Store token hash, not raw token',
    threat: 'Database breach could expose valid refresh tokens',
    decision: 'Hash refresh tokens with SHA-256, store only the hash',
    evidence: 'tokenHash = SHA256(raw_refresh_token)\nValidation: hash(incoming) === stored_hash',
    tradeoff: 'Cannot recover raw token if hashed incorrectly; requires careful generation'
  },
  {
    title: 'Bind tokens to device fingerprint',
    threat: 'Token theft on one device would work on any device',
    decision: 'Include deviceId in JWT, validate on every refresh against session record',
    evidence: 'const match = currentDevice.id === session.deviceId\nif (!match) throw SECURITY_EVENT',
    tradeoff: 'Device spoofing could bypass; requires robust fingerprinting'
  },
  {
    title: 'Optimistic locking for refresh races',
    threat: 'Concurrent refresh requests could double-issue tokens',
    decision: 'Use version counter, UPDATE WHERE version=N fails if already rotated',
    evidence: 'UPDATE sessions SET version=version+1\nWHERE id=? AND version=?\nResult: success | OptimisticLockException',
    tradeoff: 'Client must handle 401 and retry; adds latency on high-concurrency'
  },
  {
    title: 'Consume OTP on success, not on attempt',
    threat: 'Attacker could enumerate passwords/methods with unlimited guesses',
    decision: 'Mark OTP as consumed only after successful password/provider verification',
    evidence: 'if (password_hash_valid) {\n  UPDATE otp SET consumed=true, used_at=NOW()\n}',
    tradeoff: 'OTP remains valid across failed password attempts; relies on password strength'
  },
  {
    title: 'Reject family rotation on device mismatch',
    threat: 'Stolen old tokens could be used if rotation continues on new device',
    decision: 'If deviceId changes mid-family, invalidate entire family and force re-auth',
    evidence: 'if (refresh_deviceId !== session_deviceId) {\n  UPDATE sessions SET revoked=true WHERE family_id=?\n}',
    tradeoff: 'Users on multi-device lose convenience; security > UX'
  },
  {
    title: 'Log security events without exposing cause',
    threat: 'Timing/error message patterns leak system logic to attackers',
    decision: 'All auth failures return 401 Unauthorized with generic "Invalid credentials"',
    evidence: '[2024-01-15 14:32:01] SECURITY_EVENT type=DEVICE_MISMATCH user_id=abc\n← logged server-side, client sees 401 only',
    tradeoff: 'Harder for legitimate users to debug; essential for security'
  }
];

const ATTACK_SCENARIOS = [
  {
    scenario: 'Replay Attack',
    behavior: 'Detect duplicate refresh token usage via version check and family tracking',
    clientSees: '401 Unauthorized',
    httpStatus: '401',
    logLine: '[2024-01-15 14:32:01] SECURITY_EVENT type=REPLAY_DETECTED user_id=abc family_id=xyz'
  },
  {
    scenario: 'Expired OTP',
    behavior: 'Check otp.expired_at < NOW(), reject if expired',
    clientSees: '401 Unauthorized',
    httpStatus: '401',
    logLine: '[2024-01-15 14:32:02] AUTH_FAILED type=OTP_EXPIRED user_id=def code=OTP_000'
  },
  {
    scenario: 'Device Mismatch',
    behavior: 'Compare JWT deviceId with session.deviceId, reject if different',
    clientSees: '401 Unauthorized',
    httpStatus: '401',
    logLine: '[2024-01-15 14:32:03] SECURITY_EVENT type=DEVICE_MISMATCH user_id=ghi session_id=uvw'
  },
  {
    scenario: 'Concurrent Race',
    behavior: 'Both requests attempt UPDATE WHERE version=1, second fails OptimisticLockException',
    clientSees: '401 Unauthorized',
    httpStatus: '401',
    logLine: '[2024-01-15 14:32:04] TOKEN_REFRESH type=RACE_DETECTED user_id=jkl version_conflict=true'
  },
  {
    scenario: 'Brute Force OTP',
    behavior: 'Increment attempt counter, block after 5 failed attempts for 15 minutes',
    clientSees: '429 Too Many Requests',
    httpStatus: '429',
    logLine: '[2024-01-15 14:32:05] SECURITY_EVENT type=OTP_BRUTE_FORCE user_id=mno attempts=6 blocked_until=14:47'
  },
  {
    scenario: 'Revoked Family',
    behavior: 'Check session.revoked flag before processing, reject if true',
    clientSees: '401 Unauthorized',
    httpStatus: '401',
    logLine: '[2024-01-15 14:32:06] SECURITY_EVENT type=FAMILY_REVOKED user_id=pqr family_id=stu reason=DEVICE_CHANGE'
  }
];

const DB_RECORD_EXAMPLE = {
  id: 'sess_7a8b9c0d1e2f3g4h5i6j',
  userId: 'user_abc123xyz',
  tokenHash: 'sha256_c8d9e0f1g2h3i4j5k6l7m8n9o0p1q2r3',
  familyId: 'fam_a1b2c3d4e5f6g7h8i9j0',
  deviceId: 'dev_9z8y7x6w5v4u3t2s1r0q',
  version: 3,
  revoked: false,
  expiresAt: '2024-02-15T14:32:01Z',
  createdAt: '2024-01-15T14:32:01Z',
  rotatedAt: '2024-01-15T15:02:34Z'
};

const DB_RECORD_FIELDS = {
  id: 'Unique session identifier; composite key for quick lookup',
  userId: 'Foreign key to users table; identifies the authenticated user',
  tokenHash: 'SHA-256 of raw refresh token; compared on every refresh, raw never stored',
  familyId: 'Groups rotated tokens in a chain; any token in family can invalidate all',
  deviceId: 'Fingerprint of client device; compared on every refresh, mismatch = SECURITY_EVENT + 401',
  version: 'Optimistic lock counter; UPDATE WHERE version=N fails if already rotated',
  revoked: 'Set true on: replay detection, device change mid-family, explicit logout. Checked before hash compare',
  expiresAt: 'Refresh token expiration; if NOW() > expiresAt, reject refresh request',
  createdAt: 'Session creation timestamp; used for audit and rate limiting',
  rotatedAt: 'Last rotation timestamp; used to detect stale rotations in concurrent scenarios'
};

// Token Lifecycle States
const TOKEN_LIFECYCLE_STATES = [
  {
    state: 'ISSUED',
    description: 'Token generated during login/signup',
    service: 'AuthService.generateTokens()',
    dbChange: 'INSERT INTO sessions',
    jwtClaim: 'iat = NOW()'
  },
  {
    state: 'ACTIVE',
    description: 'Token actively in use, passes all validations',
    service: 'TokenValidator.verify()',
    dbChange: 'None',
    jwtClaim: 'exp = issued_at + 15min'
  },
  {
    state: 'ROTATED',
    description: 'Token successfully refreshed, new token issued',
    service: 'TokenService.rotate()',
    dbChange: 'UPDATE sessions SET version++',
    jwtClaim: 'iat = NOW() (new token)'
  },
  {
    state: 'EXPIRED',
    description: 'Token past expiration window',
    service: 'TokenValidator.checkExpiry()',
    dbChange: 'None (session remains)',
    jwtClaim: 'exp < NOW()'
  },
  {
    state: 'INVALIDATED',
    description: 'Token revoked due to replay, device mismatch, or explicit logout',
    service: 'SessionService.revoke()',
    dbChange: 'UPDATE sessions SET revoked=true',
    jwtClaim: 'None (rejected before decode)'
  }
];

const ArchitectureDiagram = () => {
  const [hoveredNode, setHoveredNode] = useState(null);

  const nodes = [
    { id: 'token', x: 60, y: 80, label: 'Token Layer', desc: 'JWT handling, validation, claims' },
    { id: 'session', x: 200, y: 80, label: 'Session Layer', desc: 'Persistence, rotation, versioning' },
    { id: 'identity', x: 340, y: 80, label: 'Identity Layer', desc: 'User lookup, device binding' }
  ];

  return (
    <svg className="arch-diagram" viewBox="0 0 400 150">
      <defs>
        <marker id="arrowhead" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
          <polygon points="0 0, 10 3, 0 6" fill="var(--accent)" />
        </marker>
      </defs>

      {/* Connection lines */}
      {nodes.map((node, idx) => {
        if (idx < nodes.length - 1) {
          return (
            <line
              key={`line-${idx}`}
              x1={node.x + 45}
              y1={node.y + 25}
              x2={nodes[idx + 1].x - 10}
              y2={nodes[idx + 1].y + 25}
              stroke="var(--border)"
              strokeWidth="2"
              markerEnd="url(#arrowhead)"
              opacity={hoveredNode ? (hoveredNode === node.id || hoveredNode === nodes[idx + 1].id ? 1 : 0.2) : 0.5}
            />
          );
        }
        return null;
      })}

      {/* Nodes */}
      {nodes.map(node => (
        <g
          key={node.id}
          onMouseEnter={() => setHoveredNode(node.id)}
          onMouseLeave={() => setHoveredNode(null)}
          style={{ cursor: 'pointer' }}
        >
          <rect
            x={node.x - 35}
            y={node.y}
            width="80"
            height="50"
            rx="4"
            fill="var(--bg-subtle)"
            stroke={hoveredNode === node.id ? 'var(--accent)' : 'var(--border)'}
            strokeWidth="2"
            style={{ transition: 'all 100ms ease' }}
          />
          <text x={node.x} y={node.y + 18} textAnchor="middle" className="arch-node-label">
            {node.label}
          </text>
          {hoveredNode === node.id && (
            <text x={node.x} y={node.y + 40} textAnchor="middle" className="arch-node-desc">
              {node.desc}
            </text>
          )}
        </g>
      ))}
    </svg>
  );
};

const TokenLifecycleTimeline = () => {
  const [selectedState, setSelectedState] = useState(0);

  return (
    <div className="token-lifecycle-container">
      <div className="timeline">
        {TOKEN_LIFECYCLE_STATES.map((state, idx) => (
          <div
            key={state.state}
            className={`timeline-dot ${selectedState === idx ? 'active' : ''}`}
            onClick={() => setSelectedState(idx)}
            style={{ cursor: 'pointer' }}
          >
            {selectedState === idx && <span className="dot-inner" />}
          </div>
        ))}
      </div>

      <div className="state-detail">
        <div className="state-label">STATE {String(selectedState + 1).padStart(2, '0')}</div>
        <h4>{TOKEN_LIFECYCLE_STATES[selectedState].state}</h4>
        <div className="state-info">
          <div className="info-row">
            <span className="label">Service:</span>
            <span className="value">{TOKEN_LIFECYCLE_STATES[selectedState].service}</span>
          </div>
          <div className="info-row">
            <span className="label">DB Change:</span>
            <span className="value">{TOKEN_LIFECYCLE_STATES[selectedState].dbChange}</span>
          </div>
          <div className="info-row">
            <span className="label">JWT Claim:</span>
            <span className="value code">{TOKEN_LIFECYCLE_STATES[selectedState].jwtClaim}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

const DBRecordExplorer = () => {
  const [hoveredField, setHoveredField] = useState(null);

  return (
    <div className="db-explorer">
      <div className="json-viewer">
        <div className="json-line">
          <span className="json-brace">{'{'}</span>
        </div>
        {Object.entries(DB_RECORD_EXAMPLE).map(([key, value]) => (
          <div
            key={key}
            className={`json-field ${hoveredField === key ? 'highlighted' : ''}`}
            onMouseEnter={() => setHoveredField(key)}
            onMouseLeave={() => setHoveredField(null)}
          >
            <span className="json-key">"{key}"</span>
            <span className="json-colon">:</span>
            <span className="json-value">"{value}"</span>
            <span className="json-comma">,</span>
            {hoveredField === key && (
              <div className="field-tooltip">{DB_RECORD_FIELDS[key]}</div>
            )}
          </div>
        ))}
        <div className="json-line">
          <span className="json-brace">{'}'}</span>
        </div>
      </div>
    </div>
  );
};

const IdentifierFlowDiagram = () => {
  const [selectedBranch, setSelectedBranch] = useState(null);

  const branches = {
    exists: {
      verified: {
        local: {
          text: 'local password path',
          response: '{ next_action: "PASSWORD_PROMPT", user_exists: true }'
        },
        oauth: {
          text: 'OAuth provider path',
          response: '{ next_action: "OAUTH_REDIRECT", provider: "google" }'
        }
      },
      notVerified: {
        text: 'email verification required',
        response: '{ next_action: "EMAIL_VERIFICATION", retry_after: "5min" }'
      }
    },
    notExists: {
      text: 'new user signup',
      response: '{ next_action: "SIGNUP_REQUIRED", user_exists: false }'
    }
  };

  return (
    <div className="identifier-flow">
      <div className="flow-start">Submit identifier</div>

      <div className="flow-question">
        <div className="q-text">User exists?</div>
        <div className="flow-branches">
          <div
            className={`branch ${selectedBranch === 'notExists' ? 'active' : ''}`}
            onClick={() => setSelectedBranch('notExists')}
          >
            <span className="branch-label">NO</span>
            <div className="branch-result">
              <div className="result-text">{branches.notExists.text}</div>
              <div className="result-status">200 OK</div>
            </div>
          </div>

          <div className={`branch-vertical ${selectedBranch?.startsWith('exists') ? 'active' : ''}`}>
            <span className="branch-label">YES</span>
            <div className="sub-question">
              <div className="q-text">email_verified?</div>
              <div className="sub-branches">
                <div
                  className={`sub-branch ${selectedBranch === 'exists.notVerified' ? 'active' : ''}`}
                  onClick={() => setSelectedBranch('exists.notVerified')}
                >
                  <span className="label">NO</span>
                  <div className="result">
                    <span>{branches.exists.notVerified.text}</span>
                    <div className="code">{branches.exists.notVerified.response}</div>
                  </div>
                </div>

                <div className={`sub-branch ${selectedBranch?.startsWith('exists.verified') ? 'active' : ''}`}>
                  <span className="label">YES</span>
                  <div className="provider-check">
                    <div className="q-text">provider type?</div>
                    <div className="providers">
                      <div
                        className={`provider ${selectedBranch === 'exists.verified.local' ? 'active' : ''}`}
                        onClick={() => setSelectedBranch('exists.verified.local')}
                      >
                        <span>LOCAL</span>
                        <div className="result code">{branches.exists.verified.local.response}</div>
                      </div>
                      <div
                        className={`provider ${selectedBranch === 'exists.verified.oauth' ? 'active' : ''}`}
                        onClick={() => setSelectedBranch('exists.verified.oauth')}
                      >
                        <span>GOOGLE</span>
                        <div className="result code">{branches.exists.verified.oauth.response}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {selectedBranch && (
        <div className="flow-detail">
          {selectedBranch === 'notExists' && (
            <div className="detail-content">
              <div className="detail-label">Next Action</div>
              <div className="detail-response">{branches.notExists.response}</div>
              <div className="detail-note">User account does not exist. Frontend should redirect to signup flow.</div>
            </div>
          )}
          {selectedBranch === 'exists.notVerified' && (
            <div className="detail-content">
              <div className="detail-label">Next Action</div>
              <div className="detail-response">{branches.exists.notVerified.response}</div>
              <div className="detail-note">Email unverified. Send verification link and require confirmation before proceeding.</div>
            </div>
          )}
          {selectedBranch === 'exists.verified.local' && (
            <div className="detail-content">
              <div className="detail-label">Next Action</div>
              <div className="detail-response">{branches.exists.verified.local.response}</div>
              <div className="detail-note">Verified local user. Frontend prompts for password entry.</div>
            </div>
          )}
          {selectedBranch === 'exists.verified.oauth' && (
            <div className="detail-content">
              <div className="detail-label">Next Action</div>
              <div className="detail-response">{branches.exists.verified.oauth.response}</div>
              <div className="detail-note">Verified OAuth user. Redirect to Google Sign-In flow.</div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

const ConcurrentRefreshAnimation = () => {
  const [step, setStep] = useState(0);

  const steps = [
    {
      title: 'Both requests arrive',
      desc: 'Request A and Request B both attempt to refresh simultaneously',
      highlight: ['a', 'b']
    },
    {
      title: 'Both SELECT session',
      desc: 'Both queries fetch: version=1',
      highlight: ['a', 'b'],
      sql: 'SELECT * FROM sessions WHERE id=?'
    },
    {
      title: 'Request A commits',
      desc: 'A executes UPDATE WHERE version=1, succeeds. version becomes 2.',
      highlight: ['a'],
      sql: 'UPDATE sessions SET version=2 WHERE id=? AND version=1',
      result: '✓ 1 row affected'
    },
    {
      title: 'Request B fails',
      desc: 'B attempts same UPDATE, but version is now 2. No rows match.',
      highlight: ['b'],
      sql: 'UPDATE sessions SET version=2 WHERE id=? AND version=1',
      result: '✗ 0 rows affected (OptimisticLockException)'
    },
    {
      title: 'Request B returns 401',
      desc: 'Client receives 401 Unauthorized + clears cookies. Frontend redirects to login.',
      highlight: ['b'],
      response: '{\n  "error": "INVALID_TOKEN",\n  "status": 401\n}'
    }
  ];

  const maxSteps = steps.length - 1;

  return (
    <div className="concurrent-refresh-animation">
      <div className="animation-columns">
        <div className="request-column">
          <div className={`request-box ${steps[step].highlight.includes('a') ? 'highlighted' : ''}`}>
            <div className="request-label">REQUEST A</div>
            {steps[step].sql && <div className="sql-box">{steps[step].sql}</div>}
            {steps[step].result && steps[step].highlight.includes('a') && (
              <div className="result-box success">{steps[step].result}</div>
            )}
          </div>
        </div>

        <div className="request-column">
          <div className={`request-box ${steps[step].highlight.includes('b') ? 'highlighted' : ''}`}>
            <div className="request-label">REQUEST B</div>
            {steps[step].sql && <div className="sql-box">{steps[step].sql}</div>}
            {steps[step].result && steps[step].highlight.includes('b') && (
              <div className="result-box error">{steps[step].result}</div>
            )}
          </div>
        </div>

        {steps[step].response && (
          <div className="response-box">
            <div className="response-label">HTTP Response</div>
            <div className="response-content">{steps[step].response}</div>
          </div>
        )}
      </div>

      <div className="step-info">
        <div className="step-title">{steps[step].title}</div>
        <div className="step-desc">{steps[step].desc}</div>
      </div>

      <div className="animation-controls">
        <button
          className="control-btn"
          onClick={() => setStep(Math.max(0, step - 1))}
          disabled={step === 0}
        >
          ← Previous
        </button>
        <span className="step-counter">{step + 1} / {steps.length}</span>
        <button
          className="control-btn"
          onClick={() => setStep(Math.min(maxSteps, step + 1))}
          disabled={step === maxSteps}
        >
          Next →
        </button>
      </div>

      <div className="step-explanation">
        Why optimistic locking instead of SELECT FOR UPDATE? No lock held during crypto operations.
        Request B fails fast, client retries cleanly. In high-concurrency, some retries expected.
      </div>
    </div>
  );
};

const AttackSurfaceTable = () => {
  const [expandedRow, setExpandedRow] = useState(null);

  return (
    <div className="attack-surface-table">
      <div className="table-header">
        <div className="col scenario">SCENARIO</div>
        <div className="col behavior">SYSTEM BEHAVIOR</div>
        <div className="col client">CLIENT SEES</div>
        <div className="col status">HTTP</div>
      </div>

      {ATTACK_SCENARIOS.map((row, idx) => (
        <div key={row.scenario}>
          <div
            className={`table-row ${expandedRow === idx ? 'expanded' : ''}`}
            onClick={() => setExpandedRow(expandedRow === idx ? null : idx)}
            style={{ cursor: 'pointer' }}
          >
            <div className="col scenario">{row.scenario}</div>
            <div className="col behavior">{row.behavior}</div>
            <div className="col client">{row.clientSees}</div>
            <div className="col status">{row.httpStatus}</div>
            <div className="expand-indicator">
              <ChevronRight size={16} style={{
                transform: expandedRow === idx ? 'rotate(90deg)' : 'rotate(0)',
                transition: 'transform 200ms'
              }} />
            </div>
          </div>

          {expandedRow === idx && (
            <div className="table-row-expanded">
              <div className="expanded-content">
                <div className="log-label">Server Log</div>
                <div className="log-line">{row.logLine}</div>
                <div className="expanded-note">
                  This log is visible to server logs only. Client always receives the same generic response.
                </div>
              </div>
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default function AuthSystemDocsPage() {
  const [activeSection, setActiveSection] = useState('overview');
  const [isDark, setIsDark] = useState(false);
  const contentRef = useRef(null);

  useEffect(() => {
    // Set theme on mount
    const isDarkPreferred = window.matchMedia('(prefers-color-scheme: dark)').matches;
    setIsDark(isDarkPreferred);
    updateTheme(isDarkPreferred);
  }, []);

  const updateTheme = (dark) => {
    if (dark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  };

  const toggleTheme = () => {
    const newTheme = !isDark;
    setIsDark(newTheme);
    updateTheme(newTheme);
  };

  const scrollToSection = (sectionId) => {
    setActiveSection(sectionId);
    const element = document.getElementById(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  return (
    <div className="auth-system-docs">
      <style>{`
        :root {
          --bg: #ffffff;
          --bg-subtle: #f7f7f8;
          --bg-code: #f0f0f2;
          --border: #e4e4e7;
          --text-primary: #0f0f10;
          --text-muted: #6b6b78;
          --text-code: #d63384;
          --accent: #0066ff;
          --accent-subtle: #eff4ff;
          --success: #16a34a;
          --error: #dc2626;
          --warning: #d97706;
        }

        :root.dark {
          --bg: #0a0c10;
          --bg-subtle: #0f1117;
          --bg-code: #161b22;
          --border: rgba(255,255,255,0.08);
          --text-primary: rgba(255,255,255,0.87);
          --text-muted: rgba(255,255,255,0.4);
          --text-code: #f97583;
          --accent: #4d9fff;
          --accent-subtle: rgba(77,159,255,0.08);
          --success: #34d399;
          --error: #f87171;
          --warning: #fb923c;
        }
      `}</style>

      <div className="docs-container">
        {/* Left Sidebar */}
        <aside className="docs-sidebar">
          <div className="sidebar-header">
            <span className="system-name">AUTH SYSTEM</span>
          </div>

          <nav className="sidebar-nav">
            {SECTIONS.map(section => (
              <a
                key={section.id}
                href={`#${section.id}`}
                className={`nav-link ${activeSection === section.id ? 'active' : ''}`}
                onClick={(e) => {
                  e.preventDefault();
                  scrollToSection(section.id);
                }}
              >
                {section.label}
              </a>
            ))}
          </nav>

          <div className="sidebar-footer">
            <button className="theme-toggle" onClick={toggleTheme} title="Toggle theme">
              {isDark ? <Sun size={16} /> : <Moon size={16} />}
            </button>
          </div>
        </aside>

        {/* Main Content */}
        <main className="docs-content" ref={contentRef}>
          {/* Overview */}
          <section id="overview" className="doc-section">
            <div className="section-label">SECTION 01</div>
            <h2>Overview</h2>

            <p>
              This system implements a stateless JWT authentication model with persisted refresh sessions.
              Tokens are short-lived (15 minutes), refresh tokens are long-lived (30 days), and all refresh
              operations are validated against server-side session records.
            </p>

            <p>
              What makes this non-tutorial: every refresh rotates the token and invalidates the previous one,
              all tokens are bound to a device fingerprint, concurrent refresh attempts use optimistic locking
              to prevent double-issuance, and OTP is consumed only on successful credential verification.
            </p>

            <p>
              For a recruiter: this is a production-grade auth system that balances developer convenience
              (stateless tokens, simple refresh) with security (rotation, device binding, race safety, audit logging).
            </p>

            <div className="guarantees">
              <div className="guarantee">
                <ChevronRight size={14} className="guarantee-icon" />
                <span>Every token is bound to a device fingerprint and validated on each refresh</span>
              </div>
              <div className="guarantee">
                <ChevronRight size={14} className="guarantee-icon" />
                <span>Refresh tokens rotate and are consumed one-time; stolen tokens cannot be reused</span>
              </div>
              <div className="guarantee">
                <ChevronRight size={14} className="guarantee-icon" />
                <span>Concurrent refresh requests are serialized with optimistic locking, never double-issued</span>
              </div>
            </div>
          </section>

          {/* Architecture */}
          <section id="architecture" className="doc-section">
            <div className="section-label">SECTION 02</div>
            <h2>Architecture</h2>

            <p>
              The system is layered in three independent concerns: Token (JWT generation and validation),
              Session (persistence and rotation), and Identity (user lookup and device binding).
              Each layer is testable in isolation and can scale independently.
            </p>

            <p>
              Hover over each layer below to see the service responsible and what it validates.
            </p>

            <ArchitectureDiagram />
          </section>

          {/* Token Lifecycle */}
          <section id="token-lifecycle" className="doc-section">
            <div className="section-label">SECTION 03</div>
            <h2>Token Lifecycle</h2>

            <p>
              From issuance to expiration, every token moves through five distinct states.
              Each state is guarded by specific validations and triggers DB updates.
              Click any state below to see what the system checks at that point.
            </p>

            <TokenLifecycleTimeline />
          </section>

          {/* Session Model */}
          <section id="session-model" className="doc-section">
            <div className="section-label">SECTION 04</div>
            <h2>Session Model</h2>

            <p>
              A session record represents one persistent refresh token and all rotations of it.
              Every field serves a specific validation function. Hover over fields in the example
              below to see what reads/writes it and what happens if it mismatches.
            </p>

            <DBRecordExplorer />
          </section>

          {/* Identifier Flow */}
          <section id="identifier-flow" className="doc-section">
            <div className="section-label">SECTION 05</div>
            <h2>Identifier Flow</h2>

            <p>
              When a user submits an identifier (email or username), the system must resolve:
              Does this user exist? Is their email verified? What authentication method do they use?
              Click each branch below to see the next_action returned to the frontend.
            </p>

            <IdentifierFlowDiagram />
          </section>

          {/* OTP Engine */}
          <section id="otp-engine" className="doc-section">
            <div className="section-label">SECTION 06</div>
            <h2>OTP Engine</h2>

            <p>
              OTPs are purpose-scoped: different codes for email verification, password reset, and login.
              Each OTP has an attempt counter and expiration. The key insight: OTP is marked consumed only
              after the subsequent credential (password or OAuth) is verified, not after the OTP code itself is validated.
            </p>

            <p>
              This prevents attackers from using OTP to brute-force passwords without consequence. After 5 failed
              password attempts following a valid OTP, the user is locked out for 15 minutes and must request a new OTP.
            </p>

            <div className="otp-states">
              <div className="state-box">
                <div className="state-name">PENDING</div>
                <div className="state-desc">Created, waiting for user submission</div>
              </div>
              <div className="state-box">
                <div className="state-name">VERIFIED</div>
                <div className="state-desc">Code matches, consumed on credential success</div>
              </div>
              <div className="state-box">
                <div className="state-name">EXPIRED</div>
                <div className="state-desc">15 minute window passed</div>
              </div>
              <div className="state-box">
                <div className="state-name">FAILED</div>
                <div className="state-desc">Max attempts (5) exceeded, blocked 15min</div>
              </div>
            </div>
          </section>

          {/* Concurrent Refresh */}
          <section id="concurrent-refresh" className="doc-section">
            <div className="section-label">SECTION 07</div>
            <h2>Concurrent Refresh</h2>

            <p>
              The race condition: two refresh requests arrive simultaneously, both fetch the same session
              with version=1, both attempt to rotate. Without protection, both would succeed and issue
              valid tokens — allowing token duplication.
            </p>

            <p>
              Solution: optimistic locking. The second UPDATE WHERE version=1 fails because version is now 2.
              The second request gets 401 + cleared cookies and must redirect to login. Why not SELECT FOR UPDATE?
              Because crypto operations (token signing, hash generation) must not hold database locks.
            </p>

            <ConcurrentRefreshAnimation />
          </section>

          {/* Attack Surface */}
          <section id="attack-surface" className="doc-section">
            <div className="section-label">SECTION 08</div>
            <h2>Attack Surface</h2>

            <p>
              The system's external surface consists of these attack scenarios. In each case, the server logs
              the exact security event (which attack was detected), but the client always receives a generic 401
              or 429. This prevents attackers from learning which validation failed.
            </p>

            <p>
              Click any row to expand and see the server log that would be generated (visible only in server logs,
              never sent to the client).
            </p>

            <AttackSurfaceTable />
          </section>

          {/* Design Decisions */}
          <section id="design-decisions" className="doc-section">
            <div className="section-label">SECTION 09</div>
            <h2>Design Decisions</h2>

            <p>
              Six key decisions that shaped this system. Each balances security, performance, and developer experience.
            </p>

            {DESIGN_DECISIONS.map((decision, idx) => (
              <div key={idx} className="design-decision">
                <h4>{decision.title}</h4>

                <div className="threat-block">
                  <div className="threat-label">Threat</div>
                  <div className="threat-text">{decision.threat}</div>
                </div>

                <div className="decision-section">
                  <div className="label">Decision</div>
                  <div className="text">{decision.decision}</div>
                </div>

                <div className="evidence-block">
                  <div className="label">Evidence</div>
                  <pre className="code">{decision.evidence}</pre>
                </div>

                <div className="tradeoff-section">
                  <em>{decision.tradeoff}</em>
                </div>

                {idx < DESIGN_DECISIONS.length - 1 && <div className="decision-divider" />}
              </div>
            ))}
          </section>
        </main>
      </div>
    </div>
  );
}
