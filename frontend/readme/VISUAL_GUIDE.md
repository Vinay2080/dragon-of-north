# Premium Landing Page - Visual Guide

## Complete Page Layout

```
╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║                         ┌─────────────────────┐                         ║
║                         │ Authentication      │                         ║
║                         │ Platform            │                         ║
║                         └─────────────────────┘                         ║
║                                                                          ║
║                   Control Sessions.                                     ║
║                   Not Just Logins.                                      ║
║                                                                          ║
║              Short-lived tokens, refresh rotation,                      ║
║          and full session visibility — built for modern                 ║
║                    systems that demand control.                         ║
║                                                                          ║
║              ┌──────────────────┐  ┌─────────────┐                    ║
║              │ Explore Sessions │  │ View Flow → │                    ║
║              └──────────────────┘  └─────────────┘                    ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                            [SECTION 1: HERO]
                        py-20 md:py-28 lg:py-32
                          (Light gradient bg)

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║              Built for Real-World Security                             ║
║     Enterprise-grade authentication designed to eliminate               ║
║          common vulnerabilities                                         ║
║                                                                          ║
║  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐            ║
║  │ ⚡          │    │ 🔒           │    │ ⚠️           │            ║
║  │ Short-lived │    │ Session-Level│    │ Instant      │            ║
║  │ Tokens      │    │ Control      │    │ Revocation   │            ║
║  │ Reduce      │    │ Track every  │    │ Terminate    │            ║
║  │ attack      │    │ device and   │    │ compromised  │            ║
║  │ surface     │    │ session in   │    │ sessions     │            ║
║  │ with auto   │    │ real-time.   │    │ immediately. │            ║
║  │ expiration. │    │ Revoke       │    │ No waiting   │            ║
║  │             │    │ access       │    │ for token    │            ║
║  │             │    │ instantly.   │    │ expiry.      │            ║
║  └──────────────┘    └──────────────┘    └──────────────┘            ║
║                                                                          ║
║  Stats Row:                                                             ║
║                                                                          ║
║          0                    <100ms              ∞                    ║
║    Token Reuse Risk    Session Revocation    Devices Tracked         ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                       [SECTION 2: TRUST / PROOF]
                      bg-gray-50 py-20 md:py-28
                            lg:py-32

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║                        How It Works                                    ║
║                 Interactive flow: click a step to                      ║
║                    explore the details.                                ║
║                                                                          ║
║  ┌────────────┐┌──────────┐┌──────────┐┌──────────┐┌──────────┐      ║
║  │ Step 1     ││ Step 2   ││ Step 3   ││ Step 4   ││ Step 5   │      ║
║  │ Secure     ││ Access   ││ Token    ││ Refresh  ││ Session  │      ║
║  │ Login      ││ Token    ││ Expires  ││ Token    ││ Tracked  │      ║
║  │            ││ Issued   ││          ││ Rotates  ││          │      ║
║  │ Users auth ││ A short- ││ Expiry   ││ Every    ││ Every    │      ║
║  │ with creds.││ lived JWT││ limits   ││ refresh  ││ device & │      ║
║  │ Policies & ││ is issued││ impact of││ generates││ session  │      ║
║  │ validation ││ for API  ││ leaked   ││ new token││ remains  │      ║
║  │ applied.   ││ access.  ││ tokens.  ││ to block ││ observable.║      ║
║  │            ││          ││          ││ replays. ││          │      ║
║  └────────────┘└──────────┘└──────────┘└──────────┘└──────────┘      ║
║                      ↓ (Active card highlighted)                     ║
║                                                                          ║
║  ┌──────────────────────────────────────────────────────────┐         ║
║  │ Secure Login                                             │         ║
║  │                                                          │         ║
║  │ Users authenticate with credentials. Policies and       │         ║
║  │ validations are applied before issuing tokens.          │         ║
║  │ Identity verification, MFA checks, and compliance       │         ║
║  │ policies run at the auth layer. Only valid,             │         ║
║  │ authorized users receive session material.              │         ║
║  │                                                          │         ║
║  │ [Learn More]  [View Flow]                               │         ║
║  └──────────────────────────────────────────────────────────┘         ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                   [SECTION 3: HOW IT WORKS (INTERACTIVE)]
                          py-20 md:py-28 lg:py-32

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║                          Core Features                                 ║
║             Everything you need to build secure, scalable              ║
║                         authentication.                                ║
║                                                                          ║
║  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐              ║
║  │ ⚡           │   │ 🔒            │   │ 👁️            │              ║
║  │ Short-lived  │   │ Refresh Token │   │ Session      │              ║
║  │ Tokens       │   │ Rotation      │   │ Visibility   │              ║
║  │              │   │              │   │              │              ║
║  │ Reduce attack│   │ Every refresh │   │ Track every  │              ║
║  │ surface with │   │ generates a   │   │ device and   │              ║
║  │ automatic    │   │ new token to  │   │ session in   │              ║
║  │ expiration.  │   │ prevent replay│   │ real-time.   │              ║
║  │              │   │ attacks.      │   │              │              ║
║  │              │   │              │   │              │              ║
║  │ Learn More →│   │ Learn More →│   │ Explore      │              ║
║  │              │   │              │   │ Sessions →   │              ║
║  └──────────────┘   └──────────────┘   └──────────────┘              ║
║                                                                          ║
║  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐              ║
║  │ ⚠️            │   │ 📱            │   │ 🛡️            │              ║
║  │ Instant      │   │ Device       │   │ Enterprise   │              ║
║  │ Revocation   │   │ Awareness    │   │ Grade        │              ║
║  │              │   │              │   │ Policies     │              ║
║  │ Terminate    │   │ Identify and │   │              │              ║
║  │ compromised  │   │ manage       │   │ Enforce      │              ║
║  │ sessions     │   │ sessions per │   │ expiry       │              ║
║  │ immediately. │   │ device.      │   │ limits,      │              ║
║  │ No waiting   │   │ Support      │   │ device       │              ║
║  │ for token    │   │ multi-device │   │ quotas, &    │              ║
║  │ expiry.      │   │ workflows.   │   │ geographic   │              ║
║  │              │   │              │   │ restrictions.║              ║
║  │ View Security│   │ Explore      │   │ Learn More →║              ║
║  │ Demo →       │   │ Features →   │   │              │              ║
║  └──────────────┘   └──────────────┘   └──────────────┘              ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                      [SECTION 4: CORE FEATURES]
                      bg-gray-50 py-20 md:py-28
                            lg:py-32

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║                Traditional Auth vs. Modern Control                     ║
║                                                                          ║
║  ┌──────────────────────────┐    ┌──────────────────────────┐         ║
║  │ Common Risks             │    │ Our Solution             │         ║
║  │                          │    │                          │         ║
║  │ ✕ Long-lived Tokens      │    │ ✓ Short-lived Tokens     │         ║
║  │   Hours/days of exposure │    │   Minutes, minimize risk │         ║
║  │                          │    │                          │         ║
║  │ ✕ No Session Tracking    │    │ ✓ Full Visibility        │         ║
║  │   Blind to devices       │    │   Real-time tracking     │         ║
║  │                          │    │                          │         ║
║  │ ✕ Weak Revocation        │    │ ✓ Instant Revocation     │         ║
║  │   Wait for expiry        │    │   Terminate in ms        │         ║
║  │                          │    │                          │         ║
║  │ ✕ Replay Vulnerability   │    │ ✓ Rotation-Based Refresh │         ║
║  │   Reused tokens          │    │   Every refresh new token│         ║
║  │                          │    │                          │         ║
║  └──────────────────────────┘    └──────────────────────────┘         ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                   [SECTION 5: WHY TRADITIONAL AUTH FAILS]
                          py-20 md:py-28 lg:py-32

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║                            Use Cases                                  ║
║              Designed for real-world scenarios and                     ║
║                     modern challenges.                                 ║
║                                                                          ║
║  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐              ║
║  │ Enterprise   │   │ Multi-Device │   │ Real-Time    │              ║
║  │ Security     │   │ Control      │   │ Session      │              ║
║  │ Enforce      │   │ Users work   │   │ Tracking     │              ║
║  │ device       │   │ from multiple│   │ Know exactly │              ║
║  │ quotas, geo- │   │ devices.     │   │ where users  │              ║
║  │ fencing, &   │   │ Track and    │   │ are logged   │              ║
║  │ revocation   │   │ manage each  │   │ in. Revoke   │              ║
║  │ policies.    │   │ independently.  │   │ suspicious   │              ║
║  │              │   │              │   │ activity     │              ║
║  │              │   │              │   │ instantly.   │              ║
║  └──────────────┘   └──────────────┘   └──────────────┘              ║
║                                                                          ║
║  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐              ║
║  │ Compliance & │   │ Incident     │   │ Developer    │              ║
║  │ Audit        │   │ Response     │   │ Experience   │              ║
║  │ Full audit   │   │ Terminate    │   │ Clean APIs   │              ║
║  │ trail of     │   │ all sessions │   │ for session  │              ║
║  │ auth events. │   │ for a user   │   │ management.  │              ║
║  │ Exportable   │   │ in ms. No    │   │ Minimal      │              ║
║  │ session      │   │ database     │   │ config,      │              ║
║  │ history for  │   │ queries      │   │ maximum      │              ║
║  │ compliance.  │   │ required.    │   │ security.    │              ║
║  └──────────────┘   └──────────────┘   └──────────────┘              ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                         [SECTION 6: USE CASES]
                      bg-gray-50 py-20 md:py-28
                            lg:py-32

╔══════════════════════════════════════════════════════════════════════════╗
║                                                                          ║
║                                                                          ║
║             Start Building Secure Systems                             ║
║                                                                          ║
║    Enterprise-grade authentication with session control, token         ║
║          rotation, and real-time visibility.                          ║
║                                                                          ║
║        ┌──────────────────┐    ┌────────────────────────┐            ║
║        │  Get Started     │    │ Explore Docs →         │            ║
║        └──────────────────┘    └────────────────────────┘            ║
║                                                                          ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
                         [SECTION 7: FINAL CTA]
                     bg-gradient-to-b (violet-600 to -700)
                          py-20 md:py-28 lg:py-32
```

