import {API_CONFIG} from '../config'

export const CSRF_COOKIE_NAME = API_CONFIG.CSRF_COOKIE_NAME || 'XSRF-TOKEN'
export const CSRF_HEADER_NAME = API_CONFIG.CSRF_HEADER_NAME || 'X-XSRF-TOKEN'

let csrfBootstrapPromise = null

export const isStateChangingMethod = (method = 'GET') => {
    const normalizedMethod = method.toUpperCase()
    return !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(normalizedMethod)
}

export const readCookie = (cookieName) => {
    if (typeof document === 'undefined' || !document.cookie) {
        return null
    }

    const encodedName = encodeURIComponent(cookieName)
    const cookies = document.cookie.split(';')

    for (const cookiePair of cookies) {
        const [rawKey, ...rawValueParts] = cookiePair.trim().split('=')
        if (rawKey === encodedName) {
            const rawValue = rawValueParts.join('=')
            return decodeURIComponent(rawValue)
        }
    }

    return null
}

export const attachCsrfHeader = (requestOptions = {}) => {
    const method = (requestOptions.method || 'GET').toUpperCase()
    if (!isStateChangingMethod(method)) {
        return requestOptions
    }

    const csrfToken = readCookie(CSRF_COOKIE_NAME)
    if (!csrfToken) {
        return requestOptions
    }

    return {
        ...requestOptions,
        headers: {
            ...(requestOptions.headers || {}),
            [CSRF_HEADER_NAME]: csrfToken,
        },
    }
}

export const ensureCsrfCookie = async () => {
    const existingToken = readCookie(CSRF_COOKIE_NAME)
    if (existingToken) {
        return existingToken
    }

    if (csrfBootstrapPromise) {
        return csrfBootstrapPromise
    }

    csrfBootstrapPromise = fetch(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.CSRF}`, {
        method: 'GET',
        credentials: 'include',
        headers: {
            Accept: 'application/json',
        },
    })
        .then((response) => {
            if (!response.ok) {
                throw new Error('Failed to initialize CSRF token')
            }

            const token = readCookie(CSRF_COOKIE_NAME)
            if (!token) {
                throw new Error(`CSRF cookie ${CSRF_COOKIE_NAME} was not set`)
            }

            return token
        })
        .finally(() => {
            csrfBootstrapPromise = null
        })

    return csrfBootstrapPromise
}

export const ensureCsrfHeader = async (requestOptions = {}) => {
    const method = (requestOptions.method || 'GET').toUpperCase()
    if (!isStateChangingMethod(method)) {
        return requestOptions
    }

    if (!readCookie(CSRF_COOKIE_NAME)) {
        await ensureCsrfCookie()
    }

    return attachCsrfHeader(requestOptions)
}
