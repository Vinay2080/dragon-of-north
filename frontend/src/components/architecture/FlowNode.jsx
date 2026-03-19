import {createElement} from 'react';

const toneByState = {
    idle: {
        border: 'border-slate-200 dark:border-slate-700/50',
        text: 'text-slate-600 dark:text-slate-400',
        iconBg: 'bg-slate-100 dark:bg-slate-700/30',
        background: 'bg-white dark:bg-slate-800/30',
        glow: 'shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:shadow-[0_1px_2px_rgba(0,0,0,0.2)]',
    },
    active: {
        border: 'border-blue-400 dark:border-blue-500/60',
        text: 'text-blue-600 dark:text-blue-300',
        iconBg: 'bg-blue-100 dark:bg-blue-600/30',
        background: 'bg-blue-50/70 dark:bg-blue-950/40',
        glow: 'shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(59,130,246,0.15)] dark:shadow-[0_1px_2px_rgba(0,0,0,0.2),0_4px_12px_rgba(59,130,246,0.2)]',
    },
    complete: {
        border: 'border-green-300 dark:border-green-600/50',
        text: 'text-green-700 dark:text-green-300',
        iconBg: 'bg-green-100 dark:bg-green-600/30',
        background: 'bg-green-50/70 dark:bg-green-950/40',
        glow: 'shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(34,197,94,0.12)] dark:shadow-[0_1px_2px_rgba(0,0,0,0.2)]',
    },
    error: {
        border: 'border-red-400 dark:border-red-500/60',
        text: 'text-red-600 dark:text-red-300',
        iconBg: 'bg-red-100 dark:bg-red-600/30',
        background: 'bg-red-50/70 dark:bg-red-950/40',
        glow: 'shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(239,68,68,0.15)] dark:shadow-[0_1px_2px_rgba(0,0,0,0.2),0_4px_12px_rgba(239,68,68,0.2)]',
    },
};

const FlowNode = ({
                      icon,
                      label,
                      detail,
                      timing,
                      timingVisible = false,
                      showDetail = false,
                      active = false,
                      completed = false,
                      error = false,
                  }) => {
    const state = error ? 'error' : active ? 'active' : completed ? 'complete' : 'idle';
    const tone = toneByState[state];

    return (
        <div className="relative min-w-[160px] flex-1">
            <div
                className={`group relative rounded-xl border px-4 py-3.5 transition-all duration-300 ease-in-out ${tone.border} ${tone.text} ${tone.background} ${tone.glow} ${active ? 'scale-[1.02]' : 'scale-100'} ${!active && !completed && !error ? 'opacity-65' : 'opacity-100'}`}
            >
                <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2.5">
                        <span className={`flex h-8 w-8 items-center justify-center rounded-lg ${tone.iconBg}`}>
                            {icon ? createElement(icon, {size: 18}) : null}
                        </span>
                        <span className="text-sm font-semibold text-slate-900 dark:text-slate-50">{label}</span>
                    </div>
                    {timing && (
                        <span
                            className={`rounded-md bg-slate-100 px-1.5 py-0.5 text-[10px] font-semibold text-slate-600 dark:bg-slate-700/50 dark:text-slate-300 transition-all duration-300 ease-in-out ${timingVisible ? 'opacity-100' : 'opacity-0'}`}
                        >
                            {timing}
                        </span>
                    )}
                </div>
                <p
                    className={`mt-2 text-xs leading-relaxed text-slate-600 dark:text-slate-400 transition-all duration-300 ease-in-out ${showDetail ? 'translate-y-0 opacity-100' : 'translate-y-0.5 opacity-0 md:group-hover:translate-y-0 md:group-hover:opacity-100'}`}
                    role="status"
                    aria-live="polite"
                >
                    {detail}
                </p>
            </div>
        </div>
    );
};

export default FlowNode;
