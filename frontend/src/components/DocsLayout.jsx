const DocsLayout = ({title, subtitle, children}) => {
    return (
        <div className="relative min-h-0">
            <div className="rounded-2xl border border-white/10 bg-white/[0.03] p-6">
                <h2 className="text-3xl font-semibold tracking-tight">{title}</h2>
                <p className="mt-2 max-w-4xl text-slate-300">{subtitle}</p>
            </div>
            <div className="mt-8 space-y-8">
                {children}
            </div>
        </div>
    );
};

export default DocsLayout;
