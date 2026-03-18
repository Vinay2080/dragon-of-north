# Code Reference: Header & Profile Dropdown Implementation

## 1. PROFILE DROPDOWN COMPONENT (ProfileDropdown.jsx)

### Profile Button with Backdrop Blur

```jsx
<button
    type="button"
    onClick={() => {
        if (isOpen) {
            closeThemeSubmenuNow();
        }
        setIsOpen((v) => !v);
    }}
    className="inline-flex items-center gap-2 rounded-lg border border-gray-200 dark:border-white/10 
               bg-white/60 dark:bg-slate-900/60 px-3 py-2 text-sm text-gray-700 dark:text-gray-300 
               shadow-sm transition-all duration-200 hover:bg-white dark:hover:bg-slate-800/80 
               backdrop-blur-sm"
    aria-haspopup="menu"
    aria-expanded={isOpen}
    aria-label="Open profile menu"
>
    <User size={16}/>
</button>
```

**Key Features:**

- `bg-white/60 dark:bg-slate-900/60` - Semi-transparent backgrounds
- `backdrop-blur-sm` - Frosted glass effect
- `transition-all duration-200` - Smooth 200ms hover transition
- `border-gray-200 dark:border-white/10` - Subtle borders

---

### Main Dropdown with Scale + Fade Animation

```jsx
<ul
    className={`profile-dropdown-menu absolute right-0 mt-2 w-48 rounded-xl 
                 bg-white dark:bg-slate-900 border border-gray-200 dark:border-white/10 
                 overflow-hidden transition-all duration-150 origin-top-right 
                 ${isOpen ? 'scale-100 opacity-100 visible' : 'scale-95 opacity-0 invisible pointer-events-none'}`}
    role="menu"
    aria-label="Profile menu"
>
```

**Animations:**

- `scale-100 / scale-95` - Smooth 5% scale change
- `opacity-100 / opacity-0` - Fade in/out
- `duration-150` - 150ms duration
- `origin-top-right` - Expands from top-right corner
- `pointer-events-none` - Click-through when hidden

---

### Theme Submenu Opening to LEFT (Critical!)

```jsx
<li
    className="submenu-anchor relative"
    onMouseEnter={openThemeSubmenu}
    onMouseLeave={scheduleThemeSubmenuClose}
>
    <button
        ref={themeTriggerRef}
        type="button"
        className={`menu-item w-full text-left px-4 py-2 flex items-center justify-between 
                     hover:bg-gray-100 dark:hover:bg-white/5 
                     ${isThemeSubmenuOpen ? 'menu-item--active bg-gray-100 dark:bg-white/5' : ''}`}
        aria-haspopup="menu"
        aria-expanded={isThemeSubmenuOpen}
        onClick={() => setIsThemeSubmenuOpen((prev) => !prev)}
        onFocus={openThemeSubmenu}
    >
        <span className="text-sm">Theme</span>
        <span className={`arrow text-xs transition-transform duration-200 
                         ${isThemeSubmenuOpen ? 'rotate-90' : ''}`}>›</span>
    </button>

    {/* Hover bridge keeps submenu open on diagonal movement */}
    <div
        className={`submenu-bridge submenu-bridge--left ${isThemeSubmenuOpen ? 'is-active' : ''}`}
        onMouseEnter={openThemeSubmenu}
        onMouseLeave={scheduleThemeSubmenuClose}
        aria-hidden
    />

    {/* Theme Submenu - Opens LEFT */}
    <ul
        className={`submenu-panel submenu-panel--left rounded-xl bg-white dark:bg-slate-900 
                     border border-gray-200 dark:border-white/10 overflow-hidden w-40 
                     ${isThemeSubmenuOpen ? 'is-open' : ''}`}
        role="menu"
        aria-label="Theme selection"
        onMouseEnter={openThemeSubmenu}
        onMouseLeave={scheduleThemeSubmenuClose}
    >
        {THEME_SEQUENCE.map((t) => {
            const Icon = t === 'light' ? Sun : t === 'dark' ? Moon : Monitor;
            return (
                <li key={t} role="none">
                    <button
                        type="button"
                        role="menuitem"
                        onClick={() => {
                            setTheme(t);
                            setIsOpen(false);
                            closeThemeSubmenuNow();
                        }}
                        className="submenu-item px-4 py-2 w-full text-left hover:bg-gray-100 
                                   dark:hover:bg-white/5 flex items-center gap-2 text-sm 
                                   transition-all duration-150"
                    >
                        <Icon size={14}/>
                        <span className="capitalize">{t}</span>
                    </button>
                </li>
            );
        })}
    </ul>
</li>
```

