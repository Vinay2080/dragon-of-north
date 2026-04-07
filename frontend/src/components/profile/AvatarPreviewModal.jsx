import React, {useEffect, useState} from 'react';

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
                className="absolute right-4 top-4 rounded-full border border-white/25 bg-black/40 px-3 py-1 text-xl font-semibold text-white transition hover:bg-black/55 focus:outline-none focus:ring-2 focus:ring-white/50"
                aria-label="Close avatar preview"
            >
                x
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

