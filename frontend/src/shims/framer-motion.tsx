import {forwardRef} from 'react';

const MotionDiv = forwardRef(function MotionDiv({children, ...props}, ref) {
    return <div ref={ref} {...props}>{children}</div>;
});

export const motion = {
    div: MotionDiv,
};

export const AnimatePresence = ({children}) => <>{children}</>;
