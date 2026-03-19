const travelDurationBySpeed = {
    slow: '1.65s',
    normal: '1.1s',
    fast: '0.75s',
};

const FlowConnector = ({
                           active = false,
                           completed = false,
                           error = false,
                           speed = 'fast',
                           traveler = 'pulse',
                       }) => {
    // State-based colors: active (blue), completed (green), error (red)
    const toneClass = error
        ? 'bg-red-400/80'
        : completed
            ? 'bg-green-400/70'
            : active
                ? 'bg-blue-400/80'
                : 'bg-transparent';

    const pulseClass = error
        ? 'bg-red-400 shadow-[0_1px_6px_rgba(239,68,68,0.5)]'
        : completed
            ? 'bg-green-400 shadow-[0_1px_6px_rgba(34,197,94,0.5)]'
            : 'bg-blue-400 shadow-[0_1px_6px_rgba(59,130,246,0.5)]';

    return (
        <div
            className="relative h-8 w-12 flex-shrink-0"
            aria-hidden="true"
        >
            {/* Background line - always faint/invisible */}
            <div className="absolute inset-0 flex items-center justify-center">
                <div
                    className="h-[1.5px] w-full rounded-full bg-slate-200/30 dark:bg-slate-700/20 transition-opacity duration-300"/>
            </div>

            {/* Active/completed line - only visible when active or completed */}
            <div className="absolute inset-0 flex items-center justify-center overflow-hidden">
                <div
                    className={`h-[1.5px] rounded-full transition-all duration-300 ease-in-out ${active || completed || error ? 'w-full opacity-100' : 'w-0 opacity-0'} ${toneClass}`}
                />
            </div>

            {/* Animated traveler (pulse or token) */}
            {(active || completed || error) && (
                traveler === 'token'
                    ? (
                        <span
                            className={`absolute architecture-connector-horizontal rounded-md border px-1 py-0.5 text-[8px] font-bold uppercase tracking-wider ${error ? 'border-red-400/60 bg-red-100/30 text-red-600 dark:border-red-500/40 dark:bg-red-950/40 dark:text-red-300' : completed ? 'border-green-400/60 bg-green-100/30 text-green-600 dark:border-green-500/40 dark:bg-green-950/40 dark:text-green-300' : 'border-blue-400/60 bg-blue-100/30 text-blue-600 dark:border-blue-500/40 dark:bg-blue-950/40 dark:text-blue-300'}`}
                            style={{'--don-travel-duration': travelDurationBySpeed[speed] ?? travelDurationBySpeed.fast}}
                        >
                            token
                        </span>
                    )
                    : (
                        <span
                            className={`absolute h-2.5 w-2.5 rounded-full architecture-connector-horizontal ${pulseClass}`}
                            style={{'--don-travel-duration': travelDurationBySpeed[speed] ?? travelDurationBySpeed.fast}}
                        />
                    )
            )}
        </div>
    );
};

export default FlowConnector;


