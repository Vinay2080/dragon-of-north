import {useEffect, useState} from 'react';
import DocsLayout from '../components/DocsLayout';

const events = [
    {title: 'User logs in', detail: 'Credentials are validated and a secure session starts.'},
    {title: 'Access token issued (15 minutes)', detail: 'Short TTL minimizes attacker dwell time.'},
    {title: 'Attacker steals cookie', detail: 'A browser compromise leaks existing token material.'},
    {title: 'Access token expires', detail: 'Stolen token becomes unusable after a brief window.'},
    {title: 'Refresh token request detected', detail: 'Rotation mismatch flags suspicious replay behavior.'},
    {title: 'Server revokes session', detail: 'Token family is invalidated and user is forced to re-authenticate.'},
];

const SecurityDemoPage = () => {
    const [active, setActive] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => setActive((prev) => (prev + 1) % events.length), 1600);
        return () => clearInterval(timer);
    }, []);

    return (
        <DocsLayout title="Security Demo" subtitle="Visual timeline of a token theft scenario to show why expiration + rotation + revocation are mandatory.">
            <section className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
                <h3 className="mb-5 text-xl font-semibold">Token Theft Timeline</h3>
                <div className="space-y-3">
                    {events.map((event, index) => (
                        <div key={event.title} className={`flex items-start gap-3 rounded-xl border p-4 transition-all duration-500 ${index === active ? 'border-amber-300/70 bg-amber-300/10 shadow-[0_0_20px_rgba(251,191,36,0.25)]' : 'border-white/10 bg-black/10'}`}>
                            <div className={`mt-1 h-3 w-3 rounded-full ${index <= active ? 'bg-amber-300' : 'bg-slate-600'}`} />
                            <div>
                                <p className="font-medium">{event.title}</p>
                                <p className="text-sm text-slate-300">{event.detail}</p>
                            </div>
                        </div>
                    ))}
                </div>
            </section>
        </DocsLayout>
    );
};

export default SecurityDemoPage;
