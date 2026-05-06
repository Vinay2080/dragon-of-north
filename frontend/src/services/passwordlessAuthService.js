import {API_CONFIG} from '../config';
import {apiService} from './apiService';
import {getDeviceId} from '../utils/device';

const isMissingEndpointError = (result) => (
    apiService.isErrorResponse(result) &&
    (result.status === 401 || result.status === 404 || result.status === 405)
);

const postWithFallback = async (primaryEndpoint, fallbackEndpoint, body) => {
    const primaryResult = await apiService.post(primaryEndpoint, body, {skipAuthRefresh: true});

    if (!isMissingEndpointError(primaryResult)) {
        return primaryResult;
    }

    return apiService.post(fallbackEndpoint, body, {skipAuthRefresh: true});
};

export const requestPasswordlessLogin = (email) => {
    return postWithFallback(
        API_CONFIG.ENDPOINTS.PASSWORDLESS_REQUEST,
        API_CONFIG.ENDPOINTS.PASSWORDLESS_REQUEST_FALLBACK,
        {email}
    );
};

export const verifyPasswordlessLogin = (token) => {
    return postWithFallback(
        API_CONFIG.ENDPOINTS.PASSWORDLESS_VERIFY,
        API_CONFIG.ENDPOINTS.PASSWORDLESS_VERIFY_FALLBACK,
        {
            token,
            device_id: getDeviceId(),
        }
    );
};
