import {useEffect} from 'react';
import {useLocation} from 'react-router-dom';
import {DEFAULT_TAB_TITLE, formatBrandedPageTitle, ROUTE_PAGE_TITLES} from '../constants/branding';

export const useDocumentTitle = (pageName) => {
    useEffect(() => {
        document.title = formatBrandedPageTitle(pageName);
    }, [pageName]);
};

export const useRouteDocumentTitle = (routeTitleMap = ROUTE_PAGE_TITLES) => {
    const location = useLocation();

    useEffect(() => {
        const pageTitle = routeTitleMap[location.pathname];
        document.title = pageTitle ? formatBrandedPageTitle(pageTitle) : DEFAULT_TAB_TITLE;
    }, [location.pathname, routeTitleMap]);
};

