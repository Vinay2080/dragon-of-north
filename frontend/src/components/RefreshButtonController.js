/**
 * Refresh Button Component Utility
 *
 * Production-ready utilities for managing refresh button animations
 * and interactions with proper error handling and accessibility.
 *
 * Usage:
 *   import { RefreshButtonController } from '@/components/RefreshButtonController';
 *   const controller = new RefreshButtonController(buttonElement);
 *   await controller.spin(asyncFunction);
 */

/**
 * RefreshButtonController - Manages refresh button state and animations
 *
 * @class
 * @example
 * const btn = document.querySelector('.scc-refresh-btn');
 * const controller = new RefreshButtonController(btn);
 *
 * // Manual control
 * await controller.startSpinning();
 * // ... do something ...
 * await controller.stopSpinning();
 *
 * // Or let it handle the async operation
 * await controller.spin(async () => {
 *   const data = await fetchSessions();
 *   return data;
 * });
 */
export class RefreshButtonController {
    /**
     * Create a new RefreshButtonController
     * @param {HTMLButtonElement} buttonElement - The refresh button element
     * @param {Object} options - Configuration options
     * @param {number} options.minSpinTime - Minimum spin duration in ms (default: 500)
     * @param {boolean} options.showFeedback - Show visual feedback on completion (default: true)
     * @param {Function} options.onSpinStart - Callback when spinning starts
     * @param {Function} options.onSpinEnd - Callback when spinning ends
     */
    constructor(buttonElement, options = {}) {
        // Validate input
        if (!(buttonElement instanceof HTMLElement)) {
            throw new Error('RefreshButtonController: Must pass a valid HTML element');
        }

        this.button = buttonElement;
        this.isSpinning = false;
        this.minSpinTime = options.minSpinTime ?? 500;
        this.showFeedback = options.showFeedback ?? true;
        this.onSpinStart = options.onSpinStart ?? null;
        this.onSpinEnd = options.onSpinEnd ?? null;

        // Setup event listeners
        this._setupListeners();
    }

    /**
     * Setup internal event listeners
     * @private
     */
    _setupListeners() {
        // Prevent rapid successive clicks
        this.button.addEventListener('click', (e) => {
            if (this.isSpinning) {
                e.preventDefault();
                e.stopPropagation();
            }
        });

        // Keyboard accessibility
        this.button.addEventListener('keydown', (e) => {
            if (e.key === ' ' || e.key === 'Enter') {
                if (this.isSpinning) {
                    e.preventDefault();
                }
            }
        });
    }

    /**
     * Start the spinning animation
     * @returns {Promise<void>}
     */
    async startSpinning() {
        if (this.isSpinning) return;

        this.isSpinning = true;
        this.button.disabled = true;
        this.button.classList.add('spinning');
        this.button.setAttribute('aria-busy', 'true');

        // Call user callback
        if (this.onSpinStart && typeof this.onSpinStart === 'function') {
            try {
                await this.onSpinStart();
            } catch (error) {
                console.error('RefreshButtonController: onSpinStart callback error:', error);
            }
        }
    }

    /**
     * Stop the spinning animation with optional delay
     * @param {number} delay - Delay before stopping in ms (default: 0)
     * @returns {Promise<void>}
     */
    async stopSpinning(delay = 0) {
        if (!this.isSpinning) return;

        // Wait for specified delay if provided
        if (delay > 0) {
            await new Promise(resolve => setTimeout(resolve, delay));
        }

        this.isSpinning = false;
        this.button.classList.remove('spinning');
        this.button.disabled = false;
        this.button.setAttribute('aria-busy', 'false');

        // Call user callback
        if (this.onSpinEnd && typeof this.onSpinEnd === 'function') {
            try {
                await this.onSpinEnd();
            } catch (error) {
                console.error('RefreshButtonController: onSpinEnd callback error:', error);
            }
        }
    }

    /**
     * Execute an async operation with automatic spinning
     * Ensures minimum spin time for better UX
     *
     * @param {Function} asyncFn - Async function to execute
     * @returns {Promise<any>} - Result from asyncFn
     * @throws {Error} - If asyncFn throws
     *
     * @example
     * try {
     *   const result = await controller.spin(async () => {
     *     return await fetch('/api/sessions').then(r => r.json());
     *   });
     * } catch (error) {
     *   console.error('Operation failed:', error);
     * }
     */
    async spin(asyncFn) {
        if (typeof asyncFn !== 'function') {
            throw new Error('RefreshButtonController.spin: Must pass a function');
        }

        const startTime = Date.now();

        try {
            // Start spinning
            await this.startSpinning();

            // Execute the async operation
            const result = await asyncFn();

            // Ensure minimum spin time has passed
            const elapsedTime = Date.now() - startTime;
            const remainingTime = Math.max(0, this.minSpinTime - elapsedTime);

            // Stop spinning with any remaining time
            await this.stopSpinning(remainingTime);

            return result;
        } catch (error) {
            // Stop spinning on error
            await this.stopSpinning();
            throw error;
        }
    }

    /**
     * Reset button to initial state
     * Useful for cleanup or error recovery
     */
    reset() {
        this.isSpinning = false;
        this.button.disabled = false;
        this.button.classList.remove('spinning');
        this.button.setAttribute('aria-busy', 'false');
    }

