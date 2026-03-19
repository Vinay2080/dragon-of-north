import {Play, RotateCcw, Zap} from 'react-feather';

const methodTone = {
    GET: 'text-blue-600 dark:text-blue-400',
    POST: 'text-green-600 dark:text-green-400',
    DELETE: 'text-red-600 dark:text-red-400',
};

const SimulationController = ({
                                  actionOptions,
                                  selectedAction,
                                  onActionChange,
                                  endpoint,
                                  onPlay,
                                  onMasterPlay,
                                  masterRunning,
                                  onReset,
                                  status,
                                  onStatusChange,
                                  speed,
                                  onSpeedChange,
                                  running,
                              }) => {
    const [method = '', ...pathParts] = endpoint.split(' ');
    const endpointPath = pathParts.join(' ');

    return (
        <div
            className="rounded-xl bg-white p-4 border border-slate-200 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:bg-slate-900/50 dark:border-slate-700/50 dark:shadow-[0_1px_2px_rgba(0,0,0,0.1)]">
            <div
                className="inline-flex flex-wrap items-center gap-1 rounded-full bg-slate-100 p-1 border border-slate-200 dark:bg-slate-800/60 dark:border-slate-700/50">
                {actionOptions.map((action) => (
                    <button
                        key={action.id}
                        type="button"
                        onClick={() => onActionChange(action.id)}
                        className={`rounded-full px-3 py-1.5 text-xs font-medium transition-all duration-300 ease-in-out ${selectedAction === action.id ? 'bg-blue-50 text-blue-700 border border-blue-200 dark:bg-blue-600/20 dark:text-blue-200 dark:border-blue-600/40' : 'text-slate-600 dark:text-slate-400 hover:bg-slate-50 dark:hover:bg-slate-700/40'}`}
                    >
                        {action.label}
                    </button>
                ))}
            </div>

            <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                <button
                    type="button"
                    onClick={onMasterPlay}
                    disabled={masterRunning}
                    className="inline-flex items-center gap-1.5 rounded-full bg-indigo-600 px-3 py-1.5 font-medium text-white transition-all duration-300 ease-in-out hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-blue-600 dark:hover:bg-blue-700"
                >
                    <Zap size={14}/>
                    {masterRunning ? 'Running All...' : 'Play Full Simulation'}
                </button>

                <button
                    type="button"
                    onClick={onPlay}
                    disabled={running}
                    className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 font-medium text-slate-700 border border-slate-200 transition-all duration-300 ease-in-out hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-60 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50"
                >
                    <Play size={14}/>
                    {running ? 'Playing...' : 'Play Request'}
                </button>

                <button
                    type="button"
                    onClick={onReset}
                    className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-3 py-1.5 font-medium text-slate-700 border border-slate-200 transition-all duration-300 ease-in-out hover:bg-slate-200 dark:bg-slate-700/30 dark:text-slate-400 dark:border-slate-600/50 dark:hover:bg-slate-700/50"
                >
                    <RotateCcw size={14}/>
                    Reset
                </button>

                <div
                    className="ml-auto inline-flex items-center overflow-hidden rounded-full bg-slate-100 border border-slate-200 p-0.5 dark:bg-slate-700/40 dark:border-slate-600/50">
                    {['success', 'error'].map((item) => (
                        <button
                            key={item}
                            type="button"
                            onClick={() => onStatusChange(item)}
                            className={`rounded-full px-3 py-1.5 uppercase tracking-wide transition-all duration-300 ease-in-out font-medium text-xs ${status === item ? item === 'success' ? 'bg-green-50 text-green-700 dark:bg-green-600/20 dark:text-green-200' : 'bg-red-50 text-red-700 dark:bg-red-600/20 dark:text-red-200' : 'text-slate-600 dark:text-slate-400'}`}
                        >
                            {item}
                        </button>
                    ))}
                </div>

                <div
                    className="inline-flex items-center overflow-hidden rounded-full bg-slate-100 border border-slate-200 p-0.5 dark:bg-slate-700/40 dark:border-slate-600/50">
                    {['slow', 'normal', 'fast'].map((item) => (
                        <button
                            key={item}
                            type="button"
                            onClick={() => onSpeedChange(item)}
                            className={`rounded-full px-3 py-1.5 uppercase tracking-wide transition-all duration-300 ease-in-out font-medium text-xs ${speed === item ? 'bg-indigo-50 text-indigo-700 dark:bg-blue-600/20 dark:text-blue-200' : 'text-slate-600 dark:text-slate-400'}`}
                        >
                            {item}
                        </button>
                    ))}
                </div>
            </div>

            <div
                className="mt-4 rounded-lg bg-slate-50 px-3 py-2 text-xs font-medium text-slate-700 border border-slate-200 dark:bg-slate-800/50 dark:text-slate-300 dark:border-slate-700/50">
                API Endpoint:{' '}
                <span key={endpoint}
                      className="inline-flex items-center gap-2 font-mono transition-all duration-300 ease-in-out">
                    <span className={methodTone[method] ?? 'text-slate-600 dark:text-slate-300'}>{method}</span>
                    <span>{endpointPath}</span>
                </span>
            </div>
        </div>
    );
};

export default SimulationController;

