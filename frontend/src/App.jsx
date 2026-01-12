import { Routes, Route } from "react-router-dom";
import UsernamePage from "./pages/UsernamePage";
import PageA from "./pages/PageA.jsx";

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<UsernamePage />} />
            <Route path="/a" element={<PageA />} />
        </Routes>
    );
}