import {API_CONFIG} from '../config';
import {apiService} from './apiService';
import {getDeviceId} from '../utils/device';
import {extractUserStatus, isDeletedUserStatus} from './authSession';

export const getCurrentUser = async () => {
    return apiService.get(API_CONFIG.ENDPOINTS.PROFILE);
};

export const resolveCurrentUserStatus = (payload) => {
    return extractUserStatus(payload) || null;
};

export const deleteCurrentUser = async () => {
    return apiService.post(API_CONFIG.ENDPOINTS.ACCOUNT_DELETE, {
        device_id: getDeviceId(),
    });
};

export const isDeletedUserPayload = (payload) => {
    return isDeletedUserStatus(extractUserStatus(payload));
};
