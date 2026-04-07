import React from 'react';

/**
 * Icon-only button with consistent hit-area, focus ring, and a tooltip.
 *
 * - Keeps behavior intact: you still pass onClick, type, disabled, etc.
 * - Accessibility: requires an aria-label and supports keyboard focus.
 */
const IconButton = ({
                        label,
                        tooltip,
                        children,
                        className = '',
                        tooltipPlacement = 'bottom',
                        ...props
                    }) => {
    const resolvedTooltip = tooltip || label;

    const placementClasses = {
        bottom: 'top-full mt-2 left-1/2 -translate-x-1/2',
        top: 'bottom-full mb-2 left-1/2 -translate-x-1/2',
        right: 'left-full ml-2 top-1/2 -translate-y-1/2',
        left: 'right-full mr-2 top-1/2 -translate-y-1/2',
    };

    return (
        <span className="group relative inline-flex">
            <button
                type="button"
                aria-label={label}
                className={`inline-flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200/70 bg-white/70 text-slate-700 shadow-sm backdrop-blur transition-all duration-200 hover:-translate-y-0.5 hover:border-teal-200/80 hover:bg-white/80 hover:shadow-[0_16px_30px_rgba(20,184,166,0.14)] focus:outline-none focus-visible:ring-2 focus-visible:ring-teal-400/50 focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed disabled:opacity-55 dark:border-slate-800/70 dark:bg-slate-950/35 dark:text-slate-200 dark:hover:border-teal-500/35 dark:hover:bg-slate-950/45 dark:focus-visible:ring-offset-slate-950 ${className}`}
                {...props}
            >
                {children}
            </button>

            {resolvedTooltip ? (
                <span
                    role="tooltip"
                    className={`pointer-events-none absolute z-40 hidden whitespace-nowrap rounded-xl border border-slate-200/70 bg-white/90 px-3 py-1.5 text-xs font-semibold text-slate-800 shadow-[0_18px_40px_rgba(15,23,42,0.10)] backdrop-blur-sm group-hover:block group-focus-within:block dark:border-slate-700/70 dark:bg-slate-950/85 dark:text-slate-100 ${placementClasses[tooltipPlacement] || placementClasses.bottom}`}
                >
                    {resolvedTooltip}
                </span>
            ) : null}
        </span>
    );
};

export default IconButton;

