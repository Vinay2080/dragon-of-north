import {AlertCircle, CheckCircle, Loader} from 'react-feather';

/**
 * AuthStateComponents - Reusable UI component library for auth state display
 * These components are exported for use throughout the application
 */

/**
 * AuthLoadingOverlay - Full-screen loader with blur
 */
export function AuthLoadingOverlay({isVisible, message = 'Authenticating...'}) {
    return (
        <div
            className={`
                fixed inset-0 z-50
                flex items-center justify-center
                bg-black/20 dark:bg-black/40
                backdrop-blur-sm
                transition-all duration-200
                ${isVisible ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'}
            `}
        >
            <div
                className="bg-white dark:bg-slate-800 px-6 py-4 rounded-xl border border-gray-200 dark:border-white/10 text-sm text-center">
                <div className="flex flex-col items-center gap-3">
                    <Loader size={20} className="animate-spin text-violet-600 dark:text-violet-400"/>
                    <span className="text-gray-700 dark:text-gray-300">{message}</span>
                </div>
            </div>
        </div>
    );
}

/**
 * AuthSuccessMessage - Success state with action button
 */
export function AuthSuccessMessage({message, actionLabel, onAction, actionLink}) {
    return (
        <div className="flex flex-col items-center gap-4 p-6 text-center">
            <div className="flex justify-center">
                <div className="rounded-full bg-green-100 dark:bg-green-500/20 p-3">
                    <CheckCircle size={24} className="text-green-600 dark:text-green-400"/>
                </div>
            </div>
            <div>
                <h3 className="font-semibold text-gray-900 dark:text-white">{message}</h3>
            </div>
            {actionLabel && (
                actionLink ? (
                    <a
                        href={actionLink}
                        className="w-full px-4 py-2.5 rounded-lg bg-violet-600 hover:bg-violet-700 text-white font-medium transition-all duration-200"
                    >
                        {actionLabel}
                    </a>
                ) : (
                    <button
                        onClick={onAction}
                        className="w-full px-4 py-2.5 rounded-lg bg-violet-600 hover:bg-violet-700 text-white font-medium transition-all duration-200"
                    >
                        {actionLabel}
                    </button>
                )
            )}
        </div>
    );
}

/**
 * AuthErrorMessage - Error state with retry option
 */
export function AuthErrorMessage({message, onRetry, onDismiss}) {
    return (
        <div className="animate-in fade-in duration-200">
            <div
                className="flex gap-3 p-3 rounded-lg bg-red-50 dark:bg-red-500/10 border border-red-200 dark:border-red-500/20">
                <div className="flex-shrink-0 mt-0.5">
                    <AlertCircle size={18} className="text-red-600 dark:text-red-400"/>
                </div>
                <div className="flex-1 min-w-0">
                    <p className="text-sm text-red-700 dark:text-red-300">{message}</p>
                </div>
            </div>
            {onRetry && (
                <div className="mt-3 flex gap-2">
                    <button
                        onClick={onRetry}
                        className="flex-1 px-3 py-2 rounded-lg bg-violet-600 hover:bg-violet-700 text-white text-sm font-medium transition-all duration-200"
                    >
                        Retry
                    </button>
                    {onDismiss && (
                        <button
                            onClick={onDismiss}
                            className="flex-1 px-3 py-2 rounded-lg border border-gray-200 dark:border-white/10 text-gray-700 dark:text-gray-300 text-sm font-medium hover:bg-gray-50 dark:hover:bg-white/5 transition-all duration-200"
                        >
                            Dismiss
                        </button>
                    )}
                </div>
            )}
        </div>
    );
}

/**
 * AuthButtonState - Button with loading/disabled state
 */
export function AuthButtonState({
                                    isLoading,
                                    isDisabled,
                                    children,
                                    loadingText = 'Loading...',
                                    ...props
                                }) {
    return (
        <button
            disabled={isLoading || isDisabled}
            className={`
                w-full py-3 px-4 rounded-lg font-medium
                transition-all duration-200
                flex items-center justify-center gap-2
                ${isLoading || isDisabled
                ? 'bg-violet-400 dark:bg-violet-600/50 text-white cursor-not-allowed opacity-70'
                : 'bg-violet-600 hover:bg-violet-700 dark:hover:bg-violet-500 text-white hover:scale-[1.02] active:scale-95'
            }
            `}
            {...props}
        >
            {isLoading ? (
                <>
                    <Loader size={18} className="animate-spin"/>
                    <span>{loadingText}</span>
                </>
            ) : (
                children
            )}
        </button>
    );
}

/**
 * AuthInputField - Input with disabled state
 */
export function AuthInputField({
                                   disabled,
                                   ...props
                               }) {
    return (
        <input
            disabled={disabled}
            className={`
                w-full px-4 py-3 rounded-lg border
                transition-all duration-200
                ${disabled
                ? 'bg-gray-100 dark:bg-slate-700/50 border-gray-200 dark:border-white/5 text-gray-500 dark:text-gray-400 cursor-not-allowed'
                : 'bg-white dark:bg-slate-800 border-gray-200 dark:border-white/10 text-gray-900 dark:text-white focus:border-violet-400 focus:ring-2 focus:ring-violet-400/10'
            }
            `}
            {...props}
        />
    );
}

