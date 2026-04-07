import React, {useEffect, useState} from 'react';
import {X} from 'lucide-react';

const AvatarPreviewModal = ({isOpen, imageSrc, onClose}) => {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        const animationFrame = requestAnimationFrame(() => {
            setIsVisible(true);
        });

        const onKeyDown = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        document.addEventListener('keydown', onKeyDown);

        return () => {
            cancelAnimationFrame(animationFrame);
            document.removeEventListener('keydown', onKeyDown);
            document.body.style.overflow = previousOverflow;
            setIsVisible(false);
        };
    }, [isOpen, onClose]);

    if (!isOpen || !imageSrc) {
        return null;
    }

    return (
        <div
            className={`fixed inset-0 z-[90] flex items-center justify-center bg-black/60 p-4 backdrop-blur-md transition-opacity duration-200 ${isVisible ? 'opacity-100' : 'opacity-0'}`}
            onClick={onClose}
            role="dialog"
            aria-modal="true"
            aria-label="Avatar preview"
        >
            <button
                type="button"
                onClick={onClose}
                className="absolute right-3 top-3 h-8 w-8 flex items-center justify-center rounded-full bg-red-50 text-red-500 hover:bg-red-100 hover:text-red-600 active:bg-red-200 transition-colors duration-200 shadow-sm hover:shadow-md focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                aria-label="Close modal"
            >
                <X className="h-4 w-4"/>
            </button>
            <img
                src={imageSrc}
                alt="Profile avatar preview"
                onClick={(event) => event.stopPropagation()}
                className={`max-h-[80vh] max-w-[80vw] rounded-2xl object-contain shadow-[0_28px_70px_rgba(0,0,0,0.55)] transition-all duration-200 ${isVisible ? 'scale-100' : 'scale-95'}`}
            />
        </div>
    );
};

export default AvatarPreviewModal;

