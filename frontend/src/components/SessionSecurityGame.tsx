import {motion} from 'framer-motion';
import {Monitor, Shield, User} from 'lucide-react';
import {useState} from 'react';

type RiskLevel = 'safe' | 'suspicious';

type Session = {
    id: string;
    deviceName: string;
    deviceType: 'desktop' | 'mobile' | 'tablet';
    location: string;
    ipAddress: string;
    lastUsed: string;
    riskLevel: RiskLevel;
};

const SAFE_SESSIONS: Omit<Session, 'riskLevel'>[] = [
    {
        id: 's1',
        deviceName: 'MacBook Pro · Chrome',
        deviceType: 'desktop',
        location: 'Oslo, Norway',
        ipAddress: '185.220.12.44',
        lastUsed: '2 min ago',
    },
    {
        id: 's2',
        deviceName: 'iPhone 15 · Safari',
        deviceType: 'mobile',
        location: 'Bergen, Norway',
        ipAddress: '51.174.92.20',
        lastUsed: '11 min ago',
    },
    {
        id: 's3',
        deviceName: 'Windows Workstation · Edge',
        deviceType: 'desktop',
        location: 'Stockholm, Sweden',
        ipAddress: '89.132.201.73',
        lastUsed: '34 min ago',
    },
    {
        id: 's4',
        deviceName: 'iPad Air · Safari',
        deviceType: 'tablet',
        location: 'Trondheim, Norway',
        ipAddress: '79.160.18.66',
        lastUsed: '1 hour ago',
    },
    {
        id: 's5',
        deviceName: 'Linux Terminal · Firefox',
        deviceType: 'desktop',
        location: 'Copenhagen, Denmark',
        ipAddress: '93.191.114.52',
        lastUsed: '3 hours ago',
    },
];

const getDeviceIcon = (deviceType: Session['deviceType']) => {
    if (deviceType === 'mobile') return User;
    return Monitor;
};

const buildMockSessions = (): Session[] => {
    const shuffled = [...SAFE_SESSIONS].sort(() => Math.random() - 0.5);
    const selected = shuffled.slice(0, 5);
    const suspiciousIndex = Math.floor(Math.random() * selected.length);

    return selected.map((session, index) => ({
        ...session,
        riskLevel: index === suspiciousIndex ? 'suspicious' : 'safe',
    }));
};

