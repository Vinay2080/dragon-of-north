import {CSSProperties, HTMLAttributes, ReactNode, RefObject} from 'react';
import {useInView} from '../hooks/useInView';

const buildRevealDelayStyle = (delay: number, style?: CSSProperties): CSSProperties => ({
    ...(style ?? {}),
    '--reveal-delay': `${Math.max(delay, 0)}s`,
} as CSSProperties);

const cx = (...parts: Array<string | undefined | false>) => parts.filter(Boolean).join(' ');

interface RevealProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
    children: ReactNode;
    delay?: number;
}

/**
 * Reusable scroll-reveal wrapper component
 * 
 * Usage:
 * <Reveal>
 *   <div>Your content here</div>
 * </Reveal>
 * 
 * With custom delay:
 * <Reveal delay={0.2}>
 *   <div>Your content here</div>
 * </Reveal>
 */
export const Reveal = ({
    children,
    delay = 0,
    className = '',
                           style,
                           ...rest
}: RevealProps) => {
    const [ref, visible] = useInView() as [RefObject<HTMLDivElement | null>, boolean];

    return (
        <div
            ref={ref}
            style={buildRevealDelayStyle(delay, style)}
            className={cx('reveal', visible && 'revealed', className)}
            {...rest}
        >
            {children}
        </div>
    );
};

interface RevealListProps {
    children: ReactNode;
    className?: string;
}

/**
 * Reveal container for lists/cards with automatic stagger
 * 
 * Usage:
 * <RevealList>
 *   <div>Item 1</div>
 *   <div>Item 2</div>
 *   <div>Item 3</div>
 * </RevealList>
 */
export const RevealList = ({ children, className = '' }: RevealListProps) => {
    const [ref, visible] = useInView() as [RefObject<HTMLDivElement | null>, boolean];

    return (
        <div
            ref={ref}
            className={cx('reveal reveal-stagger', visible && 'revealed', className)}
        >
            {children}
        </div>
    );
};

interface RevealItemProps extends Omit<HTMLAttributes<HTMLElement>, 'children'> {
    children: ReactNode;
    className?: string;
    as?: 'div' | 'a' | 'article';
    href?: string;
    target?: string;
    rel?: string;
}

/**
 * Individual item for use inside RevealList
 * Automatically gets staggered animation
 * 
 * Usage:
 * <RevealList>
 *   <RevealItem>Item 1</RevealItem>
 *   <RevealItem as="a" href="/link">Link Item</RevealItem>
 * </RevealList>
 */
export const RevealItem = ({ 
    children, 
    className = '', 
    as = 'div',
    href,
    target,
    rel,
                               ...rest
}: RevealItemProps) => {
    const elementProps: Record<string, unknown> = {
        className: cx('reveal-stagger-item', className),
        ...rest,
    };
    
    // Add link-specific props if it's an anchor
    if (as === 'a' && href) {
        elementProps.href = href;
        if (target) elementProps.target = target;
        if (rel) elementProps.rel = rel;
    }

    if (as === 'a') {
        return <a {...elementProps}>{children}</a>;
    }

    if (as === 'article') {
        return <article {...elementProps}>{children}</article>;
    }

    return <div {...elementProps}>{children}</div>;
};



