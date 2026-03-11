import {NavLink} from 'react-router-dom';

const links = [
    {to: '/', label: 'Home'},
    {to: '/features', label: 'Features'},
    {to: '/architecture', label: 'Architecture'},
    {to: '/security-demo', label: 'Security Demo'},
    {to: '/deployment', label: 'Deployment'},
    {to: '/login', label: 'App Login'},
];

const DocsLayout = ({title, subtitle, children}) => (
    <div className="relative min-h-screen overflow-hidden bg-[#070b14] text-slate-100">
        <div className="dynamic-grid-overlay" aria-hidden />
        <header className="sticky top-0 z-20 border-b border-white/10 bg-[#070b14]/85 backdrop-blur">
            <div className="mx-auto flex w-full max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
                <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-cyan-300">Identity Architecture Lab</p>
                    <h1 className="text-lg font-semibold">{title}</h1>
                </div>
                <nav className="flex flex-wrap gap-2">
                    {links.map((link) => (
                        <NavLink
                            key={link.to}
                            to={link.to}
                            className={({isActive}) => `rounded-lg border px-3 py-1.5 text-xs font-medium transition ${isActive ? 'border-cyan-300/70 bg-cyan-300/10 text-cyan-100' : 'border-white/15 text-slate-300 hover:border-white/35'}`}
                        >
                            {link.label}
                        </NavLink>
                    ))}
                </nav>
            </div>
        </header>
        <main className="relative z-10 mx-auto w-full max-w-7xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">
            <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
                <h2 className="text-3xl font-semibold tracking-tight">{title}</h2>
                <p className="mt-2 max-w-4xl text-slate-300">{subtitle}</p>
            </div>
            {children}
        </main>
    </div>
);

export default DocsLayout;
