import React from 'react';
import {Eye, Lock, Shield, Zap} from 'react-feather';
import CascadingMenu from './CascadingMenu';

/**
 * CascadingMenuExample - Demonstrates JetBrains-style cascading menu
 *
 * Usage:
 * <CascadingMenuExample />
 */

export default function CascadingMenuExample() {
    // Example menu structure with nested items
    const authSystemMenu = [
        {
            id: 'overview',
            label: 'Overview',
            icon: Zap,
            items: [
                {
                    id: 'home',
                    label: 'Home',
                    onClick: () => console.log('Navigate to Home'),
                },
                {
                    id: 'architecture',
                    label: 'Architecture',
                    onClick: () => console.log('Navigate to Architecture'),
                },
            ],
        },
        {
            id: 'core',
            label: 'Core Concepts',
            icon: Lock,
            items: [
                {
                    id: 'features',
                    label: 'Features',
                    onClick: () => console.log('Navigate to Features'),
                },
                {
                    id: 'flow',
                    label: 'Identifier Flow',
                    items: [
                        {
                            id: 'jwt-flow',
                            label: 'JWT Flow',
                            onClick: () => console.log('Navigate to JWT Flow'),
                        },
                        {
                            id: 'refresh-rotation',
                            label: 'Refresh Rotation',
                            onClick: () => console.log('Navigate to Refresh Rotation'),
                        },
                    ],
                },
                {
                    id: 'security-demo',
                    label: 'Security Demo',
                    onClick: () => console.log('Navigate to Security Demo'),
                },
            ],
        },
        {
            id: 'platform',
            label: 'Platform Info',
            icon: Shield,
            items: [
                {
                    id: 'deployment',
                    label: 'Deployment',
                    onClick: () => console.log('Navigate to Deployment'),
                },
                {
                    id: 'privacy',
                    label: 'Privacy',
                    onClick: () => console.log('Navigate to Privacy'),
                },
                {
                    id: 'terms',
                    label: 'Terms',
                    onClick: () => console.log('Navigate to Terms'),
                },
            ],
        },
        {
            id: 'sessions',
            label: 'Sessions',
            icon: Eye,
            items: [
                {
                    id: 'active-sessions',
                    label: 'Active Sessions',
                    onClick: () => console.log('Navigate to Active Sessions'),
                },
                {
                    id: 'session-history',
                    label: 'Session History',
                    onClick: () => console.log('Navigate to Session History'),
                },
            ],
        },
    ];

    const handleMenuItemClick = (item) => {
        console.log('Selected:', item.label);
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-slate-50 dark:bg-slate-900 p-8">
            <div className="space-y-8">
                <h1 className="text-3xl font-bold text-slate-900 dark:text-slate-100">
                    JetBrains-Style Cascading Menu
                </h1>
                <p className="text-slate-600 dark:text-slate-400 max-w-2xl">
                    Hover over menu items with arrows (‹) to see submenus open to the LEFT side.
                    Multiple levels of nesting are supported. The menu closes when you move away.
                </p>

                <div className="flex gap-4 flex-wrap">
                    {/* Basic cascading menu */}
                    <CascadingMenu
                        trigger="Authentication System"
                        items={authSystemMenu}
                        onItemClick={handleMenuItemClick}
                        className="mt-8"
                    />

                    {/* Simple cascading menu */}
                    <CascadingMenu
                        trigger="Theme"
                        items={[
                            {
                                id: 'theme-options',
                                label: 'Theme Options',
                                items: [
                                    {
                                        id: 'light',
                                        label: 'Light',
                                        onClick: () => console.log('Set light theme'),
                                    },
                                    {
                                        id: 'dark',
                                        label: 'Dark',
                                        onClick: () => console.log('Set dark theme'),
                                    },
                                    {
                                        id: 'system',
                                        label: 'System',
                                        onClick: () => console.log('Set system theme'),
                                    },
                                ],
                            },
                        ]}
                        onItemClick={handleMenuItemClick}
                        className="mt-8"
                    />
                </div>

                {/* Features section */}
                <div
                    className="mt-12 bg-white dark:bg-slate-800 p-6 rounded-lg border border-slate-200 dark:border-slate-700">
                    <h2 className="text-xl font-bold text-slate-900 dark:text-slate-100 mb-4">
                        Features
                    </h2>
                    <ul className="space-y-2 text-sm text-slate-600 dark:text-slate-400">
                        <li>✅ Submenus open to the LEFT (side cascade)</li>
                        <li>✅ No overlap between parent and child menus</li>
                        <li>✅ Clean separation with 8px gap</li>
                        <li>✅ Smooth hover interactions</li>
                        <li>✅ 100ms delay to prevent flicker</li>
                        <li>✅ Dark/light theme support</li>
                        <li>✅ Rounded corners and subtle shadow</li>
                        <li>✅ Active item highlighting</li>
                        <li>✅ Multi-level nesting support</li>
                        <li>✅ Click-outside to close</li>
                    </ul>
                </div>
            </div>
        </div>
    );
}

