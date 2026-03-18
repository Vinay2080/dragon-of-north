import React, {useEffect, useRef, useState} from 'react';
import {ChevronRight} from 'react-feather';

/**
 * CascadingMenu - JetBrains-style cascading side menu
 *
 * Features:
 * - Submenus open to the LEFT (side cascade)
 * - No overlap between menus
 * - Smooth hover interactions with delay to prevent flicker
 * - Dark/light theme support
 * - Proper positioning with small gaps
 */

const CascadingMenuItems = ({items, onItemClick, level = 0}) => {
    const [activeSubmenuId, setActiveSubmenuId] = useState(null);
    const [submenuTimeoutId, setSubmenuTimeoutId] = useState(null);
    const submenuRefs = useRef({});

    const handleMouseEnter = (itemId) => {
        clearTimeout(submenuTimeoutId);
        setActiveSubmenuId(itemId);
    };

    const handleMouseLeave = () => {
        const timeoutId = setTimeout(() => {
            setActiveSubmenuId(null);
        }, 100); // 100ms delay to prevent flicker
        setSubmenuTimeoutId(timeoutId);
    };

    useEffect(() => {
        return () => {
            if (submenuTimeoutId) {
                clearTimeout(submenuTimeoutId);
            }
        };
    }, [submenuTimeoutId]);

    return (
        <div className="space-y-1">
            {items.map((item) => {
                const hasSubmenu = item.items && item.items.length > 0;
                const isActive = activeSubmenuId === item.id;

                return (
                    <div
                        key={item.id}
                        className="relative group/item"
                        onMouseEnter={() => hasSubmenu && handleMouseEnter(item.id)}
                        onMouseLeave={handleMouseLeave}
                        ref={(el) => {
                            if (el) submenuRefs.current[item.id] = el;
                        }}
                    >
                        {/* Menu Item */}
                        <button
                            onClick={() => {
                                if (!hasSubmenu && item.onClick) {
                                    item.onClick();
                                } else if (!hasSubmenu && onItemClick) {
                                    onItemClick(item);
                                }
                            }}
                            className={`w-full px-3 py-2 text-left text-sm font-medium transition-all duration-200 ease-out flex items-center justify-between gap-2 rounded-lg ${
                                isActive
                                    ? 'bg-violet-100 text-violet-700 border-none font-bold dark:bg-violet-500/20 dark:text-violet-300'
                                    : 'text-slate-700 border-none hover:bg-slate-100 hover:font-bold dark:text-slate-300 dark:hover:bg-white/5'
                            }`}
                        >
                            <span className="flex items-center gap-2.5 flex-1 min-w-0">
                                {item.icon && <item.icon className="h-4 w-4 flex-shrink-0"/>}
                                <span className="truncate">{item.label}</span>
                            </span>
                            {hasSubmenu && (
                                <ChevronRight
                                    className={`h-4 w-4 transition-transform duration-200 flex-shrink-0 ${isActive ? 'rotate-180' : ''}`}/>
                            )}
                        </button>

                        {/* Submenu - Opens to the LEFT */}
                        {hasSubmenu && (
                            <>
                                {/* Hover bridge to prevent menu from closing when moving mouse to submenu */}
                                <div
                                    className={`absolute right-full top-0 bottom-0 w-[14px] z-30 ${isActive ? 'block' : 'hidden'}`}/>
                                <div
                                    className={`absolute right-[calc(100%+12px)] -top-1 w-48 bg-white dark:bg-slate-800 border border-slate-200 dark:border-white/10 rounded-lg shadow-xl z-40 transition-all duration-200 p-1 ${
                                        isActive
                                            ? 'opacity-100 visible pointer-events-auto translate-x-0'
                                            : 'opacity-0 invisible pointer-events-none translate-x-1'
                                    }`}
                                >
                                    <CascadingMenuItems
                                        items={item.items}
                                        onItemClick={onItemClick}
                                        level={level + 1}
                                    />
                                </div>
                            </>
                        )}
                    </div>
                );
            })}
        </div>
    );
};

export default function CascadingMenu({
                                          trigger,
                                          items,
                                          onItemClick,
                                          align = 'left',
                                          className = '',
                                      }) {
    const [isOpen, setIsOpen] = useState(false);
    const menuRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

    const handleItemClick = (item) => {
        onItemClick?.(item);
        setIsOpen(false);
    };

    return (
        <div className={`relative inline-block ${className}`} ref={menuRef}>
            {/* Trigger Button */}
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-lg border border-slate-300 bg-white text-slate-900 transition-all duration-200 hover:border-slate-400 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100 dark:hover:border-slate-500 dark:hover:bg-slate-700"
            >
                {trigger}
            </button>

            {/* Parent Menu */}
            {isOpen && (
                <div
                    className={`absolute ${align === 'left' ? 'left-0' : 'right-0'} top-full mt-2 w-56 bg-white dark:bg-slate-800 border border-slate-200 dark:border-white/10 rounded-lg shadow-xl z-50 overflow-hidden`}
                >
                    <div className="p-2">
                        <CascadingMenuItems
                            items={items}
                            onItemClick={handleItemClick}
                            level={0}
                        />
                    </div>
                </div>
            )}
        </div>
    );
}

