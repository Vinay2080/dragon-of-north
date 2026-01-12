import { Routes, Route } from "react-router-dom";
import AuthIdentifierPage from "./pages/AuthIdentifierPage";

const SignupPage = () => <div style={{ padding: '20px' }}>Signup Page (Placeholder)</div>;
const OtpPage = () => <div style={{ padding: '20px' }}>OTP Page (Placeholder)</div>;
const DashboardPage = () => <div style={{ padding: '20px' }}>Dashboard Page (Placeholder)</div>;

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<AuthIdentifierPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/otp" element={<OtpPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
        </Routes>
    );
}