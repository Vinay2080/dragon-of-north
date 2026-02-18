import {Route, Routes} from "react-router-dom";
import {AuthProvider} from "./context/AuthContext.jsx";
import ProtectedRoute from "./components/ProtectedRoute";
import AuthIdentifierPage from "./pages/AuthIdentifierPage";
import SignupPage from "./pages/SignupPage";
import OtpPage from "./pages/OtpPage";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";

export default function App() {
    return (
        <AuthProvider>
            <Routes>
                <Route path="/" element={<AuthIdentifierPage/>}/>
                <Route path="/signup" element={<SignupPage/>}/>
                <Route path="/otp" element={<OtpPage/>}/>
                <Route path="/login" element={<LoginPage/>}/>
                <Route
                    path="/dashboard"
                    element={
                        <ProtectedRoute>
                            <DashboardPage/>
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </AuthProvider>
    );
}