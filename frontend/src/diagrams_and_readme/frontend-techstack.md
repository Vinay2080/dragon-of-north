# Frontend tech stack + AI context map

This file is a *structured snapshot* to help AI context gathering for future edits. It is based on scanning source files
under `frontend/`.

## 1) Runtime / framework

- React (used from `react`, `react-dom`)
- React Router DOM (used from `react-router-dom`)

## 2) Build tooling

- Vite (see `frontend/vite.config.js`)
- Tailwind CSS (see `frontend/tailwind.config.js`, `frontend/src/index.css`)

## 3) Language / typing

- Primary: JavaScript + JSX (`.jsx` dominates)
- TypeScript present (`.ts/.tsx` exists; e.g. `src/context/ThemeContext.tsx`, `src/components/Navbar.tsx`)

## 4) Routing (where routes are declared)

- Router mounting: `frontend/src/main.jsx`
    - Wraps the app with:
        - `<ThemeProvider>` from `frontend/src/context/ThemeContext.tsx`
        - `<BrowserRouter>` from `react-router-dom`
        - `<App />` from `frontend/src/App.jsx`
- Route table: `frontend/src/App.jsx`
    - Uses `<Routes>` + `<Route>`.
    - Uses a shared layout wrapper via:
        - `<Route element={<AppLayout/>}> ... </Route>`
        - Layout component file: `frontend/src/components/AppLayout.jsx`

## 5) Pages (route → component mapping)

Declared in `frontend/src/App.jsx` under the `AppLayout` wrapper unless noted.

### Public / docs / platform info pages

- `/` → `frontend/src/pages/PremiumLandingPage.jsx`
- `/features` → `frontend/src/pages/FeaturesDocsPage.jsx`
- `/architecture` → `frontend/src/pages/ArchitectureDocsPage.jsx`
- `/security-demo` → `frontend/src/pages/SecurityDemoPage.jsx`
- `/identifier-flow` → `frontend/src/pages/IdentifierFlowPage.jsx`
- `/deployment` → `frontend/src/pages/DeploymentDocsPage.jsx`
- `/privacy` → `frontend/src/pages/PrivacyPolicyPage.jsx`
- `/terms` → `frontend/src/pages/TermsOfServicePage.jsx`
- `/platform` → `frontend/src/pages/PlatformPage.jsx`

### Auth / account pages

- `/signup` → `frontend/src/pages/SignupPage.jsx`
- `/otp` → `frontend/src/pages/OtpPage.jsx`
- `/login` → `frontend/src/pages/LoginPage.jsx` (wraps `AuthPage`)
- `/auth/callback` → `frontend/src/pages/OAuthCallbackPage.jsx`
- `/forgot-password` → `frontend/src/pages/ForgotPasswordRequestPage.jsx`
- `/reset-password` → `frontend/src/pages/ResetPasswordPage.jsx`

### Protected pages (wrapped in `ProtectedRoute`)

- `/sessions` → `frontend/src/pages/SessionsPage.jsx`
- `/profile` → `frontend/src/pages/ProfilePage.jsx`

### Redirect-only

- `/dashboard` → `<Navigate to="/sessions" replace />` (declared in `frontend/src/App.jsx`)

## 6) Shared layout / navigation shell

- App-wide layout file: `frontend/src/components/AppLayout.jsx`
    - Renders a sidebar (`<aside className="dashboard-sidebar" ...>`) and a topbar (
      `<header className="dashboard-topbar" ...>`), and `<Outlet/>` for nested pages.

## 7) CSS / theming system

### Global CSS variables

- Global CSS file: `frontend/src/index.css`
    - Defines a large `:root { ... }` CSS custom property set.
    - Dark tokens are overridden via selector block:
        - `:root[data-theme="dark"], body[data-theme="dark"], .dark { ... }`
    - Light override block present:
        - `:root[data-theme="light"], body[data-theme="light"], .light { ... }`

### Tailwind configuration

- Tailwind config: `frontend/tailwind.config.js`
    - `darkMode: "class"`
    - `theme.extend.colors` map is defined and references CSS variables (e.g. `hsl(var(--background))`).

### JS theme toggling

- Global theme context: `frontend/src/context/ThemeContext.tsx`
    - Persists `don-theme` in `localStorage`.
    - Applies theme by toggling `document.documentElement.classList` and setting `data-theme` attributes on `<html>` and
      `<body>`.
- Platform page local toggle (independent): `frontend/src/pages/PlatformPage.jsx`
    - Toggles `document.documentElement.classList.toggle('dark', isDark)`.
    - Injects page-local CSS via `<style>{platformStyles}</style>` which defines its own `:root` variables and
      `:root.dark` overrides.

