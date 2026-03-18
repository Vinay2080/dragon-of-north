import {useState} from 'react';
import {AUTH_STATE} from '../constants/authState';

/**
 * useAuthState - Comprehensive auth state handling hook
 * Handles: loading, success, error states with proper UX
 */
export function useAuthState() {
    const [state, setState] = useState(AUTH_STATE.IDLE);
    const [message, setMessage] = useState('');

    const setIdle = () => {
        setState(AUTH_STATE.IDLE);
        setMessage('');
    };

    const setLoading = (msg = 'Authenticating...') => {
        setState(AUTH_STATE.LOADING);
        setMessage(msg);
    };

    const setSuccess = (msg = 'Success!') => {
        setState(AUTH_STATE.SUCCESS);
        setMessage(msg);
    };

    const setError = (msg = 'Could not complete action. Please try again.') => {
        setState(AUTH_STATE.ERROR);
        setMessage(msg);
    };

    const reset = () => {
        setState(AUTH_STATE.IDLE);
        setMessage('');
    };

    return {
        state,
        message,
        setIdle,
        setLoading,
        setSuccess,
        setError,
        reset,
        isLoading: state === AUTH_STATE.LOADING,
        isSuccess: state === AUTH_STATE.SUCCESS,
        isError: state === AUTH_STATE.ERROR,
    };
}