const SessionSecurityGame = () => {
    const [sessions, setSessions] = useState<Session[]>(() => buildMockSessions());
    const [warningMessage, setWarningMessage] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [revokedId, setRevokedId] = useState<string | null>(null);
    const [attemptedWrongId, setAttemptedWrongId] = useState<string | null>(null);
    const [revealSuspicious, setRevealSuspicious] = useState(false);


    const resetGame = () => {
        setSessions(buildMockSessions());
        setWarningMessage('');
        setSuccessMessage('');
        setRevokedId(null);
        setAttemptedWrongId(null);
        setRevealSuspicious(false);
    };

    const handleRevoke = (session: Session) => {
        if (revokedId) return;


        if (session.riskLevel === 'suspicious') {
            setRevokedId(session.id);
            setWarningMessage('');
            setSuccessMessage('Threat Neutralized');
            return;
        }

        setAttemptedWrongId(session.id);
        setRevealSuspicious(true);
        setSuccessMessage('');
        setWarningMessage('Incorrect target. Re-check telemetry and revoke the suspicious session.');

        window.setTimeout(() => setAttemptedWrongId(null), 520);
    };

    return (
        <section className="relative mt-14 overflow-hidden rounded-3xl border border-cyan-300/20 bg-slate-900/40 p-5 backdrop-blur-xl sm:p-8">
            <div className="pointer-events-none absolute inset-0">
                {[...Array(11)].map((_, index) => (
                    <motion.span
                        key={`node-${index}`}
                        className="absolute h-1.5 w-1.5 rounded-full bg-cyan-300/50"
                        style={{
                            left: `${8 + (index * 9) % 84}%`,
                            top: `${14 + (index * 13) % 70}%`,
                        }}
                        animate={{
                            y: [0, -9, 0],
                            opacity: [0.25, 0.8, 0.25],
                        }}
                        transition={{duration: 5 + (index % 3), repeat: Infinity, ease: 'easeInOut'}}
                    />
                ))}
            </div>

            <div className="relative z-10">
                <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
                    <div>
                        <p className="text-xs uppercase tracking-[0.22em] text-cyan-200/80">Interactive Security Drill</p>
                        <h2 className="text-xl font-semibold text-white sm:text-2xl">Find and revoke the suspicious session</h2>
                    </div>
                    {successMessage ? (
                        <span className="rounded-full border border-emerald-300/40 bg-emerald-400/15 px-3 py-1 text-xs font-semibold text-emerald-200">
                            {successMessage}
                        </span>
                    ) : null}
                </div>

                {warningMessage ? (
                    <motion.div
                        initial={{opacity: 0, y: -6}}
                        animate={{opacity: 1, y: 0}}
                        className="mb-5 rounded-xl border border-amber-300/40 bg-amber-300/10 px-4 py-2 text-sm text-amber-100"
                    >
                        {warningMessage}
                    </motion.div>
                ) : null}

                <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                    {sessions.map((session) => {
                        const DeviceIcon = getDeviceIcon(session.deviceType);
                        const isRevoked = revokedId === session.id;
                        const isWrongPick = attemptedWrongId === session.id;
                        const isSuspicious = session.riskLevel === 'suspicious';
                        const reveal = revealSuspicious && isSuspicious && !isRevoked;

                        return (
                            <motion.article
                                key={session.id}
                                layout
                                whileHover={{y: -4, scale: 1.01}}
                                animate={{
                                    opacity: isRevoked ? 0 : 1,
                                    scale: isRevoked ? 0.92 : 1,
                                    x: isWrongPick ? [0, -7, 7, -5, 5, 0] : 0,
                                    borderColor: reveal ? 'rgba(251, 191, 36, 0.7)' : 'rgba(103, 232, 249, 0.20)',
                                    boxShadow: reveal
                                        ? '0 0 0 1px rgba(251,191,36,0.4), 0 0 36px rgba(251,191,36,0.24)'
                                        : '0 0 0 1px rgba(103,232,249,0.1)',
                                }}
                                transition={{duration: 0.35}}
                                className="relative overflow-hidden rounded-2xl border bg-white/[0.05] p-4 backdrop-blur-xl"
                            >
                                <div className="mb-4 flex items-start justify-between gap-3">
                                    <div className="flex items-center gap-3">
                                        <span className="inline-flex h-10 w-10 items-center justify-center rounded-xl border border-cyan-200/30 bg-cyan-300/10 text-cyan-100">
                                            <DeviceIcon size={18} />
                                        </span>
                                        <div>
                                            <p className="text-sm font-semibold text-slate-100">{session.deviceName}</p>
                                            <p className="text-xs text-slate-400">{session.location}</p>
                                        </div>
                                    </div>
                                    <span
                                        className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${
                                            isSuspicious
                                                ? 'border border-rose-300/40 bg-rose-400/15 text-rose-200'
                                                : 'border border-emerald-300/40 bg-emerald-400/15 text-emerald-200'
                                        }`}
                                    >
                                        {isSuspicious ? 'Risk Elevated' : 'Trusted'}
                                    </span>
                                </div>

                                <div className="space-y-1.5 text-sm text-slate-300">
                                    <p><span className="text-slate-400">IP:</span> {session.ipAddress}</p>
                                    <p><span className="text-slate-400">Last Used:</span> {session.lastUsed}</p>
                                </div>

                                <motion.button
                                    whileHover={{scale: 1.02}}
                                    whileTap={{scale: 0.98}}
                                    disabled={Boolean(revokedId)}
                                    onClick={() => handleRevoke(session)}
                                    className="mt-4 inline-flex w-full items-center justify-center rounded-lg border border-cyan-300/35 bg-cyan-400/10 px-4 py-2 text-sm font-semibold text-cyan-100 transition hover:bg-cyan-300/20 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                    Revoke Session
                                </motion.button>
                            </motion.article>
                        );
                    })}
                </div>

                {revokedId ? (
                    <motion.div
                        initial={{opacity: 0, scale: 0.95}}
                        animate={{opacity: 1, scale: 1}}
                        className="relative mt-8 overflow-hidden rounded-2xl border border-emerald-300/35 bg-emerald-400/10 p-6 text-center"
                    >
                        <motion.div
                            aria-hidden
                            className="pointer-events-none absolute inset-0"
                            animate={{
                                background: [
                                    'radial-gradient(circle at 50% 60%, rgba(34,197,94,0.26), transparent 58%)',
                                    'radial-gradient(circle at 50% 55%, rgba(251,146,60,0.35), transparent 56%)',
                                    'radial-gradient(circle at 50% 60%, rgba(34,197,94,0.26), transparent 58%)',
                                ],
                            }}
                            transition={{duration: 1.2, repeat: 2}}
                        />
                        <motion.div
                            aria-hidden
                            className="mx-auto mb-3 h-14 w-14 rounded-full bg-orange-400/20"
                            animate={{scale: [1, 1.24, 1], opacity: [0.5, 0.95, 0.5]}}
                            transition={{duration: 0.8, repeat: 2}}
                        />
                        <div className="relative z-10">
                            <p className="text-lg font-semibold text-emerald-100">Threat Neutralized</p>
                            <p className="mt-1 text-sm text-emerald-50/85">
                                Dragon fire deployed. Suspicious session has been revoked successfully.
                            </p>
                            <motion.button
                                whileHover={{y: -2}}
                                whileTap={{scale: 0.98}}
                                onClick={resetGame}
                                className="mt-4 inline-flex items-center gap-2 rounded-lg bg-emerald-400/20 px-5 py-2 text-sm font-semibold text-emerald-100 transition hover:bg-emerald-400/30"
                            >
                                <Shield size={16} />
                                Play Again
                            </motion.button>
                        </div>
                    </motion.div>
                ) : null}
            </div>
        </section>
    );
};

export default SessionSecurityGame;