## 8) Animation / icons / shims

### Local shims present

- `frontend/src/shims/framer-motion.tsx`
- `frontend/src/shims/lucide-react.tsx`
- `frontend/src/shims/xyflow-react.jsx`

### Framer Motion usage (examples)

- `frontend/src/components/Navbar.tsx`: imports `{AnimatePresence, motion}` from `framer-motion`
- `frontend/src/pages/PremiumLandingPage.jsx`: imports `{motion}` from `framer-motion`

### Icons usage (examples)

- Direct package import exists in some files: `import {ChevronDown, Loader2, Trash2} from 'lucide-react';` (
  `frontend/src/pages/SessionsPage.jsx`)
- Shim import exists in others: `import * as Icons from '../shims/lucide-react';` (
  `frontend/src/components/AppLayout.jsx`)

## 9) Notable shared UI components (paths)

- Layout wrappers:
    - `frontend/src/components/AppLayout.jsx` (main layout)
    - `frontend/src/components/DocsLayout.jsx` (docs section wrapper; used by some docs pages)
- Navigation:
    - `frontend/src/components/Navbar.tsx` (separate navbar component; not the same as `AppLayout` topbar)
- Alerts:
    - `frontend/src/components/AlertBanner.jsx`
- Auth card wrapper:
    - `frontend/src/components/auth/AuthCardLayout.jsx`

## 10) Platform page specifics (for future context)

- File: `frontend/src/pages/PlatformPage.jsx`
- Contains:
    - Inline CSS string `platformStyles` injected into the page.
    - A sidebar `<aside className="platform-sidebar" ...>` with TOC buttons.
    - Sticky behavior via CSS: `.platform-sidebar { position: sticky; top: 0; height: 100vh; ... }`
    - Active section tracking via `IntersectionObserver` that updates `activeSection`.

---

## 11) Pages index (AI context gathering details)

This section lists *what each page imports/depends on* and any *notable styling/theming patterns* visible directly in
the page source.

### `frontend/src/pages/PremiumLandingPage.jsx`

- Route: `/`
- Imports include (examples from scan):
    - `framer-motion` (`import {motion} from 'framer-motion';`)
- Styling approach: Tailwind utility classes in JSX (e.g. `dark:bg-...`, `dark:text-...` seen in return)

### `frontend/src/pages/FeaturesDocsPage.jsx`

- Route: `/features`
- Imports (verbatim header region):
    - `import {useEffect, useMemo, useRef, useState} from 'react';`
    - `import DocsLayout from '../components/DocsLayout';`
    - `import './FeaturesDocsPage.css';`
- Local CSS file: `frontend/src/pages/FeaturesDocsPage.css`
- Uses `DocsLayout`: YES (`frontend/src/components/DocsLayout.jsx`)

### `frontend/src/pages/ArchitectureDocsPage.jsx`

- Route: `/architecture`
- Imports (verbatim header region):
    - `import {useEffect, useMemo, useRef, useState} from 'react';`
    - `import { ... } from 'react-feather';` (icons)
    - `import DocsLayout from '../components/DocsLayout';`
    - `import FlowNode from '../components/architecture/FlowNode';`
    - `import FlowConnector from '../components/architecture/FlowConnector';`
    - `import SimulationController from '../components/architecture/SimulationController';`
    - `import {useScrollReveal} from '../hooks/useScrollReveal';`
    - `import './ArchitectureDocsPage.css';`
- Local CSS file: `frontend/src/pages/ArchitectureDocsPage.css`
- Uses `DocsLayout`: YES

### `frontend/src/pages/SecurityDemoPage.jsx`

- Route: `/security-demo`
- Imports:
    - `import {useEffect, useMemo, useRef, useState} from 'react';`
    - `import DocsLayout from '../components/DocsLayout';`
- Local CSS file: NOT FOUND (no CSS import in file header)
- Uses `DocsLayout`: YES

### `frontend/src/pages/IdentifierFlowPage.jsx`

- Route: `/identifier-flow`
- Imports:
    - `import {useEffect, useMemo, useRef, useState} from 'react';`
    - `import DocsLayout from '../components/DocsLayout';`
- Local CSS file: NOT FOUND (no CSS import in file header)
- Uses `DocsLayout`: YES

### `frontend/src/pages/DeploymentDocsPage.jsx`

