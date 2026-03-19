import { useEffect, useMemo, useRef, useState } from 'react';

interface UseScrollRevealOptions {
  threshold?: number;
  rootMargin?: string;
}

/**
 * Hook to reveal elements when they enter the viewport
 * Adds 'visible' class to trigger CSS animations
 */
export const useScrollReveal = (options: UseScrollRevealOptions = {}) => {
  const { threshold = 0.1, rootMargin = '0px' } = options;
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  // Memoize observer options to avoid dependency issues
  const observerOptions = useMemo(() => ({
    threshold,
    rootMargin,
  }), [threshold, rootMargin]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          // Optional: Stop observing after the first reveal
          observer.unobserve(entry.target);
        }
      },
      observerOptions
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => {
      if (ref.current) {
        observer.unobserve(ref.current);
      }
    };
  }, [observerOptions]);

  return { ref, isVisible };
};

/**
 * Hook for staggered animations of list items
 */
export const useStaggerReveal = (options: UseScrollRevealOptions = {}) => {
  const { threshold = 0.1, rootMargin = '0px' } = options;
  const ref = useRef<HTMLDivElement>(null);
  const [itemsVisible, setItemsVisible] = useState<boolean[]>([]);

  // Memoize observer options to avoid dependency issues
  const observerOptions = useMemo(() => ({
    threshold,
    rootMargin,
  }), [threshold, rootMargin]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          const children = Array.from(entry.target.children) as HTMLElement[];
          setItemsVisible(new Array(children.length).fill(true));
          observer.unobserve(entry.target);
        }
      },
      observerOptions
    );

    if (ref.current) {
      observer.observe(ref.current);
    }

    return () => {
      if (ref.current) {
        observer.unobserve(ref.current);
      }
    };
  }, [observerOptions]);

  return { ref, itemsVisible };
};

