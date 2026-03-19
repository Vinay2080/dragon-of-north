import {CSRF_HEADER_NAME, ensureCsrfToken, isStateChangingMethod} from '../utils/csrf';

type RequestConfig = {
    baseURL?: string;
    url?: string;
    method?: string;
    headers?: Record<string, string>;
    data?: unknown;
    withCredentials?: boolean;
};

type RequestInterceptor = (config: RequestConfig) => RequestConfig | Promise<RequestConfig>;

type ResponseInterceptor = 
    | ((response: {data: unknown; status: number; headers: Headers}) => unknown)
    | ((error: unknown) => Promise<never>);

const requestInterceptors: RequestInterceptor[] = [];
const responseSuccessInterceptors: Array<(response: {data: unknown; status: number; headers: Headers}) => unknown> = [];
const responseErrorInterceptors: Array<(error: unknown) => Promise<never>> = [];

const getToken = () => localStorage.getItem('auth_token');

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
        const isMutatingRequest = isStateChangingMethod(requestMethod);
        const requestHeaders = {...(merged.headers || {})};

        if (isMutatingRequest) {
            requestHeaders[CSRF_HEADER_NAME] = await ensureCsrfToken();
        }

        const response = await fetch(`${merged.baseURL || ''}${merged.url || ''}`, {
            method: merged.method || 'GET',
            headers: requestHeaders,
            credentials: merged.withCredentials ? 'include' : 'same-origin',
            body: merged.data ? JSON.stringify(merged.data) : undefined,
        });

        const data = await parseResponseBody(response);

        if (!response.ok) {
            const error: unknown = {response: {status: response.status, data}};
            return runErrorInterceptors(error);
        }

        return runSuccessInterceptors({data, status: response.status, headers: response.headers});
    };

    return {
        interceptors: {
            request: {
                use: (interceptor: RequestInterceptor): void => requestInterceptors.push(interceptor),
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
    const token = getToken();
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
