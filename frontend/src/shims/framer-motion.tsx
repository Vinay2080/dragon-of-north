import { Fragment, createElement, forwardRef } from 'react';

const MOTION_PROP_NAMES = new Set([
    'animate',
    'exit',
    'initial',
    'layout',
    'layoutId',
    'transition',
    'variants',
    'viewport',
    'whileFocus',
    'whileHover',
    'whileInView',
    'whileTap',
]);

const stripMotionProps = (props: Record<string, any>): Record<string, any> => {
    const sanitized: Record<string, any> = {};
    for (const [key, value] of Object.entries(props)) {
        if (!MOTION_PROP_NAMES.has(key)) {
            sanitized[key] = value;
        }
    }
    return sanitized;
};

const createMotionComponent = (tag: string) => {
    return forwardRef(function MotionComponent(props: any, ref: any) {
        const { children, ...rest } = props || {};
        return createElement(tag, { ...stripMotionProps(rest), ref }, children);
    });
};

export const motion = new Proxy({}, {
    get: (_, tag: string | symbol) => createMotionComponent(tag as string),
});

export const AnimatePresence = ({ children }: { children: React.ReactNode }) => <Fragment>{children}</Fragment>;