    /**
     * Enable/disable the button
     * @param {boolean} enabled - Whether the button should be enabled
     */
    setEnabled(enabled) {
        this.button.disabled = !enabled;
    }

    /**
     * Check if button is currently spinning
     * @returns {boolean}
     */
    getIsSpinning() {
        return this.isSpinning;
    }

    /**
     * Destroy the controller and cleanup
     */
    destroy() {
        this.isSpinning = false;
        this.onSpinStart = null;
        this.onSpinEnd = null;
    }
}

/**
 * Batch Refresh Controller - Manages multiple refresh buttons with coordinated timing
 *
 * @class
 * @example
 * const buttons = document.querySelectorAll('.scc-refresh-btn');
 * const batch = new BatchRefreshController(buttons);
 *
 * await batch.spinAll(async () => {
 *   const [sessions, devices] = await Promise.all([
 *     fetchSessions(),
 *     fetchDevices()
 *   ]);
 *   return { sessions, devices };
 * });
 */
export class BatchRefreshController {
    /**
     * Create a new BatchRefreshController
     * @param {HTMLButtonElement[]|NodeList} buttonElements - Array of button elements
     * @param {Object} options - Configuration options (passed to RefreshButtonController)
     */
    constructor(buttonElements, options = {}) {
        this.controllers = Array.from(buttonElements).map(
            btn => new RefreshButtonController(btn, options)
        );
    }

    /**
     * Start spinning all buttons
     * @returns {Promise<void>}
     */
    async spinAll() {
        await Promise.all(this.controllers.map(ctrl => ctrl.startSpinning()));
    }

    /**
     * Stop spinning all buttons
     * @param {number} delay - Delay before stopping in ms
     * @returns {Promise<void>}
     */
    async stopAll(delay = 0) {
        await Promise.all(this.controllers.map(ctrl => ctrl.stopSpinning(delay)));
    }

    /**
     * Execute async operation with all buttons spinning
     * @param {Function} asyncFn - Async function to execute
     * @returns {Promise<any>}
     */
    async spin(asyncFn) {
        const startTime = Date.now();

        try {
            await this.spinAll();
            const result = await asyncFn();

            const elapsedTime = Date.now() - startTime;
            const minTime = Math.max(...this.controllers.map(c => c.minSpinTime));
            const remainingTime = Math.max(0, minTime - elapsedTime);

            await this.stopAll(remainingTime);
            return result;
        } catch (error) {
            await this.stopAll();
            throw error;
        }
    }

    /**
     * Reset all controllers
     */
    resetAll() {
        this.controllers.forEach(ctrl => ctrl.reset());
    }

    /**
     * Enable/disable all buttons
     * @param {boolean} enabled
     */
    setAllEnabled(enabled) {
        this.controllers.forEach(ctrl => ctrl.setEnabled(enabled));
    }

    /**
     * Destroy all controllers
     */
    destroyAll() {
        this.controllers.forEach(ctrl => ctrl.destroy());
        this.controllers = [];
    }
}

/**
 * React Hook for Refresh Button (Optional - for React projects)
 *
 * @example
 * import { useRefreshButton } from '@/components/RefreshButtonController';
 *
 * function MyComponent() {
 *   const buttonRef = useRef();
 *   const { spin, isSpinning } = useRefreshButton(buttonRef);
 *
 *   const handleRefresh = async () => {
 *     await spin(async () => {
 *       await fetchData();
 *     });
 *   };
 *
 *   return (
 *     <button ref={buttonRef} onClick={handleRefresh}>
 *       <svg>...</svg>
 *     </button>
 *   );
 * }
 */
export function useRefreshButton(ref, options = {}) {
    const controllerRef = React.useRef(null);
    const [isSpinning, setIsSpinning] = React.useState(false);

    React.useEffect(() => {
        if (ref.current && !controllerRef.current) {
            const controller = new RefreshButtonController(ref.current, {
                ...options,
                onSpinStart: () => {
                    setIsSpinning(true);
                    options.onSpinStart?.();
                },
                onSpinEnd: () => {
                    setIsSpinning(false);
                    options.onSpinEnd?.();
                }
            });

            controllerRef.current = controller;

            return () => {
                controller.destroy();
                controllerRef.current = null;
            };
        }
    }, [ref, options]);

    const spin = React.useCallback(
        (asyncFn) => controllerRef.current?.spin(asyncFn),
        []
    );

    return { spin, isSpinning };
}

/**
 * Higher Order Component for Refresh Button (Optional - for React projects)
 *
 * @example
 * const RefreshableComponent = withRefreshButton(MyComponent, {
 *   minSpinTime: 1000,
 *   onRefresh: async () => await fetchData()
 * });
 */
export function withRefreshButton(Component, options = {}) {
    return (props) => {
        const buttonRef = React.useRef();
        const { spin, isSpinning } = useRefreshButton(buttonRef, options);

        return (
            <>
                <button ref={buttonRef} className="scc-refresh-btn">
                    <svg className="scc-refresh-btn__icon" {...props.svgProps} />
                </button>
                <Component {...props} spin={spin} isSpinning={isSpinning} />
            </>
        );
    };
}

export default RefreshButtonController;

