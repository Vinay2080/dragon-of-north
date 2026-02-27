export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const PHONE_REGEX = /^\+?\d{10,15}$/;

export const isEmail = (value = '') => EMAIL_REGEX.test(value.trim());
export const normalizePhone = (value = '') => value.replace(/\D/g, '');
export const isPhone = (value = '') => PHONE_REGEX.test(value.trim()) || /^\d{10,15}$/.test(normalizePhone(value));

export const validateIdentifier = (value = '') => {
    const trimmed = value.trim();
    if (!trimmed) return 'Please enter your email or phone number.';
    if (isEmail(trimmed) || isPhone(trimmed)) return '';
    return 'Enter a valid email address or phone number (10-15 digits).';
};

export const validatePassword = (value = '') => {
    const errors = [];
    if (value.length < 8) errors.push('At least 8 characters.');
    if (!/[A-Z]/.test(value)) errors.push('At least one uppercase letter.');
    if (!/[a-z]/.test(value)) errors.push('At least one lowercase letter.');
    if (!/\d/.test(value)) errors.push('At least one number.');
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(value)) errors.push('At least one special character.');
    return errors;
};

export const getIdentifierType = (value = '') => (isEmail(value) ? 'EMAIL' : 'PHONE');
