# Light Mode Color & Style Reference Guide

## Quick Color Palette

### Neutral Colors (Primary)

```
White:           #ffffff        bg-white
Light Gray:      #f8fafc        bg-slate-50
Gray:            #f1f5f9        bg-slate-100
Border:          #e2e8f0        border-slate-200
Dark Gray:       #94a3b8        text-slate-400
Dark:            #0f172a        text-slate-900
```

### Accent Color (Single)

```
Primary:         #4f46e5        bg-indigo-600, hover:bg-indigo-700
Light:           #e0e7ff        bg-indigo-50
Border:          #a5b4fc        border-indigo-300
Text:            #4338ca        text-indigo-700
```

### Status Colors

```
Success:
  Background:    #dcfce7        bg-green-50
  Border:        #86efac        border-green-300
  Text:          #166534        text-green-700

Error:
  Background:    #fee2e2        bg-red-50
  Border:        #fca5a5        border-red-300
  Text:          #991b1b        text-red-700
```

---

## Shadow System

### Standard Card Shadow

```css
shadow-[

0
_1px_2px_rgba
(
0
,
0
,
0
,
0.05
)
,
0
_4px_12px_rgba
(
0
,
0
,
0
,
0.04
)
]
```

- **Layer 1**: Close shadow (0 1px 2px) = definition
- **Layer 2**: Far shadow (0 4px 12px) = elevation
- **Effect**: Subtle depth without being heavy

### No Shadows (Dark Mode)

```css
dark:shadow-none
```

---

## Common Component Patterns

### Primary Button (Call-to-Action)

```jsx
className = "bg-indigo-600 text-white px-3 py-1.5 rounded-full hover:bg-indigo-700 border-0"
```

- Solid indigo background
- White text
- No border
- Clear primary hierarchy

### Secondary Button

```jsx
className = "bg-slate-100 text-slate-700 px-3 py-1.5 rounded-full border border-slate-200 hover:bg-slate-200"
```

- Light gray background
- Slate text
- Visible border
- Clear secondary hierarchy

### Card Container

```jsx
className = "bg-white border border-slate-200 rounded-lg p-4 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)]"
```

- Solid white background
- Light gray border
- Subtle shadow
- Clear separation

### Status Badge (Success)

```jsx
className = "bg-green-50 text-green-700 border border-green-300 px-2 py-0.5 rounded-md text-xs font-semibold"
```

- Light background + matching border
- Solid text color
- Professional appearance

### Section Container

```jsx
className = "bg-white border border-slate-200 p-6 md:p-8 rounded-xl shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)]"
```

- Solid white
- Visible border
- Proper padding
- Subtle shadow

---

## Do's & Don'ts

### ✅ DO

- Use solid white backgrounds for main content
- Use slate-200 for borders (visible but soft)
- Use indigo-600 for primary actions
- Use subtle shadows (two-layer system)
- Use solid color fills instead of transparency
- Use WCAG AA compliant contrast ratios
- Use consistent spacing and alignment
- Use single accent color throughout

### ❌ DON'T

- Use semi-transparent backgrounds (opacity < 1)
- Use faint borders (opacity < 80%)
- Use glow effects in light mode
- Use multiple accent colors
- Use heavy shadows
- Use bright colors
- Use low contrast text
- Use overly soft backgrounds

---

## Responsive Spacing

```
XS (mobile):     p-4, gap-2, md:p-6
SM (tablet):     p-6, gap-3
MD (desktop):    p-8, gap-4
LG (wide):       p-8, gap-4, space-y-8
```

---

## Border Strategy

### When to use borders

- Card containers
- Button separators
- Input fields
- Section dividers
- Badge edges

### Border color by context

```
Neutral:         border-slate-200
Success:         border-green-300
Error:           border-red-300
Info:            border-blue-300
Accent:          border-indigo-300
```

---

## Text Hierarchy

### Headings

- H1: `text-3xl font-bold text-slate-900`
- H2: `text-2xl font-bold text-slate-900`
- H3: `text-lg font-semibold text-slate-900`
- H4: `text-base font-semibold text-slate-900`

### Body Text

- Primary: `text-sm text-slate-900`
- Secondary: `text-sm text-slate-600`
- Muted: `text-xs text-slate-500`

### Small Text

- Label: `text-xs font-semibold text-slate-700`
- Caption: `text-[10px] text-slate-500`

---

## Interactive States

### Button States

```
Default:         bg-slate-100 text-slate-700
Hover:           bg-slate-200
Active:          bg-slate-300
Disabled:        opacity-50 cursor-not-allowed
Focus:           ring-2 ring-indigo-300
```

### Link States

```
Default:         text-indigo-600 underline
Hover:           text-indigo-700
Visited:         text-indigo-800
```

---

## Animation Guidelines

### Duration

```
Fast:            150ms
Normal:          300ms
Slow:            500ms
```

### Easing

```
Default:         ease-in-out
Entrance:        ease-out
Exit:            ease-in
```

### Effects to Avoid in Light Mode

- ❌ Glow shadows
- ❌ Blur effects
- ❌ Neon colors
- ❌ High opacity changes

---

## Implementation Checklist

When building new components:

- [ ] Use solid white/light backgrounds
- [ ] Add visible slate-200 borders
- [ ] Include subtle shadows
- [ ] Ensure WCAG AA contrast
- [ ] Use indigo accent only
- [ ] Add hover states
- [ ] Test on light mode
- [ ] Test on dark mode
- [ ] Verify mobile responsive
- [ ] Check animation performance

---

## Quick Copy-Paste Templates

### Full Section

```jsx
<section
    className="rounded-xl bg-white border border-slate-200 p-6 md:p-8 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:bg-slate-900/40 dark:border-slate-700/50 dark:shadow-[0_1px_2px_rgba(0,0,0,0.1)]">
    {/* Content */}
</section>
```

### Primary Button

```jsx
<button
    className="bg-indigo-600 text-white px-3 py-1.5 rounded-full hover:bg-indigo-700 font-medium text-sm transition-all duration-300 ease-in-out dark:bg-blue-600 dark:hover:bg-blue-700">
    Action
</button>
```

### Card

```jsx
<article
    className="rounded-lg bg-white border border-slate-200 p-4 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:bg-slate-800/50 dark:border-slate-700/50 dark:shadow-none">
    {/* Content */}
</article>
```

### Badge

```jsx
<span
    className="inline-flex items-center gap-1 rounded-md bg-green-50 text-green-700 border border-green-300 px-2 py-1 text-xs font-semibold dark:bg-green-600/20 dark:text-green-200 dark:border-green-600/40">
  Label
</span>
```

---

## File Locations

```
/frontend/src/
├── components/
│   └── architecture/
│       ├── FlowNode.jsx
│       ├── FlowConnector.jsx
│       └── SimulationController.jsx
├── pages/
│   ├── ArchitectureDocsPage.jsx
│   └── ArchitectureDocsPage.css
├── index.css (CSS variables)
└── context/
    └── ThemeContext.tsx (Theme logic)
```

---

**Last Updated**: March 20, 2026  
**Version**: 1.0 (Final)

