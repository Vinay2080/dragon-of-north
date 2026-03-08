import React from "react";
import ReactDOM from "react-dom/client";
import {BrowserRouter} from "react-router-dom";
import App from "./App";
import {ThemeProvider} from './context/ThemeContext.tsx';
import './index.css';


ReactDOM.createRoot(document.getElementById("root")).render(
    <ThemeProvider>
        <BrowserRouter>
            <App/>
        </BrowserRouter>
    </ThemeProvider>
);
