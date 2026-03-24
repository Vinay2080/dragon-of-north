/**
 * REFRESH BUTTON COMPONENT - QUICK REFERENCE
 *
 * This is a quick reference guide for the production-ready refresh button component.
 * For detailed documentation, see: REFRESH_BUTTON_DOCS.md
 */

// ═══════════════════════════════════════════════════════════════════════════════
// 1. HTML STRUCTURE
// ═══════════════════════════════════════════════════════════════════════════════

/*
<button
    type="button"
    class="scc-refresh-btn"
    id="refreshBtn"
    aria-label="Refresh sessions"
    title="Refresh sessions"
>
    <svg
        class="scc-refresh-btn__icon"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
    >
        <polyline points="23 4 23 10 17 10"></polyline>
        <polyline points="1 20 1 14 7 14"></polyline>
        <path d="M3.51 9a9 9 0 0 1 14.85-3.36M20.49 15a9 9 0 0 1-14.85 3.36"></path>
    </svg>
</button>
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 2. CSS CLASSES
// ═══════════════════════════════════════════════════════════════════════════════

/*
.scc-refresh-btn                           - Main button container
.scc-refresh-btn__icon                     - SVG icon inside button
.scc-refresh-btn.spinning                  - Add to button to start animation
.scc-refresh-btn:hover:not(:disabled)      - Hover state (auto)
.scc-refresh-btn:focus-visible             - Keyboard focus (auto)
.scc-refresh-btn:active:not(:disabled)     - Click/press state (auto)
.scc-refresh-btn:disabled                  - Disabled state (auto)

.dark .scc-refresh-btn                     - Dark mode (auto with .dark container)
.light .scc-refresh-btn                    - Light mode (auto with .light container)
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 3. CSS VARIABLES (Customization)
// ═══════════════════════════════════════════════════════════════════════════════

/*
--don-bg-card              - Button background color
--don-border-default       - Button border color
--don-text-primary         - Button text/icon color
--don-accent               - Hover accent color (purple)
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 4. BASIC VANILLA JAVASCRIPT USAGE
// ═══════════════════════════════════════════════════════════════════════════════

// Simple pattern:
const button = document.querySelector('.scc-refresh-btn');

button.addEventListener('click', async () => {
    if (button.classList.contains('spinning')) return; // Prevent duplicate clicks

    button.classList.add('spinning');
    button.disabled = true;

    try {
        // Your async operation here
        const response = await fetch('/api/sessions');
        const data = await response.json();
        console.log('Data refreshed:', data);
    } catch (error) {
        console.error('Refresh failed:', error);
    } finally {
        button.classList.remove('spinning');
        button.disabled = false;
    }
});

// ═══════════════════════════════════════════════════════════════════════════════
// 5. REACT USAGE (Your Project)
// ═══════════════════════════════════════════════════════════════════════════════

/*
// In SessionsPage.jsx (already updated):

const [refreshSpinning, setRefreshSpinning] = useState(false);

const loadSessions = async (forceRefresh) => {
    setRefreshSpinning(true);
    try {
        // Your API call
        const sessions = await fetchSessions(forceRefresh);
        // Update state
    } catch (error) {
        console.error('Failed to load sessions:', error);
    } finally {
        setRefreshSpinning(false);
    }
};

<button
    className={`scc-refresh-btn ${refreshSpinning ? 'spinning' : ''}`}
    onClick={() => loadSessions(true)}
    disabled={refreshSpinning}
    aria-label="Refresh sessions"
>
    <svg class="scc-refresh-btn__icon" ... />
</button>
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 6. ADVANCED USAGE WITH CONTROLLER
// ═══════════════════════════════════════════════════════════════════════════════

/*
import { RefreshButtonController } from '@/components/RefreshButtonController';

// Create instance
const button = document.querySelector('.scc-refresh-btn');
const controller = new RefreshButtonController(button, {
    minSpinTime: 500,        // Don't spin for less than 500ms
    onSpinStart: () => {
        console.log('Loading started');
    },
    onSpinEnd: () => {
        console.log('Loading finished');
    }
});

// Use it
button.addEventListener('click', async () => {
    try {
        const result = await controller.spin(async () => {
            // Your async operation
            return await fetchSessions();
        });
        console.log('Result:', result);
    } catch (error) {
        console.error('Error:', error);
    }
});
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 7. STATE MANAGEMENT
// ═══════════════════════════════════════════════════════════════════════════════

/*
Add 'spinning' class       → Icon rotates 360° in 1 second, loops
Disable button             → Opacity 0.5, no pointer events
Add 'spinning' + disable   → Stops spinning on disable
Remove 'spinning' class    → Animation stops smoothly
Set aria-busy="true"       → Tells screen readers it's loading
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 8. KEYBOARD ACCESSIBILITY
// ═══════════════════════════════════════════════════════════════════════════════

/*
TAB                   → Focus the button
SPACE                 → Click the button (auto handled by <button>)
ENTER                 → Click the button (auto handled by <button>)
ESC                   → Blur the button (browser default)

Focus indicator       → Purple outline (focus-visible)
Disabled state        → Visual feedback + keyboard ignored
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 9. DARK MODE USAGE
// ═══════════════════════════════════════════════════════════════════════════════

/*
Light Mode
──────────
background: #ffffff
border: rgba(0, 0, 0, 0.1)
color: #1f2937

Hover
border: #8b5cf6
background: rgba(139, 92, 246, 0.1)


Dark Mode
─────────
background: rgba(255, 255, 255, 0.05)
border: rgba(255, 255, 255, 0.1)
color: rgba(255, 255, 255, 0.85)

Hover
border: rgba(139, 92, 246, 0.5)
background: rgba(139, 92, 246, 0.15)
color: #d8b4fe
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 10. ANIMATION DETAILS
// ═══════════════════════════════════════════════════════════════════════════════

/*
Animation Name:    sccRefreshSpinSmooth
Duration:          1000ms
Timing Function:   linear (constant speed rotation)
Iteration Count:   infinite (repeats)
Property:          transform: rotate(0deg → 360deg)

Button Transitions:
Duration:          200ms
Easing:            cubic-bezier(0.34, 1.56, 0.64, 1)
Properties:        all (background, border, transform, etc.)

Hover Transform:   scale(1.1) → 10% bigger
Active Transform:  scale(0.95) → 5% smaller
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 11. TROUBLESHOOTING CHECKLIST
// ═══════════════════════════════════════════════════════════════════════════════

/*
✓ Icon not visible?
  - Check SVG viewBox="0 0 24 24"
  - Verify stroke="currentColor"
  - Check .scc-refresh-btn__icon width/height in CSS
  - Inspect element in DevTools

✓ Animation not spinning?
  - Verify 'spinning' class is added to button
  - Check @keyframes sccRefreshSpinSmooth is in CSS
  - Look for CSS errors in console
  - Ensure browser supports CSS animations

✓ Dark mode not working?
  - Check .dark class on parent container
  - Verify CSS variables are defined
  - Inspect .dark .scc-refresh-btn styles
  - Test with forced dark mode preference

✓ Button unresponsive?
  - Check :disabled isn't permanently set
  - Verify onClick handler is attached
  - Check for JavaScript errors in console
  - Ensure z-index isn't hidden by other elements

✓ Accessibility issues?
  - Test with Tab key navigation
  - Use screen reader (NVDA, JAWS)
  - Verify focus-visible outline is visible
  - Check aria-label and aria-hidden attributes
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 12. FILES REFERENCE
// ═══════════════════════════════════════════════════════════════════════════════

/*
Frontend Structure:
──────────────────

src/
├── pages/
│   └── SessionsPage.jsx              (UPDATED - button with SVG)
├── index.css                         (UPDATED - complete CSS)
└── components/
    ├── RefreshButton.demo.html       (NEW - standalone demo)
    ├── RefreshButtonController.js    (NEW - advanced utilities)
    └── REFRESH_BUTTON_DOCS.md        (NEW - full documentation)
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 13. PERFORMANCE TIPS
// ═══════════════════════════════════════════════════════════════════════════════

/*
✓ Use GPU acceleration (transform: rotate is already used)
✓ Avoid layout thrashing (use classes, not style attribute)
✓ Minimal DOM queries (cache button reference)
✓ Debounce rapid clicks (check 'spinning' class)
✓ Use CSS animations (hardware accelerated)
✓ Avoid JavaScript animation (CSS is faster)
✓ Use will-change: transform (hints to browser)
*/

