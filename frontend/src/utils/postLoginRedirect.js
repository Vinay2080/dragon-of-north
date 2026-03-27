const POST_LOGIN_REDIRECT_KEY = 'post_login_redirect_path';

const isSafeInternalPath = (value) => {
    if (typeof value !== 'string') {
        return false;
    }

    const trimmed = value.trim();
    return trimmed.startsWith('/') && !trimmed.startsWith('//');
};

export const persistPostLoginRedirect = (path) => {
    if (!isSafeInternalPath(path)) {
        return;
    }

    localStorage.setItem(POST_LOGIN_REDIRECT_KEY, path);
};

export const consumePostLoginRedirect = () => {
    const persisted = localStorage.getItem(POST_LOGIN_REDIRECT_KEY);
    localStorage.removeItem(POST_LOGIN_REDIRECT_KEY);

    if (!isSafeInternalPath(persisted)) {
        return null;
    }

    return persisted;
};

export const resolvePostLoginRedirectPath = ({location, defaultPath = '/'} = {}) => {
    const statePath = location?.state?.from?.pathname;
    if (isSafeInternalPath(statePath)) {
        localStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
        return statePath;
    }

    const persistedPath = consumePostLoginRedirect();
    if (persistedPath) {
        return persistedPath;
    }

    return defaultPath;
};