**Key Points:**

- `submenu-panel--left` class positions submenu to LEFT
- `submenu-bridge--left` creates invisible hover zone
- `rotate-90` arrow points toward submenu (left)
- Smooth scale + opacity transitions on open/close

---

### Menu Section Divider

```jsx
{/* Divider */}
<li className="border-t border-gray-200 dark:border-white/10 my-1"/>
```

---

## 2. CSS STYLING (index.css)

### Enhanced Profile Dropdown Menu

```css
.profile-dropdown-menu {
    z-index: 50;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.dark .profile-dropdown-menu {
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}
```

**Why:**

- `z-index: 50` ensures it's above most content
- `box-shadow: 0 12px 32px` is softer than `0 8px 22px`
- Separate dark mode shadow for visibility

---

### Menu Items with Hover States

```css
.menu-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 16px;
    border-radius: 6px;
    background: transparent;
    color: var(--don-text-primary);
    transition: background-color 150ms ease, color 150ms ease;
    cursor: pointer;
    font-size: 0.875rem;
}

.menu-item:hover,
.menu-item--active {
    background: rgba(0, 0, 0, 0.05);
}

.dark .menu-item:hover,
.dark .menu-item--active {
    background: rgba(255, 255, 255, 0.08);
}
```

**Benefits:**

- `rgba(0, 0, 0, 0.05)` light overlay in light mode
- `rgba(255, 255, 255, 0.08)` light overlay in dark mode
- Consistent hover feel across modes
- `150ms` smooth transition

---

### Submenu Bridge (Invisible Hover Zone)

```css
.submenu-bridge {
    position: absolute;
    top: -8px;
    bottom: -8px;
    width: 16px;
    pointer-events: none;
}

.submenu-bridge--left {
    right: 100%;
}

.submenu-bridge.is-active {
    pointer-events: auto;
}
```

**Purpose:**

- Creates invisible "bridge" between main menu and submenu
- Keeps submenu open when cursor moves diagonally
- No pointer events when hidden (`pointer-events: none`)
- Auto pointer events when active (`is-active`)

---

### Submenu Panel with Scale Animation

```css
.submenu-panel {
    position: absolute;
    top: 0;
    min-width: 160px;
    background: var(--don-popover, var(--don-bg-surface));
    border: 1px solid var(--don-border-default);
    border-radius: 12px;
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
    padding: 4px;
    opacity: 0;
    scale: 0.95;
    visibility: hidden;
    pointer-events: none;
    transition: opacity 150ms ease, scale 150ms ease, visibility 0s linear 150ms;
    z-index: 50;
}

.dark .submenu-panel {
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}

.submenu-panel--left {
    right: calc(100% + 12px);
}

.submenu-panel.is-open {
    opacity: 1;
    scale: 1;
    visibility: visible;
    pointer-events: auto;
    transition: opacity 150ms ease, scale 150ms ease;
}
```

**Key Features:**

- `scale: 0.95 → scale: 1` smooth size animation
- `opacity: 0 → opacity: 1` fade in/out
- `right: calc(100% + 12px)` positions to LEFT of parent
- `visibility` for accessibility (proper screen reader support)
- `150ms ease` smooth, not jarring

---

### Sticky Header Enhancement

```css
.dashboard-topbar {
    position: sticky;
    top: 0;
    z-index: 50;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--s3) var(--s4);
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    background: rgba(255, 255, 255, 0.75);
    backdrop-filter: blur(12px);
    -webkit-backdrop-filter: blur(12px);
    transition: all 200ms ease-out;
}

.dark .dashboard-topbar {
    background: rgba(15, 23, 42, 0.75);
    border-bottom-color: rgba(255, 255, 255, 0.08);
}
```

