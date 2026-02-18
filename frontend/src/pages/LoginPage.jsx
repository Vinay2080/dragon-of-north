import React, {useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {API_CONFIG} from '../config';
import {apiService} from '../services/apiService';
import {useAuth} from '../context/authUtils';
import RateLimitInfo from '../components/RateLimitInfo';

const LoginPage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const {login} = useAuth();
    const { identifier: initialIdentifier } = location.state || {};

    const [identifier, setIdentifier] = useState(initialIdentifier || '');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.LOGIN);

            if (result.api_response_status === 'success') {
                // Set auth state
                login({identifier});
                navigate('/dashboard');
            } else {
                setError(result.message || 'Login failed. Please check your credentials.');
            }
        } catch (err) {
            if (err.type === 'RATE_LIMIT_EXCEEDED') {
                setError(`Too many login attempts. Please wait ${err.retryAfter} seconds before trying again.`);
            } else if (err.type === 'API_ERROR') {
                setError(err.message || 'Login failed. Please check your credentials.');
            } else {
                setError('Failed to connect to the server. Please try again later.');
            }
            console.error('Login Error:', err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
            <div className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-8 shadow-2xl">
                <h2 className="text-2xl font-bold text-white">
                    Login
                </h2>

                <p className="mt-1 mb-6 text-sm text-slate-400">
                    Enter your credentials to access your account
                </p>

                <form onSubmit={handleLogin} noValidate>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">
                                Email or Phone
                            </label>
                            <input
                                type="text"
                                value={identifier}
                                onChange={(e) => setIdentifier(e.target.value)}
                                placeholder="Email or phone number"
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-slate-400 mb-1">
                                Password
                            </label>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Enter password"
                                className="w-full rounded-lg border border-slate-700 bg-slate-900 px-4 py-3 text-white placeholder-slate-500 focus:border-blue-500 focus:outline-none"
                                required
                            />
                        </div>

                        {error && (
                            <p className="mt-3 text-sm text-red-400">
                                {error}
                            </p>
                        )}

                        <button
                            type="submit"
                            disabled={loading || !identifier || !password}
                            className="w-full rounded-lg bg-blue-600 py-3 font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {loading ? 'Logging in...' : 'Login'}
                        </button>
                    </div>

                    <RateLimitInfo/>
                </form>

                <div className="mt-6 text-center">
                    <button
                        onClick={() => navigate('/')}
                        className="text-xs text-slate-500 hover:text-white transition"
                    >
                        Back to Welcome
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
