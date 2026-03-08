import {Route, Routes, useNavigate} from 'react-router-dom';
import {useCallback} from 'react';
import {AuthProvider} from './context/AuthContext.jsx';
import ProtectedRoute from './components/ProtectedRoute';
import SignupPage from './pages/SignupPage';
import OtpPage from './pages/OtpPage';
import AuthPage from './pages/AuthPage';
import DashboardPage from './pages/DashboardPage';
import ForgotPasswordRequestPage from './pages/ForgotPasswordRequestPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import {ToastProvider} from './context/ToastContext.jsx';
import ToastContainer from './components/Toast/ToastContainer';
import NetworkStatus from './components/NetworkStatus/NetworkStatus';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import {useSessionTimeout} from './hooks/useSessionTimeout';
import {useToast} from './hooks/useToast';
import LandingPage from './pages/LandingPage';

const AppShell = () => {
    const navigate = useNavigate();
    const {toast} = useToast();

    const onTimeout = useCallback(() => {
        localStorage.removeItem('isAuthenticated');
        localStorage.removeItem('user');
        toast.error('Your session timed out. Please log in again.');
        navigate('/login');
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
                <Route path="/" element={<LandingPage/>}/>
                <Route path="/signup" element={<SignupPage/>}/>
                <Route path="/otp" element={<OtpPage/>}/>
                <Route path="/login" element={<AuthPage/>}/>
                <Route path="/forgot-password" element={<ForgotPasswordRequestPage/>}/>
                <Route path="/reset-password" element={<ResetPasswordPage/>}/>
                <Route path="/dashboard" element={<ProtectedRoute><DashboardPage/></ProtectedRoute>}/>
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
