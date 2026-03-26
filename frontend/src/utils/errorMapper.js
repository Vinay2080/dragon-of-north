const ERROR_MESSAGES = {
    TOK_001: 'Authentication failed. Please check your credentials and try again.',
    TOK_002: 'Invalid authentication token. Please log in again.',
    TOK_003: 'Unsupported authentication token. Please log in again.',
    TOK_004: 'Invalid authentication token. Please log in again.',
    TOK_005: 'Your session is missing. Please log in again.',
    AUTH_001: "The identifier doesn't match the expected format.",
    AUTH_002: 'Too many requests. Please wait and try again.',
    AUTH_003: 'Too many requests. Please wait and try again.',
    AUTH_004: "Account status doesn't allow this action.",
    AUTH_005: 'Invalid username or password. Please check your credentials.',
    AUTH_006: 'Access denied. Please log in again to continue.',
    USER_001: 'User account not found. Please check your credentials.',
    USER_002: 'Your account is already verified.',
    VAL_001: 'Please check your input and try again.',
    VAL_002: 'Email not verified. Please verify your email with OTP before signing in.',
    OTP_001: 'Please wait {seconds} seconds before requesting another OTP.',
    OTP_002: 'Too many OTP attempts. Please try again in {minutes} minutes.',
    ROL_009: 'User role not found. Please contact support.',
};

const getTemplateValue = (data, keys, fallback) => {
    for (const key of keys) {
        const value = data?.[key];
        if (value !== undefined && value !== null) {
            return value;
        }
    }
    return fallback;
};

export const mapErrorCodeToMessage = (errorCode, data = {}) => {
    if (!errorCode || !ERROR_MESSAGES[errorCode]) {
        return data?.message || data?.defaultMessage || 'Something went wrong. Please try again.';
    }

    const seconds = getTemplateValue(data, ['seconds', 'retry_after', 'retryAfter'], 60);
    const minutes = getTemplateValue(data, ['minutes'], Math.ceil(seconds / 60));

    return ERROR_MESSAGES[errorCode]
        .replace('{seconds}', String(seconds))
        .replace('{minutes}', String(minutes));
};

export const getFieldValidationErrors = (data = {}) => {
    const validationList = data?.validation_error_list || data?.validationErrorList;
    if (!Array.isArray(validationList)) {
        return {};
    }

    return validationList.reduce((acc, item) => {
        if (!item?.field || !item?.message) {
            return acc;
        }
        if (!acc[item.field]) {
            acc[item.field] = [];
        }
        acc[item.field].push(item.message);
        return acc;
    }, {});
};

export default ERROR_MESSAGES;
