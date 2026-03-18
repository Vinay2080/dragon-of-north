import React, {useEffect, useState} from 'react';
import {ChevronUp} from 'react-feather';

export default function ScrollToTopButton() {
    const [isVisible, setIsVisible] = useState(false);

    // Show button when scrolled down
    useEffect(() => {
        const handleScroll = () => {
            if (window.scrollY > 300) {
                setIsVisible(true);
            } else {
                setIsVisible(false);
            }
        };

        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    // Smooth scroll to top
    const scrollToTop = () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    };

    return (
        <button
            onClick={scrollToTop}
            className={`
                fixed bottom-6 right-6 z-40
                p-3 rounded-lg
                bg-white dark:bg-slate-800
                border border-gray-200 dark:border-white/10
                text-gray-700 dark:text-white
                shadow-sm
                transition-all duration-200
                hover:scale-105 hover:shadow-md hover:border-gray-300 dark:hover:border-white/20
                ${isVisible ? 'opacity-100 translate-y-0 pointer-events-auto' : 'opacity-0 translate-y-4 pointer-events-none'}
            `}
            aria-label="Scroll to top"
            title="Scroll to top"
        >
            <ChevronUp size={20} strokeWidth={2.5}/>
        </button>
    );
}

