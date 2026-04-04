import {Navigate, Route, Routes, useNavigate} from 'react-router-dom';
import {useCallback} from 'react';
import {AuthProvider} from './context/AuthContext.jsx';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';
import SignupPage from './pages/SignupPage';
import OtpPage from './pages/OtpPage';
import LoginPage from './pages/LoginPage.jsx';
import SessionsPage from './pages/SessionsPage';
import ForgotPasswordRequestPage from './pages/ForgotPasswordRequestPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import {ToastProvider} from './context/ToastContext.jsx';
import ToastContainer from './components/Toast/ToastContainer';
import NetworkStatus from './components/NetworkStatus/NetworkStatus';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import {useSessionTimeout} from './hooks/useSessionTimeout';
import {useToast} from './hooks/useToast';
import PremiumLandingPage from './pages/PremiumLandingPage';
import FeaturesDocsPage from './pages/FeaturesDocsPage';
import ArchitectureDocsPage from './pages/ArchitectureDocsPage';
import SecurityDemoPage from './pages/SecurityDemoPage';
import DeploymentDocsPage from './pages/DeploymentDocsPage';
import OAuthCallbackPage from './pages/OAuthCallbackPage';
import IdentifierFlowPage from './pages/IdentifierFlowPage';
import PrivacyPolicyPage from './pages/PrivacyPolicyPage';
import TermsOfServicePage from './pages/TermsOfServicePage';
import {useRouteDocumentTitle} from './hooks/useDocumentTitle';
import ProfilePage from './pages/ProfilePage.jsx';
import {clearAuthClientState} from './services/authSession';

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
            <ToastContainer/>
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
                    <Route path="/auth/callback" element={<OAuthCallbackPage/>}/>
                    <Route path="/forgot-password" element={<ForgotPasswordRequestPage/>}/>
                    <Route path="/reset-password" element={<ResetPasswordPage/>}/>
                    <Route path="/sessions" element={<ProtectedRoute><SessionsPage/></ProtectedRoute>}/>
                    <Route path="/profile" element={<ProtectedRoute><ProfilePage/></ProtectedRoute>}/>
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
                    <AppShell/>
                </AuthProvider>
            </ToastProvider>
        </ErrorBoundary>
    );
}
//todo login flow
