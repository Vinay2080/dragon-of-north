import { useLocation } from "react-router-dom";

export default function PageA() {
    const { state } = useLocation();

    return (
        <div style={{ minHeight: "100vh", display: "grid", placeItems: "center" }}>
            <h1>Welcome {state?.username ?? "Guest"} 🔥</h1>
        </div>
    );
}
