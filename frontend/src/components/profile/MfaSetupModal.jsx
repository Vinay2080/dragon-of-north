import React, {useState, useEffect} from 'react';
import {X, Copy, Check, Download} from 'lucide-react';
import {apiService} from '../../services/apiService';
import {API_CONFIG} from '../../config';
import {getDeviceId} from '../../utils/device';
import {useToast} from '../../hooks/useToast';
import AuthButton from '../auth/AuthButton';
import AuthInput from '../auth/AuthInput';

const STEP_REQUEST = 'REQUEST';
const STEP_VERIFY = 'VERIFY';
const STEP_RECOVERY = 'RECOVERY';

const MfaSetupModal = ({isOpen, onClose, onComplete}) => {
    const {toast} = useToast();
    const [step, setStep] = useState(STEP_REQUEST);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    
    // Step 1 Data
    const [mfaSecret, setMfaSecret] = useState('');
    const [mfaQrCode, setMfaQrCode] = useState('');
    const [code, setCode] = useState('');
    const [copiedSecret, setCopiedSecret] = useState(false);
    
    // Step 2 Data
    const [backupCodes, setBackupCodes] = useState([]);
    const [copiedCodes, setCopiedCodes] = useState(false);
    const [savedConfirmed, setSavedConfirmed] = useState(false);

    useEffect(() => {
        if (!isOpen) return;
        
        let isMounted = true;
        
        const fetchSetupData = async () => {
            setStep(STEP_REQUEST);
            setIsLoading(true);
            setError('');
            
            try {
                const result = await apiService.post(API_CONFIG.ENDPOINTS.MFA_SETUP_REQUEST, {
                    deviceId: getDeviceId()
                });
                
                if (!isMounted) return;
                
                if (apiService.isErrorResponse(result)) {
                    setError(result?.backendMessage || result?.message || 'Failed to initialize MFA setup.');
                    return;
                }
                
                const data = result?.data || result;
                if (data.mfaSecret && data.mfaQrCode) {
                    setMfaSecret(data.mfaSecret);
                    setMfaQrCode(data.mfaQrCode);
                    setStep(STEP_VERIFY);
                } else {
                    setError('Invalid response from server.');
                }
            } catch (err) {
                if (isMounted) setError('An unexpected error occurred.');
            } finally {
                if (isMounted) setIsLoading(false);
            }
        };

        void fetchSetupData();

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        
        return () => {
            isMounted = false;
            document.body.style.overflow = previousOverflow;
            // Reset state
            setStep(STEP_REQUEST);
            setCode('');
            setBackupCodes([]);
            setSavedConfirmed(false);
            setError('');
        };
    }, [isOpen]);

    const handleCopySecret = () => {
        navigator.clipboard.writeText(mfaSecret);
        setCopiedSecret(true);
        setTimeout(() => setCopiedSecret(false), 2000);
    };

    const handleVerify = async (e) => {
        e.preventDefault();
        if (isLoading || code.length !== 6) return;
        
        setIsLoading(true);
        setError('');
        
        try {
            const result = await apiService.post(API_CONFIG.ENDPOINTS.MFA_SETUP_CONFIRM, {
                deviceId: getDeviceId(),
                code: code
            });
            
            if (apiService.isErrorResponse(result)) {
                setError(result?.backendMessage || result?.message || 'Invalid code. Please try again.');
                setIsLoading(false);
                return;
            }
            
            const data = result?.data || result;
            if (data.backupCodes && Array.isArray(data.backupCodes)) {
                setBackupCodes(data.backupCodes);
                setStep(STEP_RECOVERY);
                toast.success('Authenticator app verified successfully.');
            } else {
                setError('Failed to retrieve backup codes.');
            }
        } catch (err) {
            setError('An unexpected error occurred during verification.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleCopyCodes = () => {
        navigator.clipboard.writeText(backupCodes.join('\n'));
        setCopiedCodes(true);
        setTimeout(() => setCopiedCodes(false), 2000);
    };

    const handleDownloadCodes = () => {
        const text = backupCodes.join('\n');
        const blob = new Blob([text], { type: 'text/plain' });
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
        toast.success('MFA has been successfully enabled.');
        onComplete();
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="protected-gate__overlay" onClick={(e) => {
            // Prevent closing by clicking outside if they are on the recovery code step and haven't confirmed
            if (step === STEP_RECOVERY && !savedConfirmed) return;
            onClose();
        }}>
            <div className="protected-gate__modal max-w-md" onClick={(event) => event.stopPropagation()}>
                <div className="relative mb-4">
                    <h2 className="protected-gate__title">
                        {step === STEP_RECOVERY ? 'Save Recovery Codes' : 'Enable Two-Factor Authentication'}
                    </h2>
                    
                    {/* Only show close button if not in recovery step (must finish) */}
                    {step !== STEP_RECOVERY && (
                        <button
                            type="button"
                            aria-label="Close modal"
                            onClick={onClose}
                            className="absolute right-3 top-3 h-8 w-8 flex items-center justify-center rounded-full bg-red-50 text-red-500 hover:bg-red-100 hover:text-red-600 active:bg-red-200 transition-colors duration-200 shadow-sm hover:shadow-md"
                        >
                            <X className="h-4 w-4"/>
                        </button>
                    )}
                </div>

                {isLoading && step === STEP_REQUEST ? (
                    <div className="auth-section py-8 flex justify-center">
                        <span className="btn-loading-indicator text-slate-500">
                            <span className="spinner spinner-sm"></span>
                            <span>Initializing setup...</span>
                        </span>
                    </div>
                ) : step === STEP_VERIFY ? (
                    <div className="auth-section space-y-5">
                        <p className="text-sm text-slate-600 dark:text-slate-400">
                            Scan this QR code with your authenticator app (like Google Authenticator or Authy).
                        </p>
                        
                        <div className="flex justify-center bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
                            {mfaQrCode ? (
                                <img src={mfaQrCode} alt="MFA QR Code" className="w-48 h-48 object-contain" />
                            ) : (
                                <div className="w-48 h-48 bg-slate-100 flex items-center justify-center text-slate-400 text-xs text-center p-4">
                                    QR code not available
                                </div>
                            )}
                        </div>
                        
                        <div className="space-y-2">
                            <label className="text-xs font-semibold uppercase tracking-wider text-slate-500">Or enter this setup key manually:</label>
                            <div className="flex items-center gap-2">
                                <code className="flex-1 block p-2.5 bg-slate-100 dark:bg-slate-800 rounded-lg text-sm font-mono text-slate-800 dark:text-slate-200 break-all border border-slate-200 dark:border-slate-700">
                                    {mfaSecret}
                                </code>
                                <button
                                    onClick={handleCopySecret}
                                    className="p-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-500 hover:text-teal-600 hover:border-teal-300 transition-colors"
                                    title="Copy secret key"
                                >
                                    {copiedSecret ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
                                </button>
                            </div>
                        </div>

                        <form onSubmit={handleVerify} className="space-y-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                            <label className="auth-label">Enter the 6-digit code from your app</label>
                            <AuthInput
                                value={code}
                                onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
                                placeholder="000000"
                                maxLength={6}
                                hasError={Boolean(error)}
                                className="text-center tracking-[0.5em] font-mono text-lg"
                                autoComplete="one-time-code"
                            />
                            {error && <p className="text-xs text-red-500 mt-1">{error}</p>}
                            
                            <AuthButton 
                                type="submit" 
                                disabled={code.length !== 6 || isLoading}
                                className="w-full mt-2"
                            >
                                {isLoading ? (
                                    <span className="btn-loading-indicator">
                                        <span className="spinner spinner-sm"></span>
                                        <span>Verifying...</span>
                                    </span>
                                ) : 'Verify and Enable'}
                            </AuthButton>
                        </form>
                    </div>
                ) : step === STEP_RECOVERY ? (
                    <div className="auth-section space-y-5">
                        <div className="p-3 bg-amber-50 dark:bg-amber-500/10 border border-amber-200 dark:border-amber-500/20 rounded-lg text-sm text-amber-800 dark:text-amber-200">
                            <p className="font-semibold mb-1">Store these codes securely!</p>
                            <p className="text-xs opacity-90">If you lose access to your authenticator app, these codes are the only way to access your account. Each code can only be used once.</p>
                        </div>
                        
                        <div className="grid grid-cols-2 gap-2 bg-slate-50 dark:bg-slate-900/50 p-4 rounded-xl border border-slate-200 dark:border-slate-800 max-h-48 overflow-y-auto">
                            {backupCodes.map((bc, idx) => (
                                <code key={idx} className="text-sm font-mono text-slate-700 dark:text-slate-300 py-1.5 px-2 bg-white dark:bg-slate-800 rounded border border-slate-100 dark:border-slate-700 text-center">
                                    {bc}
                                </code>
                            ))}
                        </div>
                        
                        <div className="flex gap-2">
                            <button
                                onClick={handleCopyCodes}
                                className="flex-1 flex items-center justify-center gap-2 py-2 px-3 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors"
                            >
                                {copiedCodes ? <Check className="w-4 h-4 text-emerald-500" /> : <Copy className="w-4 h-4" />}
                                {copiedCodes ? 'Copied!' : 'Copy Codes'}
                            </button>
                            <button
                                onClick={handleDownloadCodes}
                                className="flex-1 flex items-center justify-center gap-2 py-2 px-3 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors"
                            >
                                <Download className="w-4 h-4" />
                                Download
                            </button>
                        </div>

                        <div className="pt-4 border-t border-slate-100 dark:border-slate-800">
                            <label className="flex items-start gap-3 cursor-pointer group">
                                <div className="relative flex items-center mt-0.5">
                                    <input 
                                        type="checkbox" 
                                        checked={savedConfirmed}
                                        onChange={(e) => setSavedConfirmed(e.target.checked)}
                                        className="peer sr-only"
                                    />
                                    <div className="h-5 w-5 rounded border border-slate-300 bg-white peer-checked:border-teal-500 peer-checked:bg-teal-500 peer-focus:ring-2 peer-focus:ring-teal-500/30 transition-all dark:border-slate-600 dark:bg-slate-800 dark:peer-checked:border-teal-400 dark:peer-checked:bg-teal-400"></div>
                                    <svg className="absolute left-1/2 top-1/2 h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 text-white opacity-0 peer-checked:opacity-100 transition-opacity dark:text-slate-900" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="3">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                                    </svg>
                                </div>
                                <span className="text-sm text-slate-700 dark:text-slate-300 group-hover:text-slate-900 dark:group-hover:text-slate-100 transition-colors">
                                    I have safely recorded these recovery codes.
                                </span>
                            </label>
                            
                            <AuthButton 
                                onClick={handleComplete}
                                disabled={!savedConfirmed}
                                className="w-full mt-4"
                            >
                                Complete Setup
                            </AuthButton>
                        </div>
                    </div>
                ) : null}
            </div>
        </div>
    );
};

export default MfaSetupModal;
