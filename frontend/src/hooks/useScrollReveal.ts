import {useEffect, useMemo, useRef, useState} from 'react';

interface UseScrollRevealOptions {
  threshold?: number;
  rootMargin?: string;
    once?: boolean;
}

const VELOCITY_SMOOTHING_CURRENT_WEIGHT = 0.8;
const VELOCITY_SMOOTHING_NEW_WEIGHT = 0.2;
const VELOCITY_MIN = 0.1;
const VELOCITY_MAX = 2;
const DURATION_MIN_MS = 250;
const DURATION_MAX_MS = 1000;
const SLOW_SCROLL_DURATION_MS = 880;
const FAST_SCROLL_DURATION_MS = 340;

let trackerConsumers = 0;
let trackerRunning = false;
let trackerRafId = 0;
let trackerLatestScrollY = 0;
let trackerLastScrollY = 0;
let trackerLastFrameTimestamp = 0;
let trackerSmoothedVelocity = VELOCITY_MIN;

const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

const updateSmoothedVelocity = (rawVelocity: number) => {
    trackerSmoothedVelocity =
        trackerSmoothedVelocity * VELOCITY_SMOOTHING_CURRENT_WEIGHT + rawVelocity * VELOCITY_SMOOTHING_NEW_WEIGHT;
};

const onScroll = () => {
    trackerLatestScrollY = window.scrollY || window.pageYOffset || 0;
};

const tickVelocityTracker = (timestamp: number) => {
    const deltaY = Math.abs(trackerLatestScrollY - trackerLastScrollY);

    if (trackerLastFrameTimestamp > 0) {
        const deltaTime = Math.max(timestamp - trackerLastFrameTimestamp, 1);
        const rawVelocity = deltaY / deltaTime;
        updateSmoothedVelocity(rawVelocity);
    }

    trackerLastScrollY = trackerLatestScrollY;
    trackerLastFrameTimestamp = timestamp;

    if (trackerRunning) {
        trackerRafId = window.requestAnimationFrame(tickVelocityTracker);
    }
};

const startVelocityTracker = () => {
    if (typeof window === 'undefined') {
        return;
    }

    trackerConsumers += 1;

    if (trackerRunning) {
        return;
    }

    trackerRunning = true;
    trackerLastFrameTimestamp = 0;
    trackerLatestScrollY = window.scrollY || window.pageYOffset || 0;
    trackerLastScrollY = trackerLatestScrollY;
    trackerSmoothedVelocity = VELOCITY_MIN;

    window.addEventListener('scroll', onScroll, {passive: true});
    trackerRafId = window.requestAnimationFrame(tickVelocityTracker);
};

const stopVelocityTracker = () => {
    if (typeof window === 'undefined') {
        return;
    }

    trackerConsumers = Math.max(trackerConsumers - 1, 0);

    if (trackerConsumers > 0 || !trackerRunning) {
        return;
    }

    trackerRunning = false;
    window.removeEventListener('scroll', onScroll);
    window.cancelAnimationFrame(trackerRafId);
    trackerRafId = 0;
};

const getRevealDurationMs = () => {
    const clampedVelocity = clamp(trackerSmoothedVelocity, VELOCITY_MIN, VELOCITY_MAX);
    const normalizedVelocity = (clampedVelocity - VELOCITY_MIN) / (VELOCITY_MAX - VELOCITY_MIN);
    const mappedDuration =
        SLOW_SCROLL_DURATION_MS - normalizedVelocity * (SLOW_SCROLL_DURATION_MS - FAST_SCROLL_DURATION_MS);

    return clamp(Math.round(mappedDuration), DURATION_MIN_MS, DURATION_MAX_MS);
};

/**
 * Hook to reveal elements when they enter the viewport
 * Adds 'visible' class to trigger CSS animations
 */
export const useScrollReveal = (options: UseScrollRevealOptions = {}) => {
    const {threshold = 0.15, rootMargin = '0px 0px -25% 0px', once = false} = options;
    const ref = useRef<HTMLElement>(null);
  const [isVisible, setIsVisible] = useState(false);
    const lastScrollYRef = useRef(0);
    const hasEnteredViewportRef = useRef(false);

  // Memoize observer options to avoid dependency issues
  const observerOptions = useMemo(() => ({
    threshold,
    rootMargin,
  }), [threshold, rootMargin]);

  useEffect(() => {
      startVelocityTracker();

      const element = ref.current;

      if (!element) {
          stopVelocityTracker();
          return;
      }

      lastScrollYRef.current = window.scrollY || window.pageYOffset || 0;

    const observer = new IntersectionObserver(
      ([entry]) => {
          lastScrollYRef.current = window.scrollY || window.pageYOffset || 0;
          const dynamicDurationMs = getRevealDurationMs();

          // Keep animation behavior stable and only adapt duration to scroll speed.
          element.style.transitionDuration = `${dynamicDurationMs}ms`;

        if (entry.isIntersecting) {
            hasEnteredViewportRef.current = true;
          setIsVisible(true);
            element.classList.add('revealed');
            element.classList.remove('hidden-up');
            element.classList.remove('hidden-down');
            return;
        }

          if (once && hasEnteredViewportRef.current) {
              return;
          }

          setIsVisible(false);
          element.classList.remove('revealed');

          if (hasEnteredViewportRef.current) {
              element.classList.remove('hidden-up');
              element.classList.remove('hidden-down');
              element.classList.add('hidden-up');
        }
      },
      observerOptions
    );

      observer.observe(element);

    return () => {
        observer.disconnect();
        stopVelocityTracker();
    };
  }, [observerOptions, once]);

  return { ref, isVisible };
};

/**
 * Hook for staggered animations of list items
 * @deprecated Currently unused, reserved for future card/item stagger animations
 */
export const useStaggerReveal = (options: UseScrollRevealOptions = {}) => {
    const {threshold = 0.4, rootMargin = '0px 0px -20% 0px', once = false} = options;
    const ref = useRef<HTMLElement>(null);
  const [itemsVisible, setItemsVisible] = useState<boolean[]>([]);
    const hasEnteredViewportRef = useRef(false);

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
            hasEnteredViewportRef.current = true;
          const children = Array.from(entry.target.children) as HTMLElement[];
          setItemsVisible(new Array(children.length).fill(true));
            return;
        }

          if (once && hasEnteredViewportRef.current) {
              return;
          }

          setItemsVisible([]);
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

