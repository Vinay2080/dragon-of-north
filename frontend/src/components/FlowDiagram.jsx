import {useEffect, useState} from 'react';

export const AnimatedFlow = ({steps}) => {
    const [active, setActive] = useState(0);

    useEffect(() => {
        const timer = setInterval(() => {
            setActive((prev) => (prev + 1) % steps.length);
        }, 1400);
        return () => clearInterval(timer);
    }, [steps.length]);

    return (
        <div className="rounded-xl border border-cyan-400/20 bg-[#0b1220] p-4">
            <div className="flex flex-wrap items-center gap-2">
                {steps.map((step, index) => (
                    <div key={step} className="flex items-center gap-2">
                        <div className={`rounded-lg border px-3 py-2 text-xs transition-all duration-500 ${index === active ? 'border-cyan-300 bg-cyan-300/15 text-cyan-100 shadow-[0_0_16px_rgba(103,232,249,0.35)]' : 'border-white/15 text-slate-300'}`}>
                            {step}
                        </div>
                        {index < steps.length - 1 && <span className="text-slate-500">→</span>}
                    </div>
                ))}
            </div>
        </div>
    );
};

export const VerticalFlow = ({steps}) => (
    <div className="space-y-2 rounded-xl border border-violet-400/20 bg-[#100f22] p-4">
        {steps.map((step, index) => (
            <div key={step} className="flex items-center gap-2 text-sm">
                <span className="inline-flex h-6 w-6 items-center justify-center rounded-full border border-violet-300/40 bg-violet-300/10 text-xs text-violet-100">{index + 1}</span>
                <span>{step}</span>
            </div>
        ))}
    </div>
);
