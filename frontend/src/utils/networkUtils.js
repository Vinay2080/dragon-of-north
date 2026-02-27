export const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

export const exponentialBackoffDelay = (attempt, baseDelay = 300) => {
    const jitter = Math.floor(Math.random() * 150);
    return Math.min(baseDelay * (2 ** attempt) + jitter, 5000);
};

export const shouldRetryRequest = (status) => {
    if (!status) {
        return true;
    }

    return status >= 500 || status === 429;
};
