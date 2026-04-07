const AUTH_ERROR_CODES = {
    EMAIL_NOT_VERIFIED: new Set(['EMAIL_NOT_VERIFIED', 'VAL_002']),
    USER_NOT_FOUND: new Set(['USER_NOT_FOUND', 'USER_001']),
    USER_REACTIVATION_REQUIRED: new Set(['USER_REACTIVATION_REQUIRED', 'USER_004']),
    AUTHENTICATION_FAILED: new Set(['AUTHENTICATION_FAILED', 'AUTH_005']),
    USER_BLOCKED: new Set(['USER_BLOCKED', 'USER_005']),
    RATE_LIMIT_EXCEEDED: new Set(['RATE_LIMIT_EXCEEDED', 'AUTH_003']),
    TOO_MANY_REQUESTS: new Set(['TOO_MANY_REQUESTS', 'AUTH_002']),
};

const APP_USER_STATUS = {
    PENDING_VERIFICATION: 'PENDING_VERIFICATION',
    ACTIVE: 'ACTIVE',
    LOCKED: 'LOCKED',
    DELETED: 'DELETED',
};

const getErrorCode = (result) => String(
    result?.errorCode ||
    result?.error_code ||
    result?.raw?.error_code ||
    result?.raw?.errorCode ||
    result?.raw?.code ||
    result?.data?.error_code ||
    result?.data?.code ||
    ''
).toUpperCase();

const getAppUserStatus = (result) => String(
    result?.data?.app_user_status ||
    result?.raw?.app_user_status ||
    result?.app_user_status ||
    ''
).toUpperCase();

const hasCode = (set, code) => set.has(code);

export const handleAuthResponse = (result, navigate, toast, identifier, identifierType) => {
    if (result?.api_response_status === 'success') {
        navigate('/sessions', {replace: true});
        return {type: 'SUCCESS'};
    }

    const errorCode = getErrorCode(result);
    const status = getAppUserStatus(result);
    const backendMessage = result?.backendMessage || result?.message || 'Something went wrong. Please try again.';

    if (
        hasCode(AUTH_ERROR_CODES.EMAIL_NOT_VERIFIED, errorCode) ||
        status === APP_USER_STATUS.PENDING_VERIFICATION
    ) {
        navigate('/otp', {
            replace: true,
            state: {
                identifier,
                identifierType,
                flow: 'LOGIN_UNVERIFIED',
                reason: 'EMAIL_NOT_VERIFIED',
            },
        });
        return {type: 'REDIRECTED', destination: 'OTP'};
    }

    if (
        hasCode(AUTH_ERROR_CODES.USER_NOT_FOUND, errorCode) ||
        hasCode(AUTH_ERROR_CODES.USER_REACTIVATION_REQUIRED, errorCode) ||
        status === APP_USER_STATUS.DELETED
    ) {
        toast.error('Invalid email or password');
        return {type: 'ERROR'};
    }

    if (hasCode(AUTH_ERROR_CODES.AUTHENTICATION_FAILED, errorCode)) {
        toast.error('Invalid email or password');
        return {type: 'ERROR'};
    }

    if (hasCode(AUTH_ERROR_CODES.USER_BLOCKED, errorCode) || status === APP_USER_STATUS.LOCKED) {
        toast.error('Account is blocked');
        return {type: 'ERROR'};
    }

    if (
        result?.type === 'RATE_LIMIT_EXCEEDED' ||
        hasCode(AUTH_ERROR_CODES.RATE_LIMIT_EXCEEDED, errorCode) ||
        hasCode(AUTH_ERROR_CODES.TOO_MANY_REQUESTS, errorCode)
    ) {
        toast.error('Too many requests. Please try again later');
        return {type: 'ERROR'};
    }

    toast.error(backendMessage);
    return {type: 'ERROR'};
};
