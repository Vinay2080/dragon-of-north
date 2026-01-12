import { useState } from "react";
import { useNavigate } from "react-router-dom";

export default function UsernamePage() {
    const [username, setUsername] = useState("");
    const navigate = useNavigate();

    const submit = () => {
        if (!username.trim()) return;
        navigate("/a", { state: { username } });
    };

    return (
        <div style={{ minHeight: "100vh", display: "grid", placeItems: "center" }}>
            <div>
                <h2>Enter username</h2>
                <input
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="dragonlord"
                />
                <br /><br />
                <button onClick={submit}>Continue</button>
            </div>
        </div>
    );
}