- Route: `/deployment`
- Imports:
    - `import DocsLayout from '../components/DocsLayout';`
    - `import {DeploymentPipelineSimulator} from '../components/FlowDiagram';`
    - `import DeploymentTopologyDiagram from '../components/diagrams/DeploymentTopologyDiagram';`
- Local CSS file: NOT FOUND (no CSS import in file header)
- Uses `DocsLayout`: YES

### `frontend/src/pages/PrivacyPolicyPage.jsx`

- Route: `/privacy`
- Imports:
    - `import DocsLayout from '../components/DocsLayout';`
- Local CSS file: NOT FOUND (no CSS import in file header)
- Uses `DocsLayout`: YES

### `frontend/src/pages/TermsOfServicePage.jsx`

- Route: `/terms`
- Imports:
    - `import DocsLayout from '../components/DocsLayout';`
- Local CSS file: NOT FOUND (no CSS import in file header)
- Uses `DocsLayout`: YES

### `frontend/src/pages/PlatformPage.jsx`

- Route: `/platform`
- Imports:
    - `import {useEffect, useMemo, useRef, useState} from 'react';`
- Local CSS strategy:
    - Inline injected CSS string: `platformStyles`
    - Defines its own `:root { ... }` variables and `:root.dark { ... }` overrides inside `platformStyles`
- Sidebar:
    - Rendered inside the page (`<aside className="platform-sidebar" ...>`)
    - Sticky: YES (CSS rule in `platformStyles`)
- Active section tracking:
    - `IntersectionObserver` updates `activeSection`
- Theme toggle:
    - Local state `isDark` toggles `document.documentElement.classList.toggle('dark', isDark)`

### `frontend/src/pages/SignupPage.jsx`

- Route: `/signup`
- Imports (partial list from file header scan):
    - Routing: `Link`, `useLocation`, `useNavigate` from `react-router-dom`
    - Services/config: `API_CONFIG`, `apiService`
    - Hooks/context: `useAuthState`, `useToast`, `useAuth`
    - Components: `AuthLoadingOverlay`, `RateLimitInfo`, `ValidationError`, `PasswordValidationChecklist`,
      `AuthFlowProgress`, `GoogleLoginButton`, `AuthCardLayout`, `AuthInput`, `AuthButton`, `AuthDivider`, `AlertBanner`
- Local CSS file: NOT FOUND (no CSS import in file header)

### `frontend/src/pages/OtpPage.jsx`

- Route: `/otp`
- Imports (partial list from file header scan):
    - Routing: `useLocation`, `useNavigate`
    - Services/config: `API_CONFIG`, `apiService`
    - Hooks: `useToast`, `useDocumentTitle`
    - Components: `RateLimitInfo`, `AuthFlowProgress`, `AuthCardLayout`, `AuthButton`, `OtpInput`
- Local CSS file: NOT FOUND (no CSS import in file header)
- Uses sessionStorage keys:
    - `otpIdentifier`, `otpIdentifierType`, `otpFlow` (declared as `OTP_SESSION_KEYS` in file)

### `frontend/src/pages/LoginPage.jsx`

- Route: `/login`
- Implementation: renders `AuthPage` (`frontend/src/pages/AuthPage.jsx`)

### `frontend/src/pages/AuthPage.jsx`

- Route: (indirect; rendered by LoginPage)
- Imports (partial list from file header scan):
    - Routing: `Link`, `useLocation`, `useNavigate`
    - Services/config: `API_CONFIG`, `apiService`
    - Hooks/context: `useToast`, `useAuthState`, `useAuth`, `useDocumentTitle`
    - Components: `AuthErrorMessage`, `AuthLoadingOverlay`, `ValidationError`, `GoogleLoginButton`, `AuthCardLayout`,
      `AuthInput`, `PasswordInput`, `AuthButton`, `AuthDivider`
    - Utils: `getDeviceId`, `persistPostLoginRedirect`, `resolvePostLoginRedirectPath`

### `frontend/src/pages/ForgotPasswordRequestPage.jsx`

- Route: `/forgot-password`
- Imports:
    - Routing: `useNavigate`
    - Services/config: `API_CONFIG`, `apiService`
    - Hooks/context: `useToast`, `useAuth`
    - Components: `AuthCardLayout`, `AuthInput`, `AuthButton`

### `frontend/src/pages/ResetPasswordPage.jsx`

- Route: `/reset-password`
- Imports:
    - Routing: `useLocation`, `useNavigate`
    - Services/config: `API_CONFIG`, `apiService`
    - Hooks: `useToast`
    - Components: `AuthCardLayout`, `AuthInput`, `PasswordInput`, `AuthButton`

