import React from 'react';
import {Navigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';

/**
 * ProtectedRoute Component
 * Wraps routes that require authentication
 */
const ProtectedRoute = ({children}) => {
    const {isAuthenticated, isLoading} = useAuth();

    // Show loading state while checking auth
    if (isLoading) {
        return (
            <div
                className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 to-slate-900">
                <div className="text-center">
                    <div
                        className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-slate-700 border-t-blue-500"></div>
                    <p className="text-slate-400">Loading...</p>
                </div>
            </div>
        );
    }

    // Redirect to log in if not authenticated
    if (!isAuthenticated) {
        return <Navigate to="/" replace/>;
    }

    // Render protected content
    return children;
};

export default ProtectedRoute;