---

## Section Breakdown

### Section 1: Hero

- **Spacing:** Large (py-32 on desktop)
- **Color:** White bg with subtle gradient
- **Typography:** Largest heading (text-6xl)
- **Layout:** Center-aligned, single column
- **CTAs:** 2 buttons (primary + secondary)
- **Animation:** Staggered fade-in on load

---

### Section 2: Trust Section

- **Spacing:** Large (py-32 on desktop)
- **Color:** Light gray background (bg-gray-50)
- **Layout:** 3-column grid
- **Cards:** White with hover effects
- **Icons:** Violet background circles
- **Animation:** Fade-in on scroll

---

### Section 3: How It Works

- **Spacing:** Large (py-32 on desktop)
- **Color:** White
- **Layout:** 5-column step cards, then detail section
- **Interaction:** Click to switch active step
- **Animation:** Smooth transition on step change
- **Detail Section:** Gradient background (violet-50)

---

### Section 4: Core Features

- **Spacing:** Large (py-32 on desktop)
- **Color:** Light gray background (bg-gray-50)
- **Layout:** 3-column grid (2 on tablet, 1 on mobile)
- **Cards:** Hover to lift and glow
- **Icons:** Large, centered
- **Animation:** Fade-in + scale on scroll

---

### Section 5: Comparison

