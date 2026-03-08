import {Fragment, createElement, forwardRef} from 'react';

const createMotionComponent = (tag) => {
    return forwardRef(function MotionComponent({children, ...props}, ref) {
        return createElement(tag, {...props, ref}, children);
    });
};

export const motion = new Proxy({}, {
    get: (_, tag) => createMotionComponent(tag),
});

export const AnimatePresence = ({children}) => <Fragment>{children}</Fragment>;
