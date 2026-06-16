import {Navigate, Route, Routes, useNavigate} from 'react-router-dom';
import {lazy, Suspense, useCallback} from 'react';
import {AuthProvider} from './context/AuthContext.jsx';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';
import {ToastProvider} from './context/ToastContext.jsx';
import NetworkStatus from './components/NetworkStatus/NetworkStatus';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import {useSessionTimeout} from './hooks/useSessionTimeout';
import {useToast} from './hooks/useToast';
import {useRouteDocumentTitle} from './hooks/useDocumentTitle';
import {clearAuthClientState} from './services/authSession';

const SignupPage = lazy(() => import('./pages/SignupPage'));
const OtpPage = lazy(() => import('./pages/OtpPage'));
const LoginPage = lazy(() => import('./pages/LoginPage.jsx'));
const SessionsPage = lazy(() => import('./pages/SessionsPage'));
const ForgotPasswordRequestPage = lazy(() => import('./pages/ForgotPasswordRequestPage'));
const ResetPasswordPage = lazy(() => import('./pages/ResetPasswordPage'));

const PremiumLandingPage = lazy(() => import('./pages/PremiumLandingPage'));
const FeaturesDocsPage = lazy(() => import('./pages/FeaturesDocsPage'));
const ArchitectureDocsPage = lazy(() => import('./pages/ArchitectureDocsPage'));
const SecurityDemoPage = lazy(() => import('./pages/SecurityDemoPage'));
const DeploymentDocsPage = lazy(() => import('./pages/DeploymentDocsPage'));
const OAuthCallbackPage = lazy(() => import('./pages/OAuthCallbackPage'));
const PasswordlessVerifyPage = lazy(() => import('./pages/PasswordlessVerifyPage.jsx'));
const IdentifierFlowPage = lazy(() => import('./pages/IdentifierFlowPage'));
const PrivacyPolicyPage = lazy(() => import('./pages/PrivacyPolicyPage'));
const TermsOfServicePage = lazy(() => import('./pages/TermsOfServicePage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage.jsx'));
const HomeDocsPage = lazy(() => import('./pages/HomeDocsPage.jsx'));
const MfaManagementPage = lazy(() => import('./pages/MfaManagementPage.jsx'));

const AppShell = () => {
    const navigate = useNavigate();
    const {toast} = useToast();
    useRouteDocumentTitle();

    const onTimeout = useCallback(() => {
        clearAuthClientState();
        toast.error('Your session timed out. Please log in again.');
        navigate('/');
    }, [navigate, toast]);

    useSessionTimeout({
        enabled: !!localStorage.getItem('isAuthenticated'),
        warningMs: 120000,
        sessionMs: 3600000,
        onWarning: () => toast.warning('Your session will expire soon. Please save your work.'),
        onTimeout,
    });

    return (
        <>
            <NetworkStatus/>
            <Routes>
                <Route element={<AppLayout/>}>
                    <Route path="/" element={<PremiumLandingPage/>}/>
                    <Route path="/features" element={<FeaturesDocsPage/>}/>
                    <Route path="/architecture" element={<ArchitectureDocsPage/>}/>
                    <Route path="/security-demo" element={<SecurityDemoPage/>}/>
                    <Route path="/identifier-flow" element={<IdentifierFlowPage/>}/>
                    <Route path="/deployment" element={<DeploymentDocsPage/>}/>
                    <Route path="/privacy" element={<PrivacyPolicyPage/>}/>
                    <Route path="/terms" element={<TermsOfServicePage/>}/>
                    <Route path="/signup" element={<SignupPage/>}/>
                    <Route path="/otp" element={<OtpPage/>}/>
                    <Route path="/login" element={<LoginPage/>}/>
                    <Route path="/passwordless/verify" element={<PasswordlessVerifyPage/>}/>
                    <Route path="/login/passwordless/verify" element={<PasswordlessVerifyPage/>}/>
                    <Route path="/auth/callback" element={<OAuthCallbackPage/>}/>
                    <Route path="/forgot-password" element={<ForgotPasswordRequestPage/>}/>
                    <Route path="/reset-password" element={<ResetPasswordPage/>}/>
                    <Route path="/home" element={<ProtectedRoute><HomeDocsPage/></ProtectedRoute>}/>
                    <Route path="/sessions" element={<ProtectedRoute><SessionsPage/></ProtectedRoute>}/>
                    <Route path="/profile" element={<ProtectedRoute><ProfilePage/></ProtectedRoute>}/>
                    <Route path="/security/mfa" element={<ProtectedRoute><MfaManagementPage/></ProtectedRoute>}/>
                </Route>
                <Route path="/dashboard" element={<Navigate to="/sessions" replace/>}/>
            </Routes>
        </>
    );
};

export default function App() {
    return (
        <ErrorBoundary>
            <ToastProvider>
                <AuthProvider>
                    <Suspense fallback={<div className="flex h-screen w-full items-center justify-center">
                        <div className="animate-pulse flex flex-col items-center gap-4">
                            <div className="h-10 w-10 rounded-full bg-slate-200 dark:bg-slate-700"></div>
                            <div className="h-4 w-32 rounded bg-slate-200 dark:bg-slate-700"></div>
                        </div>
                    </div>}>
                        <AppShell/>
                    </Suspense>
                </AuthProvider>
            </ToastProvider>
        </ErrorBoundary>
    );
}
