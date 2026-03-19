# Architecture Page Light Mode - Before & After Reference

## Section Styling

### BEFORE

```jsx
<section className="rounded-xl bg-white/50 p-6 md:p-8 dark:bg-slate-900/40">
```

- Semi-transparent white background
- No border - blends into page
- No shadow - flat appearance
- Hard to distinguish sections

### AFTER

```jsx
<section
    className="rounded-xl bg-white border border-slate-200 p-6 md:p-8 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:bg-slate-900/40 dark:border-slate-700/50">
```

- Solid white background
- Visible light gray border
- Subtle layered shadow (near + far)
- Clear visual separation from page background

---

## Primary Button (Play Full Simulation)

### BEFORE

```jsx
className = "bg-blue-500/15 px-3 py-1.5 font-medium text-blue-700 hover:bg-blue-500/22"
```

- Semi-transparent blue background
- Light blue text
- Weak visual hierarchy
- Doesn't invite clicking

### AFTER

```jsx
className = "bg-indigo-600 px-3 py-1.5 font-medium text-white hover:bg-indigo-700"
```

- Solid indigo background
- White text (high contrast)
- Strong visual hierarchy
- Clear call-to-action
- Single accent color throughout

---

## Secondary Button (Play Request, Reset)

### BEFORE

```jsx
className = "bg-slate-300/25 px-3 py-1.5 text-slate-700 hover:bg-slate-300/35"
```

- Overly soft, transparent background
- No visible border
- Unclear if clickable

### AFTER

```jsx
className = "bg-slate-100 px-3 py-1.5 text-slate-700 border border-slate-200 hover:bg-slate-200"
```

- Light gray background
- Visible border for definition
- Clear secondary priority
- Professional appearance

---

## Flow Node (Request Step)

### BEFORE

```jsx
const toneByState = {
    idle: {
        background: 'bg-slate-50 dark:bg-slate-900/60',
        border: 'border-slate-400/20 dark:border-slate-600/30',
        glow: '',
    },
    active: {
        background: 'bg-blue-50/90 dark:bg-blue-950/40',
        border: 'border-blue-500/50',
        glow: 'shadow-[0_2px_12px_rgba(59,130,246,0.18)]',
    },
}
```

- Blue accent (not consistent)
- Glow shadow in light mode (too much)
- Semi-transparent backgrounds
- Weak borders

### AFTER

```jsx
const toneByState = {
    idle: {
        background: 'bg-white dark:bg-slate-900/60',
        border: 'border-e2e8f0 dark:border-slate-600/30',
        glow: 'shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:shadow-none',
    },
    active: {
        background: 'bg-indigo-50 dark:bg-blue-950/40',
        border: 'border-indigo-300 dark:border-blue-500/50',
        glow: 'shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)] dark:shadow-none',
    },
}
```

- Indigo accent (consistent throughout)
- No glow in light mode
- Solid white backgrounds
- Visible borders
- Proper subtle shadows

---

## Status Badge

### BEFORE

```jsx
className = "bg-green-500/18 text-green-700"
```

- Semi-transparent background
- No border
- Weak visibility

### AFTER

```jsx
className = "bg-green-50 text-green-700 border border-green-300"
```

- Solid light green background
- Matching green border
- Clear and professional

---

## Device Session Card

### BEFORE

```jsx
className = {`rounded-lg border p-4 
  ${isActive ? 'border-slate-300/40 bg-slate-50/80' : 'border-red-300/40 bg-red-50/60'}`
}
```

- Semi-transparent backgrounds
- Weak borders
- Poor contrast

### AFTER

```jsx
className = {`rounded-lg border p-4 shadow-[0_1px_2px_rgba(0,0,0,0.05),0_4px_12px_rgba(0,0,0,0.04)]
  ${isActive ? 'border-slate-200 bg-white' : 'border-red-300 bg-red-50'}`
}
```

- Solid backgrounds (white for active)
- Strong visible borders
- Proper shadows for depth
- Revoked state uses red-50 with red-300 border

---

## Connection Line

### BEFORE

```jsx
<div className="h-[1.5px] w-full bg-slate-300/25 group-hover:opacity-100"/>
```

- Too faint (very low opacity)
- Hard to see in light mode
- Barely visible even on hover

### AFTER

```jsx
<div className="h-[1.5px] w-full bg-slate-200 group-hover:opacity-100"/>
```

- Proper light gray color
- Visible but subtle
- Clear on hover

---

## API Endpoint Display

### BEFORE

```jsx
className = "rounded-lg bg-slate-100/60 px-3 py-2 text-xs text-slate-600"
```

- Semi-transparent background
- Weak contrast

### AFTER

```jsx
className = "rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-700 border border-slate-200"
```

- Solid light background
- Better contrast
- Visible border for definition

---

## Icon Background

### BEFORE

```jsx
className = "bg-slate-200/50"
```

- Semi-transparent, too soft

### AFTER

```jsx
className = "bg-slate-100"
```

- Solid light gray
- Better contrast with icons

---

## Summary Table

| Element           | Before Issue            | After Solution                     |
|-------------------|-------------------------|------------------------------------|
| Sections          | Blended into background | Solid white with border + shadow   |
| Primary Buttons   | Weak hierarchy          | Solid indigo, white text           |
| Secondary Buttons | Soft, unclear           | Border + light background          |
| Cards             | Semi-transparent        | Solid backgrounds, visible borders |
| Borders           | Too faint or missing    | Consistent slate-200               |
| Shadows           | None or glowy           | Subtle two-layer effect            |
| Accent Color      | Mixed (blue/purple)     | Single indigo accent               |
| Text Contrast     | Weak                    | WCAG AA compliant                  |
| Overall Feel      | Washed out              | Premium, crisp, structured         |

---

## Key Principles Applied

1. **Solidity**: Replace transparency with solid colors
2. **Definition**: Add visible borders (slate-200)
3. **Depth**: Subtle shadows instead of glow
4. **Consistency**: Single indigo accent throughout
5. **Contrast**: Solid backgrounds for text readability
6. **Hierarchy**: Clear primary vs secondary styling
7. **Premium**: Stripe-like, structured appearance
8. **Minimal**: No unnecessary effects or colors

All changes maintain dark mode compatibility.

