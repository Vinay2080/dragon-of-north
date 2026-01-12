import { Routes, Route } from "react-router-dom";
import AuthIdentifierPage from "./pages/AuthIdentifierPage";
import SignupPage from "./pages/SignupPage";
import OtpPage from "./pages/OtpPage";
import LoginPage from "./pages/LoginPage";

const DashboardPage = () => <div style={{ padding: '20px' }}>Dashboard Page (Placeholder)</div>;

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<AuthIdentifierPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/otp" element={<OtpPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/dashboard" element={<DashboardPage />} />
        </Routes>
    );
}