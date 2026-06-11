import React, {useCallback, useEffect, useState} from 'react';
import {Link, useNavigate} from 'react-router-dom';
import {ArrowLeft, Check, Copy, Download, ShieldCheck} from 'lucide-react';
import AuthButton from '../components/auth/AuthButton';
import OtpInput from '../components/auth/OtpInput';
import {apiService} from '../services/apiService';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {useToast} from '../hooks/useToast';
import {useAuth} from '../context/authUtils';
import {useDocumentTitle} from '../hooks/useDocumentTitle';

const STEP = {
    IDLE: 'IDLE',
    REQUEST: 'REQUEST',
    VERIFY: 'VERIFY',
    RECOVERY: 'RECOVERY',
    DONE: 'DONE',
};

const TOTP_LENGTH = 6;
const EMPTY_TOTP = Array(TOTP_LENGTH).fill('');

const MfaManagementPage = () => {
    useDocumentTitle('Manage MFA');
    const navigate = useNavigate();
    const {toast} = useToast();
    const {user, patchUser} = useAuth();
    const mfaEnabled = Boolean(user?.mfaEnabled);

    const [step, setStep] = useState(STEP.IDLE);
    const [isBusy, setIsBusy] = useState(false);
    const [error, setError] = useState('');

    const [mfaSecret, setMfaSecret] = useState('');
    const [mfaQrCode, setMfaQrCode] = useState('');
    const [copiedSecret, setCopiedSecret] = useState(false);

    const [totpDigits, setTotpDigits] = useState(EMPTY_TOTP);

    const [backupCodes, setBackupCodes] = useState([]);
    const [copiedCodes, setCopiedCodes] = useState(false);
    const [savedConfirmed, setSavedConfirmed] = useState(false);

    const resetSetupState = useCallback(() => {
        setStep(STEP.IDLE);
        setError('');
        setMfaSecret('');
        setMfaQrCode('');
        setTotpDigits(EMPTY_TOTP);
        setBackupCodes([]);
        setSavedConfirmed(false);
        setCopiedSecret(false);
        setCopiedCodes(false);
    }, []);

    const startSetup = useCallback(async () => {
        setStep(STEP.REQUEST);
        setIsBusy(true);
        setError('');

        const result = await apiService.post(API_CONFIG.ENDPOINTS.MFA_SETUP_REQUEST, {
            deviceId: getDeviceId(),
        });

        setIsBusy(false);

        if (apiService.isErrorResponse(result)) {
            setError(result?.backendMessage || result?.message || 'Failed to initialize MFA setup.');
            setStep(STEP.IDLE);
            return;
        }

        const data = result?.data || result;
        const secret = data?.mfa_secret || data?.mfaSecret;
        const qrCode = data?.mfa_qr_code || data?.mfaQrCode;
        if (secret && qrCode) {
            setMfaSecret(secret);
            setMfaQrCode(qrCode);
            setStep(STEP.VERIFY);
            setTotpDigits(EMPTY_TOTP);
        } else {
            setError('Invalid response from server.');
            setStep(STEP.IDLE);
        }
    }, []);

    const verifySetup = useCallback(async (fullCode) => {
        if (isBusy || fullCode.length !== TOTP_LENGTH) return;

        setIsBusy(true);
        setError('');

        const result = await apiService.post(API_CONFIG.ENDPOINTS.MFA_SETUP_CONFIRM, {
            deviceId: getDeviceId(),
            code: fullCode,
        });

        setIsBusy(false);

        if (apiService.isErrorResponse(result)) {
            setError(result?.backendMessage || result?.message || 'Invalid code. Please try again.');
            setTotpDigits(EMPTY_TOTP);
            return;
        }

        const data = result?.data || result;
        const codes = data?.backup_codes || data?.backupCodes || data?.recovery_codes || data?.recoveryCodes;
        if (Array.isArray(codes)) {
            setBackupCodes(codes);
            setStep(STEP.RECOVERY);
            toast.success('Authenticator app verified.');
        } else {
            setError('Failed to retrieve recovery codes.');
        }
    }, [isBusy, toast]);

    const handleTotpComplete = useCallback((fullCode) => {
        void verifySetup(fullCode);
    }, [verifySetup]);

    const handleVerifySubmit = (event) => {
        event.preventDefault();
        const fullCode = totpDigits.join('');
        void verifySetup(fullCode);
    };

    const handleCopySecret = () => {
        navigator.clipboard.writeText(mfaSecret);
        setCopiedSecret(true);
        setTimeout(() => setCopiedSecret(false), 2000);
    };

    const handleCopyCodes = () => {
        navigator.clipboard.writeText(backupCodes.join('\n'));
        setCopiedCodes(true);
        setTimeout(() => setCopiedCodes(false), 2000);
    };

    const handleDownloadCodes = () => {
        const text = backupCodes.join('\n');
        const blob = new Blob([text], {type: 'text/plain'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'dragon-of-north-recovery-codes.txt';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    const handleComplete = () => {
        if (!savedConfirmed) return;
        patchUser({mfaEnabled: true});
        toast.success('MFA has been enabled on your account.');
        setStep(STEP.DONE);
    };

    useEffect(() => {
        if (step === STEP.RECOVERY) {
            const beforeUnload = (event) => {
                if (!savedConfirmed) {
                    event.preventDefault();
                    event.returnValue = '';
                }
            };
            window.addEventListener('beforeunload', beforeUnload);
            return () => window.removeEventListener('beforeunload', beforeUnload);
        }
        return undefined;
    }, [step, savedConfirmed]);

    const renderHeader = () => (
        <div className="mb-6 flex flex-col gap-3">
            <Link
                to="/profile"
                className="inline-flex w-fit items-center gap-2 text-sm font-medium text-slate-600 transition-colors hover:text-violet-600 dark:text-slate-300 dark:hover:text-violet-300"
            >
                <ArrowLeft className="h-4 w-4"/> Back to profile
            </Link>
            <div className="flex items-start gap-3">
                <div
                    className="flex h-11 w-11 items-center justify-center rounded-xl border border-violet-200/70 bg-gradient-to-br from-violet-500/20 to-indigo-500/15 text-violet-700 shadow-sm dark:border-violet-400/30 dark:from-violet-500/20 dark:to-indigo-500/15 dark:text-violet-200">
                    <ShieldCheck className="h-5 w-5"/>
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-50">Multi-factor
                        authentication</h1>
                    <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        Add an extra layer of security to your account by requiring a code from your authenticator app
                        at sign-in.
                    </p>
                </div>
            </div>
        </div>
    );

    const renderIdle = () => {
        if (mfaEnabled || step === STEP.DONE) {
            return (
                <div
                    className="rounded-2xl border border-emerald-200/70 bg-emerald-50/60 p-5 dark:border-emerald-500/25 dark:bg-emerald-500/10">
                    <div className="flex items-center gap-3">
                        <div
                            className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-700 dark:text-emerald-300">
                            <Check className="h-5 w-5"/>
                        </div>
                        <div>
                            <p className="font-semibold text-emerald-900 dark:text-emerald-200">MFA is enabled</p>
                            <p className="mt-0.5 text-sm text-emerald-800/80 dark:text-emerald-200/80">
                                You will be asked for a verification code each time you sign in.
                            </p>
                        </div>
                    </div>
                </div>
            );
        }

        return (
            <div
                className="rounded-2xl border border-slate-200/80 bg-white/80 p-5 shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
                <p className="text-sm text-slate-600 dark:text-slate-300">
                    You will need an authenticator app (Google Authenticator, Authy, 1Password, etc.). Setup takes about
                    a minute.
                </p>
                <AuthButton
                    type="button"
                    onClick={startSetup}
                    disabled={isBusy}
                    loading={isBusy && step === STEP.REQUEST}
                    className="mt-4 sm:w-auto"
                >
                    Set up MFA
                </AuthButton>
                {error && <p className="mt-3 text-sm text-red-600 dark:text-red-400">{error}</p>}
            </div>
        );
    };

    const renderVerify = () => (
        <div
            className="rounded-2xl border border-slate-200/80 bg-white/80 p-5 shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
            <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Step 1 — Scan and verify</h2>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
                Scan this QR code in your authenticator app, then enter the 6-digit code below.
            </p>

            <div className="mt-4 flex flex-col items-center gap-4 sm:flex-row sm:items-start">
                <div className="flex justify-center rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
                    {mfaQrCode ? (
                        <img src={mfaQrCode} alt="MFA QR Code" className="h-44 w-44 object-contain"/>
                    ) : (
                        <div
                            className="flex h-44 w-44 items-center justify-center bg-slate-100 p-4 text-center text-xs text-slate-400">
                            QR code not available
                        </div>
                    )}
                </div>
                <div className="flex-1 space-y-2">
                    <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">Can’t scan? Enter
                        this key:</label>
                    <div className="flex items-center gap-2">
                        <code
                            className="block flex-1 break-all rounded-lg border border-slate-200 bg-slate-100 p-2.5 text-sm font-mono text-slate-800 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200">
                            {mfaSecret}
                        </code>
                        <button
                            type="button"
                            onClick={handleCopySecret}
                            className="rounded-lg border border-slate-200 bg-white p-2.5 text-slate-500 transition-colors hover:border-teal-300 hover:text-teal-600 dark:border-slate-700 dark:bg-slate-900"
                            title="Copy secret key"
                        >
                            {copiedSecret ? <Check className="h-4 w-4 text-emerald-500"/> : <Copy className="h-4 w-4"/>}
                        </button>
                    </div>
                </div>
            </div>

            <form className="mt-5 space-y-3 border-t border-slate-100 pt-4 dark:border-slate-800"
                  onSubmit={handleVerifySubmit}>
                <label className="auth-label">Enter the 6-digit code from your app</label>
                <OtpInput
                    value={totpDigits}
                    onChange={setTotpDigits}
                    length={TOTP_LENGTH}
                    idPrefix="mfa-setup"
                    disabled={isBusy}
                    error={Boolean(error)}
                    autoFocus
                    autoSubmit
                    onComplete={handleTotpComplete}
                />
                {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}
                <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                    <button
                        type="button"
                        onClick={resetSetupState}
                        disabled={isBusy}
                        className="h-10 rounded-2xl border border-slate-300/80 bg-white px-4 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200"
                    >
                        Cancel
                    </button>
                    <AuthButton
                        type="submit"
                        loading={isBusy}
                        disabled={totpDigits.join('').length !== TOTP_LENGTH || isBusy}
                        className="sm:w-auto"
                    >
                        Verify and Continue
                    </AuthButton>
                </div>
            </form>
        </div>
    );

    const renderRecovery = () => (
        <div
            className="rounded-2xl border border-slate-200/80 bg-white/80 p-5 shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
            <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">Step 2 — Save your recovery
                codes</h2>
            <div
                className="mt-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-200">
                <p className="font-semibold">Store these codes somewhere safe.</p>
                <p className="mt-0.5 text-xs opacity-90">
                    If you lose access to your authenticator app, these one-time codes are the only way back into your
                    account.
                </p>
            </div>

            <div
                className="mt-4 grid max-h-56 grid-cols-2 gap-2 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900/50">
                {backupCodes.map((bc, idx) => (
                    <code
                        key={idx}
                        className="rounded border border-slate-100 bg-white px-2 py-1.5 text-center text-sm font-mono text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300"
                    >
                        {bc}
                    </code>
                ))}
            </div>

            <div className="mt-4 flex gap-2">
                <button
                    type="button"
                    onClick={handleCopyCodes}
                    className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                >
                    {copiedCodes ? <Check className="h-4 w-4 text-emerald-500"/> : <Copy className="h-4 w-4"/>}
                    {copiedCodes ? 'Copied!' : 'Copy codes'}
                </button>
                <button
                    type="button"
                    onClick={handleDownloadCodes}
                    className="flex flex-1 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
                >
                    <Download className="h-4 w-4"/>
                    Download
                </button>
            </div>

            <div className="mt-5 border-t border-slate-100 pt-4 dark:border-slate-800">
                <label className="group flex cursor-pointer items-start gap-3">
                    <input
                        type="checkbox"
                        checked={savedConfirmed}
                        onChange={(event) => setSavedConfirmed(event.target.checked)}
                        className="mt-1 h-4 w-4 rounded border-slate-300 text-teal-600 focus:ring-teal-500"
                    />
                    <span className="text-sm text-slate-700 dark:text-slate-300">
                        I have safely recorded these recovery codes.
                    </span>
                </label>

                <AuthButton
                    type="button"
                    onClick={handleComplete}
                    disabled={!savedConfirmed}
                    className="mt-4 sm:w-auto"
                >
                    Finish setup
                </AuthButton>
            </div>
        </div>
    );

    const renderDone = () => (
        <div className="space-y-4">
            {renderIdle()}
            <button
                type="button"
                onClick={() => navigate('/profile')}
                className="auth-secondary-btn"
            >
                Back to security settings
            </button>
        </div>
    );

    return (
        <div className="mx-auto max-w-2xl px-4 py-8">
            {renderHeader()}
            {step === STEP.IDLE && renderIdle()}
            {step === STEP.REQUEST && (
                <div
                    className="rounded-2xl border border-slate-200/80 bg-white/80 p-5 shadow-sm dark:border-slate-700/70 dark:bg-slate-900/60">
                    <span className="btn-loading-indicator text-slate-500">
                        <span className="spinner spinner-sm"></span>
                        <span>Initializing setup...</span>
                    </span>
                </div>
            )}
            {step === STEP.VERIFY && renderVerify()}
            {step === STEP.RECOVERY && renderRecovery()}
            {step === STEP.DONE && renderDone()}
        </div>
    );
};

export default MfaManagementPage;
