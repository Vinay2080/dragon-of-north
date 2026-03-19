import {useEffect, useRef, useState} from 'react';

export function useInView(options = {}) {
    const ref = useRef(null);
    const [isVisible, setIsVisible] = useState(false);
    const {
        threshold = 0.15,
        rootMargin = '0px 0px -20% 0px',
        once = true,
        root = null,
    } = options;

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (!entry.isIntersecting) {
                    return;
                }

                setIsVisible(true);

                if (once) {
                    observer.unobserve(entry.target);
                }
            },
            {
                threshold,
                rootMargin,
                root,
            }
        );

        if (ref.current) {
            observer.observe(ref.current);
        }

        return () => observer.disconnect();
    }, [once, root, rootMargin, threshold]);

    return [ref, isVisible];
}


