import { ReactNode } from 'react';

interface IconProps {
    size?: number;
    className?: string;
    [key: string]: any;
}

const IconBase = ({ children, size = 24, className = '', ...props }: { children: ReactNode; size?: number; className?: string; [key: string]: any }) => (
    <svg
        xmlns="http://www.w3.org/2000/svg"
        width={size}
        height={size}
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={className}
        {...props}
    >
        {children}
    </svg>
);

export const Sun = (props: IconProps) => <IconBase {...props}><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></IconBase>;
export const Moon = (props: IconProps) => <IconBase {...props}><path d="M12 3a7 7 0 1 0 9 9 9 9 0 1 1-9-9z"/></IconBase>;
export const Monitor = (props: IconProps) => <IconBase {...props}><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/></IconBase>;
export const Shield = (props: IconProps) => <IconBase {...props}><path d="M12 3l7 3v6c0 5-3.5 8.5-7 10-3.5-1.5-7-5-7-10V6l7-3z"/></IconBase>;
export const User = (props: IconProps) => <IconBase {...props}><path d="M20 21a8 8 0 0 0-16 0"/><circle cx="12" cy="8" r="4"/></IconBase>;
export const Menu = (props: IconProps) => <IconBase {...props}><path d="M3 12h18M3 6h18M3 18h18"/></IconBase>;
export const X = (props: IconProps) => <IconBase {...props}><path d="M18 6L6 18M6 6l12 12"/></IconBase>;
export const Home = (props: IconProps) => <IconBase {...props}><path d="M3 10.5L12 3l9 7.5"/><path d="M5 9.5V20h14V9.5"/><path d="M9.5 20v-6h5v6"/></IconBase>;
export const ChevronDown = (props: IconProps) => <IconBase {...props}><path d="m6 9 6 6 6-6"/></IconBase>;
export const BookOpen = (props: IconProps) => <IconBase {...props}><path d="M2 7a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v13a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/><path d="M22 7a2 2 0 0 0-2-2h-6a2 2 0 0 0-2 2v13a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2z"/></IconBase>;
export const Zap = (props: IconProps) => <IconBase {...props}><path d="M13 2 3 14h7l-1 8 10-12h-7l1-8z"/></IconBase>;
export const Lock = (props: IconProps) => <IconBase {...props}><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></IconBase>;
export const Loader2 = (props: IconProps) => <IconBase {...props}>
    <path d="M21 12a9 9 0 1 1-6.22-8.56"/>
</IconBase>;
export const Trash2 = (props: IconProps) => <IconBase {...props}>
    <path d="M3 6h18"/>
    <path d="M8 6V4h8v2"/>
    <path d="M19 6l-1 14H6L5 6"/>
    <path d="M10 11v6M14 11v6"/>
</IconBase>;