**Why:**

- `position: sticky` keeps it at top
- `backdrop-filter: blur(12px)` frosted glass effect
- `-webkit-backdrop-filter` Safari support
- `rgba(255, 255, 255, 0.75)` semi-transparent white
- `rgba(15, 23, 42, 0.75)` semi-transparent dark slate
- Subtle borders don't overpower

---

## 3. INTERACTION PATTERNS

### Opening Theme Submenu (Hover)

```jsx
onMouseEnter={openThemeSubmenu}
onMouseLeave={scheduleThemeSubmenuClose}
```

**Behavior:**

- Immediate open on hover
- Delayed close (150ms) to allow diagonal movement
- Click also toggles open/close

---

### Keyboard Navigation

```jsx
onKeyDown={(event) => {
    if (event.key === 'ArrowRight') {
        event.preventDefault();
        openThemeSubmenu();
        firstThemeItemRef.current?.focus();
    }
    if (event.key === 'ArrowLeft') {
        event.preventDefault();
        closeThemeSubmenuNow();
    }
}}
```

**Supports:**

- ArrowRight to open submenu
- ArrowLeft to close submenu
- Tab to navigate between items
- Enter/Space to activate

---

## 4. LAYOUT STRUCTURE

```
┌─────────────────────────────────────────────┐
│        Sticky Header (backdrop blur)        │
│  Logo   |  Title       [Auth System] [👤]   │
└─────────────────────────────────────────────┘

                    ┌──────────────┐
                    │  Theme → ┐   │
                    │  ────────┼── │ ← Divider
                    │  Login   │   │
                    └──────────┼───┘
                               │
         ┌─────────────────────┘
         │
    ┌────▼──────────┐
    │ Light (☀️)    │
    │ Dark (🌙)     │
    │ System (💻)   │
    └───────────────┘
    (Opens LEFT, no overlap)
```

---

## 5. QUICK REFERENCE: WHAT CHANGED

| Element          | Before                    | After                              | Benefit                 |
|------------------|---------------------------|------------------------------------|-------------------------|
| Button           | `bg-white/70`             | `bg-white/60` + `backdrop-blur-sm` | Modern, glassy look     |
| Dropdown         | `hidden` class            | `scale-95 opacity-0` transitions   | Smooth animation        |
| Shadow           | `0 8px 22px`              | `0 12px 32px`                      | Softer appearance       |
| Submenu          | `transform: translateX()` | `scale: 0.95`                      | Better visual feedback  |
| Submenu Position | Varies                    | `right: calc(100% + 12px)`         | Consistent LEFT opening |
| Arrow            | `rotate-180`              | `rotate-90`                        | Points to submenu       |
| Header Blur      | `color-mix()`             | `rgba()`                           | More reliable rendering |

---

## 6. BROWSER COMPATIBILITY

```css
/* Modern browsers support these features */
- backdrop-filter: ✅ Chrome 76+, Firefox 103+, Safari 9+
- scale: ✅ All modern browsers (CSS Transforms Level 2)
- rgba colors: ✅ All browsers
- transition: ✅ All browsers

/* Fallback for older browsers */
-webkit-backdrop-filter: blur(12px); /* Safari 9-15 */
```

---

## Testing Notes

### Light Mode

- Button: Clean white with subtle gray border
- Hover: Slight brightening effect
- Dropdown: White background, light gray items on hover

### Dark Mode

- Button: Slate 900 with subtle white border
- Hover: Slate 800 with glow effect
- Dropdown: Slate 900 background, subtle light overlay on hover

### Touch Devices

- Tap button to open dropdown
- Tap outside to close
- No hover bridge (handled by touch logic)

---

## Performance Considerations

1. **GPU Acceleration**: `scale` and `opacity` use GPU (efficient)
2. **No Layout Shift**: Position and size set upfront
3. **Pointer Events**: Disabled when hidden (no event bubbling)
4. **Transitions**: 150ms (feels instant, not processing-heavy)

---

End of Code Reference

