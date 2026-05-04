import React, {useEffect, useId, useMemo, useRef, useState} from 'react';
import {useTheme} from '../context/ThemeContext';
import * as Icons from '../shims/lucide-react';

type ThemeMode = 'light' | 'dark' | 'system';

const {Sun, Moon, Monitor} = Icons;

const THEME_MODES: ThemeMode[] = ['light', 'dark', 'system'];

const themeMeta: Record<ThemeMode, {
    label: string;
    Icon: React.ComponentType<{ size?: number; className?: string }>
}> = {
    light: {label: 'Light', Icon: Sun},
    dark: {label: 'Dark', Icon: Moon},
    system: {label: 'System', Icon: Monitor},
};

const focusItem = (items: Array<HTMLButtonElement | null>, index: number) => {
    const clamped = Math.max(0, Math.min(items.length - 1, index));
    items[clamped]?.focus();
    return clamped;
};

export default function ThemeSwitcherPopover() {
    const {theme, setTheme} = useTheme();
    const [open, setOpen] = useState(false);

    const popoverId = useId();
    const rootRef = useRef<HTMLDivElement | null>(null);
    const triggerRef = useRef<HTMLButtonElement | null>(null);
    const itemRefs = useRef<Array<HTMLButtonElement | null>>([]);

    const TriggerIcon = useMemo(() => themeMeta[theme].Icon, [theme]);

    useEffect(() => {
        if (!open) return;

        const onPointerDown = (event: MouseEvent) => {
            if (!rootRef.current) return;
            if (!rootRef.current.contains(event.target as Node)) {
                setOpen(false);
            }
        };

        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                event.preventDefault();
                setOpen(false);
                triggerRef.current?.focus();
            }
        };

        document.addEventListener('mousedown', onPointerDown);
        document.addEventListener('keydown', onKeyDown);
        return () => {
            document.removeEventListener('mousedown', onPointerDown);
            document.removeEventListener('keydown', onKeyDown);
        };
    }, [open]);

    useEffect(() => {
        if (!open) return;

        const initialIndex = Math.max(0, THEME_MODES.indexOf(theme));

        // Focus after paint so the element exists.
        const id = window.setTimeout(() => {
            focusItem(itemRefs.current, initialIndex);
        }, 0);

        return () => window.clearTimeout(id);
    }, [open, theme]);

    const getFocusedIndex = () => itemRefs.current.findIndex((el) => el === (document.activeElement as any));

    const moveFocus = (delta: number) => {
        const current = getFocusedIndex();
        const nextIndex = (current === -1 ? 0 : current) + delta;
        focusItem(itemRefs.current, nextIndex);
    };

    const setThemeAndClose = (mode: ThemeMode) => {
        setTheme(mode);
        setOpen(false);
        triggerRef.current?.focus();
    };

    return (
        <div className="relative" ref={rootRef}>
            <button
                ref={triggerRef}
                type="button"
                className="inline-flex h-9 w-9 items-center justify-center rounded-full text-foreground transition-colors hover:bg-muted/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
                aria-label="Change theme"
                aria-haspopup="menu"
                aria-expanded={open}
                aria-controls={popoverId}
                onClick={() => setOpen((v) => !v)}
                onKeyDown={(event) => {
                    if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        setOpen(true);
                    }
                }}
                title={`Theme: ${themeMeta[theme].label}`}
            >
                <TriggerIcon size={18}/>
            </button>

            <div
                id={popoverId}
                role="menu"
                aria-label="Theme selection"
                className={`absolute right-0 z-50 mt-2 ${open ? '' : 'hidden'}`}
            >
                <div className="rounded-xl border border-border bg-white p-2 shadow-lg dark:bg-neutral-900">
                    <div className="flex items-center gap-1">
                        {THEME_MODES.map((mode, idx) => {
                            const {Icon, label} = themeMeta[mode];
                            const isActive = mode === theme;

                            return (
                                <button
                                    key={mode}
                                    ref={(el) => {
                                        itemRefs.current[idx] = el;
                                    }}
                                    type="button"
                                    role="menuitemradio"
                                    aria-checked={isActive}
                                    aria-label={label}
                                    title={label}
                                    onClick={() => setThemeAndClose(mode)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
                                            event.preventDefault();
                                            moveFocus(1);
                                        }
                                        if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
                                            event.preventDefault();
                                            moveFocus(-1);
                                        }
                                        if (event.key === 'Home') {
                                            event.preventDefault();
                                            focusItem(itemRefs.current, 0);
                                        }
                                        if (event.key === 'End') {
                                            event.preventDefault();
                                            focusItem(itemRefs.current, THEME_MODES.length - 1);
                                        }
                                        if (event.key === 'Enter' || event.key === ' ') {
                                            event.preventDefault();
                                            setThemeAndClose(mode);
                                        }
                                    }}
                                    className={`inline-flex h-9 w-9 items-center justify-center rounded-lg transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background ${
                                        isActive ? 'bg-primary/20 text-primary' : 'text-foreground hover:bg-muted/60'
                                    }`}
                                >
                                    <Icon size={18}/>
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
}


