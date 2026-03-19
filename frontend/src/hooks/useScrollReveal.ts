import {useEffect, useMemo, useRef, useState} from 'react';

interface UseScrollRevealOptions {
  threshold?: number;
  rootMargin?: string;
    once?: boolean;
}

/**
 * Hook to reveal elements when they enter the viewport
 * Adds 'visible' class to trigger CSS animations
 */
export const useScrollReveal = (options: UseScrollRevealOptions = {}) => {
    const {threshold = 0.15, rootMargin = '0px 0px -20% 0px', once = true} = options;
    const ref = useRef<HTMLElement | null>(null);
  const [isVisible, setIsVisible] = useState(false);

  // Memoize observer options to avoid dependency issues
  const observerOptions = useMemo(() => ({
    threshold,
    rootMargin,
  }), [threshold, rootMargin]);

  useEffect(() => {
      const element = ref.current;

      if (!element) {
          return;
      }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
            setIsVisible(true);

            if (once) {
                observer.unobserve(entry.target);
            }
            return;
        }

          if (!once) {
              setIsVisible(false);
        }
      },
      observerOptions
    );

      observer.observe(element);

    return () => {
        observer.disconnect();
    };
  }, [observerOptions, once]);

  return { ref, isVisible };
};

/**
 * Hook for staggered animations of list items
 * @deprecated Currently unused, reserved for future card/item stagger animations
 */
export const useStaggerReveal = (options: UseScrollRevealOptions = {}) => {
    const {threshold = 0.15, rootMargin = '0px 0px -20% 0px', once = true} = options;
    const ref = useRef<HTMLElement | null>(null);
  const [itemsVisible, setItemsVisible] = useState<boolean[]>([]);

  // Memoize observer options to avoid dependency issues
  const observerOptions = useMemo(() => ({
    threshold,
    rootMargin,
  }), [threshold, rootMargin]);

  useEffect(() => {
      const element = ref.current;

      if (!element) {
          return;
      }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          const children = Array.from(entry.target.children) as HTMLElement[];
          setItemsVisible(new Array(children.length).fill(true));

            if (once) {
                observer.unobserve(entry.target);
            }
            return;
        }

          if (!once) {
              setItemsVisible([]);
          }
      },
      observerOptions
    );

      observer.observe(element);

    return () => {
        observer.disconnect();
    };
  }, [observerOptions, once]);

  return { ref, itemsVisible };
};

