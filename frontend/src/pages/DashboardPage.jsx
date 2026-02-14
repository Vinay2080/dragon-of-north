import React, {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';

const DashboardPage = () => {
    const navigate = useNavigate();
    const {user, logout} = useAuth();
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    const handleLogout = async () => {
        setIsLoggingOut(true);
        try {
            await logout();
            navigate('/', {replace: true});
        } catch (error) {
            console.error('Logout failed:', error);
            // Still navigate away even if logout API fails
            navigate('/', {replace: true});
        } finally {
            setIsLoggingOut(false);
        }
    };

    return (
        <div className="min-h-screen w-full bg-gradient-to-br from-slate-950 to-slate-900">
            {/* Header */}
            <header className="border-b border-slate-800 bg-slate-950/50 backdrop-blur-sm">
                <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between">
                        <div>
                            <h1 className="text-2xl font-bold text-white">Dashboard</h1>
                            <p className="text-sm text-slate-400">Welcome back!</p>
                        </div>
                        <button
                            onClick={handleLogout}
                            disabled={isLoggingOut}
                            className="rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {isLoggingOut ? (
                                <span className="flex items-center gap-2">
                                    <svg className="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
                                        <circle
                                            className="opacity-25"
                                            cx="12"
                                            cy="12"
                                            r="10"
                                            stroke="currentColor"
                                            strokeWidth="4"
                                        />
                                        <path
                                            className="opacity-75"
                                            fill="currentColor"
                                            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                        />
                                    </svg>
                                    Logging out...
                                </span>
                            ) : (
                                <span className="flex items-center gap-2">
                                    <svg
                                        className="h-4 w-4"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                                        />
                                    </svg>
                                    Logout
                                </span>
                            )}
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
                {/* User Info Card */}
                <div className="mb-8 rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                    <h2 className="mb-4 text-xl font-semibold text-white">Account Information</h2>
                    <div className="space-y-3">
                        <div className="flex items-center gap-3 rounded-lg border border-slate-800 bg-slate-900/50 p-4">
                            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-600">
                                <svg
                                    className="h-6 w-6 text-white"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                                    />
                                </svg>
                            </div>
                            <div>
                                <p className="text-sm text-slate-400">Identifier</p>
                                <p className="font-medium text-white">
                                    {user?.identifier || 'Not available'}
                                </p>
                            </div>
                        </div>

                        <div className="flex items-center gap-3 rounded-lg border border-slate-800 bg-slate-900/50 p-4">
                            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-600">
                                <svg
                                    className="h-6 w-6 text-white"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                                    />
                                </svg>
                            </div>
                            <div>
                                <p className="text-sm text-slate-400">Status</p>
                                <p className="font-medium text-green-400">Active</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Stats Grid */}
                <div className="grid gap-6 md:grid-cols-3">
                    <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                        <div className="mb-2 flex items-center justify-between">
                            <h3 className="text-sm font-medium text-slate-400">Session Active</h3>
                            <div className="rounded-full bg-green-600/20 p-2">
                                <div className="h-2 w-2 rounded-full bg-green-500"></div>
                            </div>
                        </div>
                        <p className="text-2xl font-bold text-white">Connected</p>
                    </div>

                    <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                        <h3 className="mb-2 text-sm font-medium text-slate-400">Last Login</h3>
                        <p className="text-2xl font-bold text-white">Just now</p>
                    </div>

                    <div className="rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-2xl">
                        <h3 className="mb-2 text-sm font-medium text-slate-400">Account Type</h3>
                        <p className="text-2xl font-bold text-white">Standard</p>
                    </div>
                </div>

                {/* Welcome Message */}
                <div className="mt-8 rounded-2xl border border-slate-800 bg-slate-950 p-8 text-center shadow-2xl">
                    <div
                        className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-blue-600/20">
                        <svg
                            className="h-8 w-8 text-blue-500"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2h-.095c-.5 0-.905.405-.905.905 0 .714-.211 1.412-.608 2.006L7 11v9m7-10h-2M7 20H5a2 2 0 01-2-2v-6a2 2 0 012-2h2.5"
                            />
                        </svg>
                    </div>
                    <h2 className="mb-2 text-2xl font-bold text-white">
                        You're all set!
                    </h2>
                    <p className="text-slate-400">
                        Your dashboard is ready. You're successfully authenticated and can now access all features.
                    </p>
                </div>
            </main>
        </div>
    );
};

export default DashboardPage;
