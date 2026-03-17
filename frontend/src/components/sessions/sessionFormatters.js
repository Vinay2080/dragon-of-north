export const formatDateTime = (value) => (value ? new Date(value).toLocaleString() : '—');

export const parseUserAgent = (userAgentRaw) => {
    const ua = (userAgentRaw || '').toLowerCase();

    const browser = ua.includes('edg')
        ? 'Edge'
        : ua.includes('chrome')
            ? 'Chrome'
            : ua.includes('safari')
                ? 'Safari'
                : ua.includes('firefox')
                    ? 'Firefox'
                    : ua.includes('opr') || ua.includes('opera')
                        ? 'Opera'
                        : 'Unknown Browser';

    const os = ua.includes('windows')
        ? 'Windows'
        : ua.includes('mac os') || ua.includes('macintosh')
            ? 'macOS'
            : ua.includes('iphone') || ua.includes('ipad') || ua.includes('ios')
                ? 'iOS'
                : ua.includes('android')
                    ? 'Android'
                    : ua.includes('linux')
                        ? 'Linux'
                        : 'Unknown OS';

    return {browser, os};
};

export const formatLocation = (session) => {
    if (session?.location) return session.location;
    if (session?.location_name) return session.location_name;

    const compositeLocation = [session?.city, session?.region, session?.country].filter(Boolean).join(', ');
    return compositeLocation || (session?.ip_address ? `IP ${session.ip_address}` : 'Unknown location');
};

export const getDeviceSummary = (session) => {
    const {browser, os} = parseUserAgent(session.user_agent || session.userAgent);
    return `${browser} • ${os}`;
};

