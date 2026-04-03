import {ensureCsrfHeader} from '../utils/csrf';
import {API_CONFIG} from '../config';
import {getDeviceId} from '../utils/device';
import {clearAccessToken, extractAccessToken, getAccessToken, setAccessToken} from '../services/tokenStore';

type RequestConfig = {
    baseURL?: string;
    url?: string;
    method?: string;
    headers?: Record<string, string>;
    data?: unknown;
    withCredentials?: boolean;
    __isRetryRequest?: boolean;
};

type RequestInterceptor = (config: RequestConfig) => RequestConfig | Promise<RequestConfig>;

const requestInterceptors: RequestInterceptor[] = [];
const responseSuccessInterceptors: Array<(response: {data: unknown; status: number; headers: Headers}) => unknown> = [];
const responseErrorInterceptors: Array<(error: unknown) => Promise<never>> = [];

let refreshPromise: Promise<void> | null = null;

const extractAuthErrorStatus = (error: unknown): number | null => {
    const errorRecord = error as { response?: { status?: number } };
    return typeof errorRecord?.response?.status === 'number' ? errorRecord.response.status : null;
};

const tryPersistToken = (payload: unknown): void => {
    const token = extractAccessToken(payload as Record<string, unknown>);
    if (token) {
        setAccessToken(token);
    }
};

const refreshAccessToken = async (): Promise<void> => {
    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise = (async () => {
        const method = 'POST';
        const requestConfig = await ensureCsrfHeader({
            method,
            headers: {
                'Content-Type': 'application/json',
            },
            credentials: 'include',
            body: JSON.stringify({device_id: getDeviceId()}),
        });

        const response = await fetch(
            `${import.meta.env.VITE_API_BASE_URL || ''}${API_CONFIG.ENDPOINTS.REFRESH_TOKEN}`,
            requestConfig as RequestInit,
        );

        if (!response.ok) {
            clearAccessToken();
            throw new Error('Refresh request failed');
        }

        const payload = await parseResponseBody(response);
        tryPersistToken(payload);
    })();

    try {
        await refreshPromise;
    } finally {
        refreshPromise = null;
    }
};

const applyRequestInterceptors = async (config: RequestConfig) => {
    let currentConfig = config;

    for (const interceptor of requestInterceptors) {
        currentConfig = await interceptor(currentConfig);
    }

    return currentConfig;
};

const parseResponseBody = async (response: Response): Promise<unknown> => {
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
        return response.json();
    }

    return response.text();
};

const runSuccessInterceptors = (payload: {data: unknown; status: number; headers: Headers}): unknown => {
    return responseSuccessInterceptors.reduce((acc, interceptor) => interceptor(acc as never), payload);
};

const runErrorInterceptors = async (error: unknown): Promise<never> => {
    if (responseErrorInterceptors.length === 0) {
        throw error;
    }

    for (const interceptor of responseErrorInterceptors) {
        await interceptor(error);
    }

    throw error;
};

const create = (defaults: RequestConfig) => {
    const request = async (config: RequestConfig): Promise<unknown> => {
        const merged = await applyRequestInterceptors({
            ...defaults,
            ...config,
            headers: {
                ...(defaults.headers || {}),
                ...(config.headers || {}),
            },
        });

        const requestMethod = merged.method || 'GET';
        const requestWithCsrf = await ensureCsrfHeader({
            ...merged,
            method: requestMethod,
            headers: {...((merged.headers || {}) as Record<string, string>)},
        });

        const response = await fetch(`${merged.baseURL || ''}${merged.url || ''}`, {
            method: requestMethod,
            headers: (requestWithCsrf.headers || {}) as HeadersInit,
            credentials: merged.withCredentials ? 'include' : 'same-origin',
            body: merged.data ? JSON.stringify(merged.data) : undefined,
        });

        const data = await parseResponseBody(response);
        tryPersistToken(data);

        if (!response.ok) {
            const error: unknown = {response: {status: response.status, data}};
            try {
                return await runErrorInterceptors(error);
            } catch (interceptorError) {
                const shouldRetry =
                    extractAuthErrorStatus(interceptorError) === 401
                    && !config.__isRetryRequest;

                if (shouldRetry) {
                    await refreshAccessToken();
                    return request({...config, __isRetryRequest: true});
                }

                throw interceptorError;
            }
        }

        return runSuccessInterceptors({data, status: response.status, headers: response.headers});
    };

    return {
        interceptors: {
            request: {
                use: (interceptor: RequestInterceptor): void => {
                    requestInterceptors.push(interceptor);
                },
            },
            response: {
                use: (
                    onSuccess?: (response: {data: unknown; status: number; headers: Headers}) => unknown,
                    onError?: (error: unknown) => Promise<never>
                ): void => {
                    if (onSuccess) responseSuccessInterceptors.push(onSuccess);
                    if (onError) responseErrorInterceptors.push(onError);
                },
            },
        },
        post: (url: string, data?: unknown, config: RequestConfig = {}) => request({...config, method: 'POST', url, data}),
    };
};

const axios = {create};

const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use((config: RequestConfig): RequestConfig => {
    const token = getAccessToken();
    if (!token) return config;

    return {
        ...config,
        headers: {
            ...(config.headers || {}),
            Authorization: `Bearer ${token}`,
        },
    };
});

apiClient.interceptors.response.use(
    (response: {data: unknown; status: number; headers: Headers}): {data: unknown; status: number; headers: Headers} => response,
    async (error: unknown): Promise<never> => Promise.reject(error),
);

export default apiClient;