// ═══════════════════════════════════════════════════════════════════════════════
// 14. QUICK COPY-PASTE EXAMPLES
// ═══════════════════════════════════════════════════════════════════════════════

// Example 1: Simple fetch with button
const refreshButton = document.querySelector('.scc-refresh-btn');
refreshButton.addEventListener('click', async () => {
    refreshButton.classList.add('spinning');
    refreshButton.disabled = true;

    try {
        const data = await fetch('/api/data').then(r => r.json());
        console.log('Refreshed!', data);
    } finally {
        refreshButton.classList.remove('spinning');
        refreshButton.disabled = false;
    }
});

// Example 2: Multiple operations
async function refreshAllData() {
    const button = document.querySelector('.scc-refresh-btn');
    button.classList.add('spinning');

    try {
        const [sessions, devices] = await Promise.all([
            fetch('/api/sessions').then(r => r.json()),
            fetch('/api/devices').then(r => r.json())
        ]);
        return { sessions, devices };
    } finally {
        button.classList.remove('spinning');
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// THAT'S IT! 🎉
// ═══════════════════════════════════════════════════════════════════════════════

/*
Your refresh button is now fully functional with:
✅ Visible inline SVG icon
✅ Smooth 360° rotation animation
✅ Dark & Light mode support
✅ Full accessibility
✅ Production-ready code
✅ No external dependencies

For more details, see: REFRESH_BUTTON_DOCS.md
For live demo, open: RefreshButton.demo.html
*/

