import {apiService} from './apiService';

const api = {
    get: (url, options = {}) => apiService.get(url, options),
    post: (url, data, options = {}) => apiService.post(url, data, options),
    patch: (url, data, options = {}) => apiService.patch(url, data, options),
    put: (url, data, options = {}) => apiService.put(url, data, options),
    delete: (url, options = {}) => apiService.delete(url, options),
};

export default api;
