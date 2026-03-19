import React from 'react';

/**
 * Modern Auth Layout with Sidebar, Topbar, and centered Auth Card
 * Glassmorphism + Gradient Design System
 *
 * Features:
 * - Fixed sidebar on the left
 * - Sticky topbar with blur effect
 * - Centered auth container
 * - Premium SaaS aesthetic
 */
export const AuthLayoutModern = ({children, title, subtitle}) => {
    return (
        <div className="auth-shell">
            {/* Sidebar */}
            <aside className="auth-sidebar">
                <div className="auth-sidebar-brand">
                    <span className="text-xl">▣</span>
                    <span>Dragon of North</span>
                </div>
                <nav className="auth-sidebar-nav">
                    <a href="/">Home</a>
                    <a href="/sessions">Sessions</a>
                </nav>
            </aside>

            {/* Main Content */}
            <div className="auth-main">
                {/* Topbar */}
                <header className="auth-topbar">
                    <button className="auth-topbar-pill">
                        Authentication System
                    </button>
                </header>

                {/* Auth Container */}
                <section className="auth-container">
                    <div className="auth-card">
                        {title && <h1 className="auth-title">{title}</h1>}
                        {subtitle && <p className="auth-subtitle">{subtitle}</p>}
                        <div className="auth-card-content">
                            {children}
                        </div>
                    </div>
                </section>
            </div>
        </div>
    );
};


