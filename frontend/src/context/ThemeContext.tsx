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

export const ThemeProvider = ({children}) => {
    const [theme, setTheme] = useState<ThemeMode>(() => {
        const savedTheme = localStorage.getItem(THEME_STORAGE_KEY);
        return isThemeMode(savedTheme) ? savedTheme : 'system';
    });

    useEffect(() => {
        localStorage.setItem(THEME_STORAGE_KEY, theme);

        const root = document.documentElement;
        root.style.transition = 'background-color 250ms ease, color 250ms ease';

        if (resolveIsDark(theme)) {
            root.classList.add('dark');
            root.setAttribute('data-theme', 'dark');
            return;
        }

        root.classList.remove('dark');
        root.setAttribute('data-theme', 'light');
    }, [theme]);

    useEffect(() => {
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');

        const syncSystemTheme = () => {
            if (theme !== 'system') return;

            if (mediaQuery.matches) {
                document.documentElement.classList.add('dark');
            } else {
                document.documentElement.classList.remove('dark');
            }
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
