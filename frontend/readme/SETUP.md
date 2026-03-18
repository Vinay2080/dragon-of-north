# Premium Landing Page - Setup Guide

## Quick Start

### 1. Install Dependencies

```bash
cd frontend
npm install
```

This will install `react-feather` (newly added for the landing page icons).

### 2. Run Development Server

```bash
npm run dev
```

The app will be available at `http://localhost:5173` (or the port shown in terminal).

### 3. Visit the Home Page

Navigate to `/` to see the new premium landing page.

---

## What Was Added/Changed

### New Files

- `src/pages/PremiumLandingPage.jsx` - The new landing page component

### Updated Files

- `src/App.jsx` - Routes the home path (`/`) to `PremiumLandingPage`
- `package.json` - Added `react-feather` dependency

### Documentation

- `PREMIUM_LANDING_PAGE.md` - Comprehensive design and implementation guide
- This file (`SETUP.md`)

---

## Architecture Overview

```
App.jsx
  └─ AppLayout
      └─ Routes
          ├─ "/" → PremiumLandingPage (NEW)
          ├─ "/features" → FeaturesDocsPage
          ├─ "/architecture" → ArchitectureDocsPage
          ├─ "/sessions" → SessionsPage (protected)
          └─ ... other routes
```

### PremiumLandingPage Structure

```
PremiumLandingPage
  ├─ Hero (immediate value prop)
  ├─ TrustSection (credibility & stats)
  ├─ HowItWorks (interactive flow)
  ├─ CoreFeatures (6-card grid)
  ├─ Comparison (traditional vs. modern)
  ├─ UseCases (6 real-world scenarios)
  └─ FinalCTA (conversion focus)
```

---

## Development Tips

### Adding New Sections

1. Create a component inside `PremiumLandingPage.jsx`:

```jsx
const NewSection = () => (
    <section className="py-20 md:py-28 lg:py-32">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <motion.div
                variants={containerVariants}
                initial="hidden"
                whileInView="visible"
                viewport={{once: true}}
            >
                {/* Your content */}
            </motion.div>
        </div>
    </section>
);
```

2. Add it to the main return:

```jsx
return (
    <div className="min-h-screen bg-white">
        {/* ... existing sections ... */}
        <NewSection/>
    </div>
);
```

### Modifying Colors

All color references use Tailwind classes:

- `violet-600` - Primary
- `violet-100`, `violet-50` - Light variants
- `gray-600`, `gray-900` - Text colors

To change the theme:

1. Replace all `violet-*` with your color (e.g., `blue-*`, `indigo-*`)
2. Test in light and dark modes

### Tailwind Color Palette

Reference Tailwind's default colors:

- Primary: `indigo`, `blue`, `violet`, `purple`
- Text: `gray-600` (secondary), `gray-900` (primary)
- Backgrounds: `gray-50` (alt), `white` (main)

---

## Testing

### Local Testing

```bash
npm run dev
```

### Build for Production

```bash
npm run build
```

Output will be in `dist/` folder.

### Preview Production Build

```bash
npm run preview
```

---

## Performance Checklist

- [ ] Page loads in < 2s on 4G
- [ ] Lighthouse score > 90
- [ ] No layout shifts (CLS < 0.1)
- [ ] Animations are smooth (60fps)
- [ ] Mobile responsive
- [ ] Accessibility score > 90

Run Lighthouse in Chrome DevTools:

1. Open DevTools (F12)
2. Go to "Lighthouse" tab
3. Click "Analyze page load"

---

## Troubleshooting

### "react-feather not found" error

```bash
npm install react-feather
```

### Animations not working

- Check that Framer Motion is installed: `npm ls framer-motion`
- Ensure the component is using `motion.div` from framer-motion

### Styling looks broken

- Verify Tailwind is running: `npm run dev`
- Clear browser cache (Ctrl+Shift+Delete)
- Check that `index.css` is imported in `main.jsx`

### Build fails

```bash
npm run build -- --debug
```

---

## Deployment

### Vercel (Recommended)

1. Push code to GitHub
2. Connect repo to Vercel
3. Vercel auto-detects Vite config
4. Deploy!

### Docker

See `../Dockerfile` in the project root.

### Traditional Hosting

```bash
npm run build
# Upload `dist/` folder to your host
```

---

## Browser Support

| Browser | Support | Min Version |
|---------|---------|-------------|
| Chrome  | ✅       | 90+         |
| Firefox | ✅       | 88+         |
| Safari  | ✅       | 14+         |
| Edge    | ✅       | 90+         |
| IE 11   | ❌       | N/A         |

---

## Next Steps

1. **Test the page** - Visit `/` after running `npm run dev`
2. **Customize colors** - Edit Tailwind class names to match your brand
3. **Add content** - Update titles, descriptions, and CTAs
4. **Add images/video** - Enhance sections with visual media
5. **Track conversions** - Add analytics events (e.g., button clicks)
6. **A/B test** - Try different copy, colors, layouts

---

## Support

For questions, refer to:

- `PREMIUM_LANDING_PAGE.md` - Design & implementation details
- `src/pages/PremiumLandingPage.jsx` - Inline code comments
- Framer Motion docs: https://www.framer.com/motion/
- Tailwind CSS docs: https://tailwindcss.com/

---

## License

Same as the main project. See `../LICENSE`.


