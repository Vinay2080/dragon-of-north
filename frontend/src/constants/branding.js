export const BRAND_NAME = 'Dragon of North';

export const DEFAULT_TAB_TITLE = 'Dragon of North – Developer-First Authentication Infrastructure';

export const formatBrandedPageTitle = (pageName) => {
    const normalizedPageName = (pageName || '').trim();
    return normalizedPageName ? `${normalizedPageName} – ${BRAND_NAME}` : DEFAULT_TAB_TITLE;
};

export const ROUTE_PAGE_TITLES = {
    '/': 'Home',
    '/features': 'Features',
    '/architecture': 'Architecture',
    '/security-demo': 'Security Demo',
    '/identifier-flow': 'Identifier Flow',
    '/deployment': 'Deployment',
    '/privacy': 'Privacy Policy',
    '/terms': 'Terms of Service',
    '/signup': 'Sign Up',
    '/otp': 'Verify OTP',
    '/login': 'Login',
    '/auth/callback': 'Authentication Callback',
    '/forgot-password': 'Forgot Password',
    '/reset-password': 'Reset Password',
    '/sessions': 'Sessions',
    '/dashboard': 'Dashboard',
};


