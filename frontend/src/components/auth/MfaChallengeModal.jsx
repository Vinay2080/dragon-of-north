import React, {useEffect, useMemo, useRef, useState} from 'react';
import {createPortal} from 'react-dom';
import AuthButton from './AuthButton';
import AuthInput from './AuthInput';
import OtpInput from './OtpInput';
import ValidationError from '../Validation/ValidationError';

const METHOD_LABELS = {
    TOTP: 'Authenticator app',
    RECOVERY_CODE: 'Recovery code',
};

const COPY_BY_MODE = {
    'login': {
        title: 'Verify it’s you',
        subtitle: 'Enter a verification code to finish signing in.',
        submit: 'Verify and Sign In',
    },
    'step-up': {
        title: 'Verify Identity',
        subtitle: 'Enter a verification code to confirm this action.',
        submit: 'Verify Identity',
    },
};

const TOTP_LENGTH = 6;
const EMPTY_TOTP = Array(TOTP_LENGTH).fill('');

const normalizeMethods = (methods = []) => {
    const supported = methods.filter((method) => method === 'TOTP' || method === 'RECOVERY_CODE');
    return supported.length > 0 ? supported : ['TOTP'];
};

const MfaChallengeModal = ({
                               open,
                               mode = 'login',
                               availableMethods = [],
                               error = '',
                               isSubmitting = false,
                               onCancel,
                               onSubmit,
                           }) => {
    const methods = useMemo(() => normalizeMethods(availableMethods), [availableMethods]);
    const [selectedMethod, setSelectedMethod] = useState(methods[0]);
    const [totpDigits, setTotpDigits] = useState(EMPTY_TOTP);
    const [recoveryCode, setRecoveryCode] = useState('');
    const [shakeKey, setShakeKey] = useState(0);
    const lastErrorRef = useRef('');

    useEffect(() => {
        if (open) {
            setSelectedMethod(methods[0]);
            setTotpDigits(EMPTY_TOTP);
            setRecoveryCode('');
            lastErrorRef.current = '';
        }
    }, [open, methods]);

    useEffect(() => {
        if (error && error !== lastErrorRef.current) {
            lastErrorRef.current = error;
            setShakeKey((k) => k + 1);
            if (selectedMethod === 'TOTP') {
                setTotpDigits(EMPTY_TOTP);
            }
        }
        if (!error) {
            lastErrorRef.current = '';
        }
    }, [error, selectedMethod]);

    if (!open) {
        return null;
    }

    const resolvedMethod = methods.includes(selectedMethod) ? selectedMethod : methods[0];
    const isRecoveryCode = resolvedMethod === 'RECOVERY_CODE';
    const copy = COPY_BY_MODE[mode] || COPY_BY_MODE.login;

    const totpValue = totpDigits.join('');
    const trimmedRecovery = recoveryCode.trim();
    const submitValue = isRecoveryCode ? trimmedRecovery : totpValue;
    const canSubmit = isRecoveryCode
        ? trimmedRecovery.length > 0
        : totpValue.length === TOTP_LENGTH;

    const fireSubmit = (code) => {
        if (!code || isSubmitting) return;
        onSubmit?.({providerType: resolvedMethod, code});
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        if (!canSubmit) return;
        fireSubmit(submitValue);
    };

    const handleTotpChange = (nextDigits) => {
        setTotpDigits(nextDigits);
    };

    const handleTotpComplete = (fullCode) => {
        if (isSubmitting || fullCode.length !== TOTP_LENGTH) return;
        fireSubmit(fullCode);
    };

    const handleMethodSwitch = (method) => {
        if (isSubmitting || method === resolvedMethod) return;
        setSelectedMethod(method);
        setTotpDigits(EMPTY_TOTP);
        setRecoveryCode('');
    };

    const modalContent = (
        <div
            className="fixed inset-0 z-[9999] flex items-center justify-center p-4"
            role="dialog"
            aria-modal="true"
            aria-labelledby="mfa-challenge-title"
        >
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-[6px] transition-opacity duration-200"
                onClick={isSubmitting ? undefined : onCancel}
                aria-hidden="true"
            />
            <div
                key={shakeKey}
                className={`relative w-full max-w-md rounded-3xl border border-slate-200/80 bg-white p-6 shadow-[0_24px_50px_rgba(15,23,42,0.24)] transition-all duration-200 dark:border-slate-700/70 dark:bg-slate-900 ${error ? 'mfa-shake' : ''}`}
            >
                <div className="mb-5">
                    <h3 id="mfa-challenge-title" className="text-lg font-semibold text-slate-900 dark:text-slate-100">
                        {copy.title}
                    </h3>
                    <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">
                        {copy.subtitle}
                    </p>
                </div>

                <form className="auth-form-stack" onSubmit={handleSubmit}>
                    {methods.length > 1 && (
                        <div>
                            <label className="auth-label">Verification method</label>
                            <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                                {methods.map((method) => (
                                    <button
                                        key={method}
                                        type="button"
                                        disabled={isSubmitting}
                                        onClick={() => handleMethodSwitch(method)}
                                        className={`rounded-2xl border px-3 py-2 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
                                            resolvedMethod === method
                                                ? 'border-violet-400 bg-violet-50 text-violet-700 dark:border-violet-400/70 dark:bg-violet-500/15 dark:text-violet-200'
                                                : 'border-slate-300/80 bg-white text-slate-700 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700'
                                        }`}
                                    >
                                        {METHOD_LABELS[method] || method}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    <div>
                        <label className="auth-label">
                            {isRecoveryCode ? 'Recovery code' : 'Authenticator code'}
                        </label>
                        {isRecoveryCode ? (
                            <AuthInput
                                type="text"
                                value={recoveryCode}
                                onChange={(event) => setRecoveryCode(event.target.value)}
                                placeholder="Enter recovery code"
                                autoComplete="one-time-code"
                                inputMode="text"
                                disabled={isSubmitting}
                                hasError={Boolean(error)}
                                autoFocus
                            />
                        ) : (
                            <OtpInput
                                value={totpDigits}
                                onChange={handleTotpChange}
                                length={TOTP_LENGTH}
                                idPrefix="mfa-totp"
                                disabled={isSubmitting}
                                error={Boolean(error)}
                                autoSubmit
                                autoFocus
                                onComplete={handleTotpComplete}
                            />
                        )}
                        <ValidationError errors={error ? [error] : []}/>
                    </div>

                    <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                        <button
                            type="button"
                            onClick={onCancel}
                            disabled={isSubmitting}
                            className="h-10 rounded-2xl border border-slate-300/80 bg-white px-4 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                        >
                            Cancel
                        </button>
                        <AuthButton
                            type="submit"
                            loading={isSubmitting}
                            disabled={!canSubmit || isSubmitting}
                            className="sm:w-auto"
                        >
                            {copy.submit}
                        </AuthButton>
                    </div>
                </form>
            </div>
        </div>
    );

    return createPortal(modalContent, document.body);
};

export default MfaChallengeModal;
