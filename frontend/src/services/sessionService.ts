import apiClient from '../api/client';

type SessionApiItem = {
    sessionId: string;
    deviceId?: string;
    ipAddress?: string;
    userAgent?: string;
    lastUsedAt?: string;
    revoked?: boolean;
};

export const getSessions = async (): Promise<SessionApiItem[]> => {
    const response: any = await apiClient.post('/api/v1/sessions/get/all');
    return response?.data?.data || response?.data || [];
};

export const revokeSession = async (sessionId: string) => {
    const response: any = await apiClient.post(`/api/v1/sessions/delete/${sessionId}`);
    return response?.data;
};
