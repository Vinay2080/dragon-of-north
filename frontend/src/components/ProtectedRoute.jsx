import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/authUtils';

/**
 * ProtectedRoute Component
 * Wraps routes that require authentication
 */
const ProtectedRoute = ({children}) => {
    const {isAuthenticated, isLoading} = useAuth();
    const navigate = useNavigate();
    const [isGateOpen, setIsGateOpen] = useState(true);


    useEffect(() => {
        if (!isGateOpen) {
            return;
        }

        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        return () => {
            document.body.style.overflow = originalOverflow;
        };
    }, [isGateOpen]);

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
        return (
            <div className="protected-gate">
                <div
                    className={isGateOpen ? 'protected-gate__content protected-gate__content--blurred' : 'protected-gate__content'}>
                    {children}
                </div>

                {isGateOpen && (
                    <div className="protected-gate__overlay" onClick={() => setIsGateOpen(false)}>
                        <div className="protected-gate__modal" onClick={(event) => event.stopPropagation()}>
                            <h2 className="protected-gate__title">Login Required</h2>
                            <p className="protected-gate__description">You need to be logged in to view your
                                sessions.</p>
                            <div className="protected-gate__actions">
                                <button type="button" className="btn-primary" onClick={() => navigate('/login')}>Login
                                </button>
                                <button type="button" className="btn-subtle"
                                        onClick={() => setIsGateOpen(false)}>Cancel
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        );
    }

    // Render protected content
    return children;
};

export default ProtectedRoute;
