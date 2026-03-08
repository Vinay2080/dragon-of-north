import { AnimatePresence, motion } from "framer-motion";
import {
  LogIn,
  Menu,
  Monitor,
  Moon,
  Settings,
  Shield,
  Sun,
  User,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";

type ThemeMode = "light" | "dark" | "system";

const THEME_SEQUENCE: ThemeMode[] = ["light", "dark", "system"];

const themeIcon = {
  light: Sun,
  dark: Moon,
  system: Monitor,
} as const;

const themeLabel = {
  light: "Light",
  dark: "Dark",
  system: "System",
} as const;

const Navbar = () => {
  const [theme, setTheme] = useState<ThemeMode>("system");
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement | null>(null);

  const ThemeIcon = themeIcon[theme];

  const profileItems = useMemo(
    () =>
      isAuthenticated
        ? ["Dashboard", "Active Sessions", "Settings", "Logout"]
        : ["Login", "Sign Up"],
    [isAuthenticated],
  );

  useEffect(() => {
    const savedTheme = localStorage.getItem("don-theme") as ThemeMode | null;

    if (savedTheme && THEME_SEQUENCE.includes(savedTheme)) {
      setTheme(savedTheme);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem("don-theme", theme);

    const root = document.documentElement;
    const systemPrefersDark =
      window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;

    root.style.transition = "background-color 250ms ease, color 250ms ease";

    if (theme === "dark" || (theme === "system" && systemPrefersDark)) {
      root.classList.add("dark");
      return;
    }

    root.classList.remove("dark");
  }, [theme]);

  useEffect(() => {
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    const syncSystemTheme = () => {
      if (theme !== "system") return;

      if (mediaQuery.matches) {
        document.documentElement.classList.add("dark");
      } else {
        document.documentElement.classList.remove("dark");
      }
    };

    syncSystemTheme();
    mediaQuery.addEventListener("change", syncSystemTheme);

    return () => mediaQuery.removeEventListener("change", syncSystemTheme);
  }, [theme]);

  useEffect(() => {
    const handleOutsideClick = (event: MouseEvent) => {
      if (!dropdownRef.current?.contains(event.target as Node)) {
        setIsProfileMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleOutsideClick);

    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  const cycleTheme = () => {
    const currentIndex = THEME_SEQUENCE.indexOf(theme);
    const nextTheme = THEME_SEQUENCE[(currentIndex + 1) % THEME_SEQUENCE.length];
    setTheme(nextTheme);
  };

  return (
    <header className="fixed inset-x-0 top-0 z-50 border-b border-white/20 bg-white/60 backdrop-blur-xl transition-colors duration-300 dark:border-white/10 dark:bg-slate-950/60">
      <nav className="mx-auto flex h-16 w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <button
          type="button"
          className="group inline-flex items-center gap-2 rounded-lg px-2 py-1.5 transition hover:bg-slate-900/5 dark:hover:bg-white/5"
        >
          <span className="rounded-md border border-cyan-300/50 bg-cyan-400/20 p-1 text-cyan-700 transition group-hover:scale-105 dark:border-cyan-300/30 dark:bg-cyan-300/10 dark:text-cyan-200">
            <Shield size={16} />
          </span>
          <span className="text-sm font-semibold tracking-wide text-slate-800 dark:text-slate-100 sm:text-base">
            Dragon of North
          </span>
        </button>

        <div className="hidden items-center gap-2 md:flex">
          <button
            type="button"
            onClick={cycleTheme}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-200/80 bg-white/70 px-3 py-2 text-sm text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:bg-white dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-900"
            aria-label="Cycle theme"
          >
            <ThemeIcon size={16} />
            <span>{themeLabel[theme]}</span>
          </button>

          <div className="relative" ref={dropdownRef}>
            <button
              type="button"
              onClick={() => setIsProfileMenuOpen((prev) => !prev)}
              className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200/80 bg-white/70 text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:bg-white dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200"
              aria-label="Open profile menu"
            >
              <User size={18} />
            </button>

            <AnimatePresence>
              {isProfileMenuOpen && (
                <motion.div
                  initial={{ opacity: 0, y: -8, scale: 0.98 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: -8, scale: 0.98 }}
                  transition={{ duration: 0.18, ease: "easeOut" }}
                  className="absolute right-0 mt-2 w-52 overflow-hidden rounded-xl border border-white/30 bg-white/50 p-2 shadow-2xl backdrop-blur-xl dark:border-white/10 dark:bg-slate-900/50"
                >
                  {profileItems.map((item) => (
                    <button
                      key={item}
                      type="button"
                      onClick={() => {
                        if (item === "Login") setIsAuthenticated(true);
                        if (item === "Logout") setIsAuthenticated(false);
                        setIsProfileMenuOpen(false);
                      }}
                      className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-white/50 dark:text-slate-200 dark:hover:bg-white/10"
                    >
                      {item === "Login" && <LogIn size={15} />}
                      {item === "Settings" && <Settings size={15} />}
                      {item}
                    </button>
                  ))}
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <button className="rounded-lg px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-900/5 dark:text-slate-100 dark:hover:bg-white/10">
            Login
          </button>
          <button className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-cyan-400">
            Sign Up
          </button>
        </div>

        <button
          type="button"
          onClick={() => setIsMobileMenuOpen((prev) => !prev)}
          className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-200/80 bg-white/70 text-slate-700 shadow-sm transition hover:bg-white dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 md:hidden"
          aria-label="Toggle mobile menu"
        >
          {isMobileMenuOpen ? <X size={18} /> : <Menu size={18} />}
        </button>
      </nav>

      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            transition={{ duration: 0.2, ease: "easeInOut" }}
            className="overflow-hidden border-t border-white/20 bg-white/55 px-4 pb-4 pt-2 backdrop-blur-xl dark:border-white/10 dark:bg-slate-950/65 md:hidden"
          >
            <div className="flex flex-col gap-2">
              <button
                type="button"
                onClick={cycleTheme}
                className="flex items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-white/60 dark:text-slate-200 dark:hover:bg-white/10"
              >
                <ThemeIcon size={16} />
                Theme: {themeLabel[theme]}
              </button>
              <button className="rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-white/60 dark:text-slate-200 dark:hover:bg-white/10">
                Login
              </button>
              <button className="rounded-lg bg-cyan-500 px-3 py-2 text-left text-sm font-semibold text-white transition hover:bg-cyan-400">
                Sign Up
              </button>
              <button
                type="button"
                onClick={() => setIsProfileMenuOpen((prev) => !prev)}
                className="flex items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-slate-700 transition hover:bg-white/60 dark:text-slate-200 dark:hover:bg-white/10"
              >
                <User size={16} />
                Profile
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
};

export default Navbar;
