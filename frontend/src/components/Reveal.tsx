import { ReactNode } from 'react';
import { motion, MotionProps, Easing } from 'framer-motion';

/**
 * Unified animation configuration for scroll reveals
 * Strict constraints: opacity and transform only, GPU-friendly
 */
export const REVEAL_CONFIG = {
    initial: {
        opacity: 0,
        y: 40,
    },
    animate: {
        opacity: 1,
        y: 0,
    },
    transition: {
        duration: 0.6,
        ease: 'easeOut' as Easing,
    },
};

/**
 * Stagger configuration for lists/cards
 * Each child gets delay: index * 100ms
 */
export const STAGGER_CONFIG = {
    container: {
        hidden: { opacity: 0 },
        show: {
            opacity: 1,
            transition: {
                staggerChildren: 0.1, // 100ms between each child
                delayChildren: 0,
            },
        },
    },
    item: {
        hidden: REVEAL_CONFIG.initial,
        show: REVEAL_CONFIG.animate,
    },
};

interface RevealProps extends Omit<MotionProps, 'children'> {
    children: ReactNode;
    delay?: number;
    width?: string | number;
    className?: string;
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
    width = '100%',
    className = '',
    ...motionProps
}: RevealProps) => {
    return (
        <motion.div
            initial={REVEAL_CONFIG.initial}
            whileInView={REVEAL_CONFIG.animate}
            viewport={{ once: true, amount: 0.25 }}
            transition={{
                ...REVEAL_CONFIG.transition,
                delay,
            }}
            style={{ width }}
            className={className}
            {...motionProps}
        >
            {children}
        </motion.div>
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
    return (
        <motion.div
            initial="hidden"
            whileInView="show"
            viewport={{ once: true, amount: 0.25 }}
            variants={STAGGER_CONFIG.container}
            className={className}
        >
            {children}
        </motion.div>
    );
};

interface RevealItemProps extends Omit<MotionProps, 'children'> {
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
    ...motionProps 
}: RevealItemProps) => {
    // Map element types to their motion counterparts
    const motionComponentMap: Record<string, any> = {
        div: motion.div,
        a: motion.a,
        article: motion.article,
    };
    
    const MotionComponent = motionComponentMap[as] || motion.div;
    
    const elementProps: any = {
        variants: STAGGER_CONFIG.item,
        className,
        ...motionProps,
    };
    
    // Add link-specific props if it's an anchor
    if (as === 'a' && href) {
        elementProps.href = href;
        if (target) elementProps.target = target;
        if (rel) elementProps.rel = rel;
    }
    
    return (
        <MotionComponent {...elementProps}>
            {children}
        </MotionComponent>
    );
};



