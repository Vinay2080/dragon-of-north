import React from 'react';
import {Navigate, useLocation} from 'react-router-dom';
import {useAuth} from '../context/authUtils';

/**
 * ProtectedRoute Component
 * Wraps routes that require authentication
 */
const ProtectedRoute = ({children}) => {
    const {isAuthenticated, isLoading} = useAuth();
    const location = useLocation();

    // Show loading state while checking auth
    if (isLoading) {
        return (
            <div className="protected-loading">
                <div className="text-center">
                    <div
                        className="protected-loading__spinner mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4"></div>
                    <p className="protected-loading__text">Loading...</p>
                </div>
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{from: location}} replace/>;
    }

    // Render protected content
    return children;
};

export default ProtectedRoute;
