import React from 'react';

const cx = (...values) => values.filter(Boolean).join(' ');

const VARIANT_CLASSES = {
    primary: 'bg-teal-500 hover:bg-teal-600 focus:bg-teal-600 active:bg-teal-700 text-white',
    secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700 border border-gray-300',
    danger: 'bg-red-500 hover:bg-red-600 focus:bg-red-600 active:bg-red-700 text-white',
};

const RING_CLASSES = {
    primary: 'focus:ring-teal-500',
    secondary: 'focus:ring-teal-500',
    danger: 'focus:ring-red-500',
};

/**
 * Single source of truth for button styles.
 * Exactly 3 variants: primary (teal), secondary (gray), danger (red).
 */
const Button = React.forwardRef(function Button(
    {
        variant = 'primary',
        className = '',
        type = 'button',
        disabled,
        ...props
    },
    ref
) {
    const resolvedVariant = VARIANT_CLASSES[variant] ? variant : 'primary';

    return (
        <button
            ref={ref}
            type={type}
            disabled={disabled}
            className={cx(
                // base
                'inline-flex items-center justify-center gap-2 rounded-2xl px-4 py-2 text-sm font-semibold',
                'transition-colors duration-150',
                'disabled:cursor-not-allowed disabled:opacity-60',
                // override protection: these must win even if any legacy CSS exists
                '!shadow-none',
                // interaction
                'focus:outline-none focus:ring-2 focus:ring-offset-2',
                RING_CLASSES[resolvedVariant],
                VARIANT_CLASSES[resolvedVariant],
                className
            )}
            {...props}
        />
    );
});

export default Button;