### `frontend/src/pages/OAuthCallbackPage.jsx`

- Route: `/auth/callback`
- Imports:
    - Routing: `useLocation`, `useNavigate`
    - Services/config: `API_CONFIG`, `apiService`
    - Context: `useAuth`
    - Components: `AuthCardLayout`
    - Utils: `resolvePostLoginRedirectPath`
- Uses localStorage keys (as seen in file):
    - Removes `isAuthenticated`, `user` in `redirectToLogin()`
    - Reads `auth_identifier_hint` (`IDENTIFIER_HINT_KEY`)

### `frontend/src/pages/SessionsPage.jsx`

- Route: `/sessions` (protected via `ProtectedRoute` in `frontend/src/App.jsx`)
- Imports (partial list from file header scan):
    - React hooks: `useCallback`, `useEffect`, `useMemo`, `useState`
    - Icons: `import {ChevronDown, Loader2, Trash2} from 'lucide-react';`
    - Context/hooks: `useAuth`, `useToast`
    - Services/config: `API_CONFIG`, `apiService`
    - Utils/components: `sessionFormatters`, `getDeviceId`

### `frontend/src/pages/ProfilePage.jsx`

- Route: `/profile` (protected via `ProtectedRoute` in `frontend/src/App.jsx`)
- Imports (partial list from file header scan):
    - Routing: `useNavigate`
    - Context/hooks: `useAuth`, `useToast`
    - Services/config: `apiService`, `API_CONFIG`
    - Formatting: `formatDateTime`
    - Components: `ProfileHeader`, `ProfileInfoSection`, `ProfileSettings`, `AvatarUploadModal`, `AvatarPreviewModal`,
      `Button`

---

## 12) Theming + dark-mode overlap notes (AI context)

Theme signals and mechanisms observed:

- Global theme system (`frontend/src/context/ThemeContext.tsx`) sets:
    - `document.documentElement.classList` (`dark` class)
    - `data-theme` attributes on `<html>` and `<body>` (`light`/`dark`)
- Global CSS (`frontend/src/index.css`) dark override selector includes:
    - `:root[data-theme="dark"], body[data-theme="dark"], .dark { ... }`
- Platform page (`frontend/src/pages/PlatformPage.jsx`) injects its own CSS variables inside `platformStyles` and uses
  `:root.dark { ... }` inside that string.

---

## 13) CSS modules / global CSS inventory

- Global CSS:
    - `frontend/src/index.css`
- Page-level CSS files imported into pages:
    - `frontend/src/pages/FeaturesDocsPage.css`
    - `frontend/src/pages/ArchitectureDocsPage.css`
- CSS Modules (`*.module.css`): NOT FOUND (search for `module.css` had no results)

---

## 14) External-AI edit guidance (consistency + “don’t break existing pages”)

This section is written as **AI context** for prompt authors. It documents *existing conventions and constraints
observed in the repo* so new changes match existing naming and patterns.

### 14.1 Routing + page registration conventions

- Routes are declared in `frontend/src/App.jsx` using `<Routes>` + nested `<Route element={<AppLayout/>}>`.
- Most pages are rendered under `AppLayout` via `Outlet`.
- Protected pages are wrapped inline:
    - `frontend/src/App.jsx`: `<Route path="/sessions" element={<ProtectedRoute><SessionsPage/></ProtectedRoute>}/>`
    - `frontend/src/App.jsx`: `<Route path="/profile" element={<ProtectedRoute><ProfilePage/></ProtectedRoute>}/>`

### 14.2 Layout + navigation conventions

- Main wrapper is `frontend/src/components/AppLayout.jsx`.
    - Uses class-based styling (non-Tailwind) with class names like:
        - `dashboard-shell`, `dashboard-layout`, `dashboard-sidebar`, `dashboard-topbar`, `dashboard-main`
    - Renders both sidebar + topbar and then `<Outlet/>`.
- Docs-style pages commonly use `frontend/src/components/DocsLayout.jsx`.

### 14.3 Component + file naming conventions (observed)

- Pages live in `frontend/src/pages/` and filenames are `PascalCase...Page.jsx` (examples: `PlatformPage.jsx`,
  `SessionsPage.jsx`).
- Components live in `frontend/src/components/` (and subfolders like `components/auth`, `components/profile`,
  `components/sessions`).
- Many core UI blocks use *className strings* (not CSS modules) with BEM-like patterns in some areas:
    - Examples: `platform-sidebar__brand`, `session-card__container`, `dashboard-nav-item__label`.