- **Spacing:** Large (py-32 on desktop)
- **Color:** White
- **Layout:** 2-column (stacked on mobile)
- **Left Column:** Slide in from left
- **Right Column:** Slide in from right
- **Icons:** Checkmarks and X marks

---

### Section 6: Use Cases

- **Spacing:** Large (py-32 on desktop)
- **Color:** Light gray background (bg-gray-50)
- **Layout:** 3-column grid (2 on tablet, 1 on mobile)
- **Cards:** Simple text cards
- **Animation:** Fade-in on scroll

---

### Section 7: Final CTA

- **Spacing:** Large (py-32 on desktop)
- **Color:** Violet gradient (violet-600 → violet-700)
- **Typography:** Large white text
- **Layout:** Center-aligned
- **CTAs:** 2 buttons (white primary, outline secondary)
- **Animation:** Staggered fade-in + scale

---

## Color Palette

```
Primary Colors:
├─ Violet-600: #7c3aed (buttons, accents)
├─ Violet-500: #a855f7 (hover states)
├─ Violet-100: #ede9fe (light backgrounds)
└─ Violet-50:  #faf5ff (very light backgrounds)

Neutral Colors:
├─ White:      #ffffff (main bg)
├─ Gray-50:    #f9fafb (alt bg)
├─ Gray-600:   #4b5563 (secondary text)
├─ Gray-900:   #111827 (primary text)
└─ Gray-300:   #d1d5db (borders)

Status Colors:
├─ Green:      #16a34a (success checkmarks)
└─ Red:        #dc2626 (error/risk marks)
```

