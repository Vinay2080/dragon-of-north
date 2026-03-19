import {Link} from 'react-router-dom';

/**
 * Standardized CTA Button Component
 *
 * Used for native button elements with consistent styling.
 * Variants: primary, secondary, ghost
 *
 * Example:
 * <Button variant="primary" onClick={handleClick}>Action</Button>
 * <Button variant="secondary" disabled>Disabled</Button>
 *
 * @component
 */
export function Button({
                           variant = 'primary',
                           children,
                           disabled = false,
                           className = '',
                           ...props
                       }) {
    return (
        <button
            className={`btn btn-${variant} ${disabled ? 'btn-disabled' : ''} ${className}`}
            disabled={disabled}
            {...props}
        >
            {children}
        </button>
    );
}

/**
 * Link-based CTA Button Component (React Router)
 *
 * Primary CTA: "Explore Sessions"
 * Secondary CTA: "View Architecture"
 *
 * Usage:
 * <LinkButton variant="primary" to="/sessions">Explore Sessions</LinkButton>
 * <LinkButton variant="secondary" to="/architecture">View Architecture</LinkButton>
 *
 * @component
 */
export function LinkButton({
                               variant = 'primary',
                               children,
                               to,
                               className = '',
                               ...props
                           }) {
    return (
        <Link
            to={to}
            className={`btn btn-${variant} inline-flex items-center justify-center ${className}`}
            {...props}
        >
            {children}
        </Link>
    );
}

/**
 * External Link CTA Button Component
 *
 * Used for links to external URLs (opens in new tab).
 * Variants: primary, secondary, ghost
 *
 * Example:
 * <ExternalLinkButton variant="secondary" href="https://github.com">GitHub</ExternalLinkButton>
 *
 * @component
 */
export function ExternalLinkButton({
                                       variant = 'secondary',
                                       children,
                                       href,
                                       className = '',
                                       ...props
                                   }) {
    return (
        <a
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className={`btn btn-${variant} inline-flex items-center justify-center ${className}`}
            {...props}
        >
            {children}
        </a>
    );
}