### 14.4 Styling conventions + constraints

- Global styling is in `frontend/src/index.css` (Tailwind directives + many CSS variables).
- Tailwind is configured to map colors to CSS variables (`frontend/tailwind.config.js`).
- CSS Modules are not used (no `*.module.css` found).
- Some pages use page-level CSS files:
    - `frontend/src/pages/FeaturesDocsPage.css`
    - `frontend/src/pages/ArchitectureDocsPage.css`
- The `PlatformPage` is *styling-isolated* by injecting a large `platformStyles` string and using its own CSS variables.

### 14.5 Theme conventions + “don’t conflict” notes

- Global theme state lives in `frontend/src/context/ThemeContext.tsx`:
    - Stores `don-theme` in `localStorage`.
    - Applies theme by:
        - toggling `.dark` on `<html>`/`<body>`
        - setting `data-theme="light|dark"` on `<html>`/`<body>`
- Global CSS dark overrides rely on selectors:
    - `:root[data-theme="dark"], body[data-theme="dark"], .dark { ... }`
- `PlatformPage` also toggles `.dark` on `<html>` itself and uses its own `platformStyles` variables (`:root` and
  `:root.dark`).

### 14.6 Page-specific “do not break” behaviors (observed)

#### Platform page (`frontend/src/pages/PlatformPage.jsx`)

- Depends on:
    - `IntersectionObserver` for active TOC highlighting (`activeSection`).
    - Sidebar markup and CSS classes prefixed with `platform-...`.
    - Inline style injection: `<style>{platformStyles}</style>`.
    - Local theme toggle button: `.platform-theme-toggle`.
- If external AI edits this page, preserve:
    - `SECTION_ORDER` structure and `id` values (used for scrolling + observer).
    - `id` attributes on each `<section id="...">`.
    - `platformStyles` variable names (e.g. `--bg`, `--border`, `--text-primary`, etc.).

#### Auth pages (`SignupPage.jsx`, `OtpPage.jsx`, `AuthPage.jsx`, `ForgotPasswordRequestPage.jsx`,
`ResetPasswordPage.jsx`, `OAuthCallbackPage.jsx`)

- Shared building blocks commonly used:
    - `AuthCardLayout`, `AuthInput`, `AuthButton`, `AuthDivider`, `PasswordInput`, `GoogleLoginButton`,
      `AuthFlowProgress`.
- Session / state persistence patterns observed:
    - `OtpPage.jsx` uses `sessionStorage` keys: `otpIdentifier`, `otpIdentifierType`, `otpFlow`.
    - `OAuthCallbackPage.jsx` touches `localStorage` keys: `isAuthenticated`, `user`, and `auth_identifier_hint`.

#### Sessions page (`frontend/src/pages/SessionsPage.jsx`)

- Imports icons from **package** `lucide-react` (example: `ChevronDown`, `Loader2`, `Trash2`).
- Uses `getDeviceId()` and session formatting helpers from `components/sessions/sessionFormatters`.

#### Profile page (`frontend/src/pages/ProfilePage.jsx`)

- Composes multiple components from `components/profile/*` and uses `components/ui/Button.jsx`.

### 14.7 Icon + animation usage conventions

- Both direct-package imports and shim imports exist:
    - Direct: `import {ChevronDown, Loader2, Trash2} from 'lucide-react';`
    - Shim: `import * as Icons from '../shims/lucide-react';`
- Framer Motion is used in some parts of the app (e.g. `Navbar.tsx`, `PremiumLandingPage.jsx`).
- Local shims exist that may be referenced by Vite aliasing:
    - `frontend/src/shims/framer-motion.tsx`
    - `frontend/src/shims/lucide-react.tsx`
    - `frontend/src/shims/xyflow-react.jsx`

### 14.8 Prompt template (for external AI) — safe edit checklist

When asking an external AI to change UI/pages in this repo, include these constraints in the prompt:

- “Do not rename existing routes, page components, or CSS class names unless explicitly instructed.”
- “Keep page filenames and component names in PascalCase and keep them in the existing folders (`src/pages`,
  `src/components`).”
- “Avoid introducing CSS Modules; prefer existing patterns (Tailwind utility classes, existing global CSS variables, or
  existing page CSS files).”
- “Respect the global theme mechanism (`ThemeContext.tsx` + `.dark` + `data-theme`) and avoid introducing a second
  incompatible theme state.”
- “If editing `PlatformPage.jsx`, preserve `SECTION_ORDER`, section `id`s, and the injected `platformStyles` variable
  scheme.”