---

## Typography Hierarchy

```
Hero Heading:           text-6xl font-bold (88px on desktop)
Section Heading:        text-5xl font-bold (48px on desktop)
Subsection Heading:     text-3xl font-bold (30px on desktop)
Card Title:             text-xl font-bold (20px)
Body Text:              text-lg (18px)
Small Text/Label:       text-sm (14px)
```

---

## Spacing Scale

```
Desktop (lg):
├─ Section padding: py-32 (128px)
├─ Container padding: px-8 (32px)
└─ Gap between items: gap-8 (32px)

Tablet (md):
├─ Section padding: py-28 (112px)
├─ Container padding: px-6 (24px)
└─ Gap between items: gap-6-8 (24-32px)

Mobile (sm):
├─ Section padding: py-20 (80px)
├─ Container padding: px-4 (16px)
└─ Gap between items: gap-4-6 (16-24px)
```

---

## Interactive Elements

### Buttons

```css
Primary Button:
├─ bg-violet-600 text-white
├─ hover:bg-violet-700
├─ hover:shadow-lg hover:shadow-violet-500

/
30
├─ transform hover:-translate-y-0

.5
└─ rounded-lg px-8 py-3
Secondary Button:
├─ border border-gray-300 text-gray-900
├─ hover:bg-gray-50
├─ rounded-lg px-8 py-3
└─ no shadow

/
transform
```

### Cards

```css
On Hover:
├─ border changes to violet-300
├─ shadow elevates

(
shadow-xl

)
├─ transform:
translateY

(
-
4
px

)
for some
└─ icon bg becomes violet-200
```

### Step Cards (How It Works)

```css
Active:
├─ bg-violet-50
├─ border-2 border-violet-600
├─ shadow-lg
Inactive:
├─ bg-white
├─ border-2 border-gray-200
└─ hover:border-gray-300
```

---

## Responsive Breakpoints

```
Mobile: <640px (default)
├─ 1 column layouts
├─ py-20 spacing
└─ Full width cards

Small (sm): 640px+
├─ Still mostly 1 column
├─ py-20 spacing
└─ Adjusted padding

Medium (md): 768px+
├─ 2-3 column layouts
├─ py-28 spacing
└─ px-6 padding

Large (lg): 1024px+
├─ 3 column layouts
├─ py-32 spacing
└─ px-8 padding
```

---

## Animation Timings

```
Fade In:          duration: 0.5s, opacity: 0 → 1
Slide In:         duration: 0.6s, x/y: ±40px → 0
Scale:            duration: 0.3s, scale: 0.95 → 1
Stagger:          delayChildren: 0.2s, staggerChildren: 0.1s
Viewport Trigger: whileInView + once: true
```

---

## Accessibility Features

- Semantic HTML (`<section>`, `<h2>`, `<button>`)
- ARIA labels for button states
- Keyboard navigation support
- Color contrast ≥ 4.5:1
- Focus states visible
- No motion that can cause seizures

---

## Performance Metrics

Target metrics for Lighthouse:

- Performance: > 90
- Accessibility: > 95
- Best Practices: > 90
- SEO: > 95

Current optimizations:

- No images (CSS gradients only)
- Minimal JavaScript (only Framer Motion)
- Lazy animations (`whileInView`)
- No external fonts (using system stack + Google Inter)


