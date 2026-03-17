import {createContext, useContext, useEffect, useMemo, useState} from 'react';

type ThemeMode = 'light' | 'dark' | 'system';

type ThemeContextValue = {
    theme: ThemeMode;
    setTheme: (theme: ThemeMode) => void;
};

const THEME_STORAGE_KEY = 'don-theme';

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

const isThemeMode = (value: string | null): value is ThemeMode => {
    return value === 'light' || value === 'dark' || value === 'system';
};

const resolveIsDark = (theme: ThemeMode) => {
    if (theme === 'dark') return true;
    if (theme === 'light') return false;
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
};

const applyThemeToDocument = (isDark: boolean) => {
    const mode = isDark ? 'dark' : 'light';
    const root = document.documentElement;
    const body = document.body;

    root.classList.toggle('dark', isDark);
    root.setAttribute('data-theme', mode);
    body.classList.toggle('dark', isDark);
    body.setAttribute('data-theme', mode);
};

export const ThemeProvider = ({children}) => {
    const [theme, setTheme] = useState<ThemeMode>(() => {
        const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
        return isThemeMode(savedTheme) ? savedTheme : 'system';
    });

    useEffect(() => {
        localStorage.setItem(THEME_STORAGE_KEY, theme);
        applyThemeToDocument(resolveIsDark(theme));
    }, [theme]);

    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

        const syncSystemTheme = () => {
            if (theme !== 'system') return;
            applyThemeToDocument(mediaQuery.matches);
        };

        syncSystemTheme();
        mediaQuery.addEventListener('change', syncSystemTheme);

        return () => mediaQuery.removeEventListener('change', syncSystemTheme);
    }, [theme]);

    const value = useMemo(() => ({theme, setTheme}), [theme]);

    return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
};

export const useTheme = () => {
    const context = useContext(ThemeContext);

    if (!context) {
        throw new Error('useTheme must be used within ThemeProvider');
    }

    return context;
};
