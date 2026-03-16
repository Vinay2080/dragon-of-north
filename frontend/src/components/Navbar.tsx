import {Menu, Monitor, Moon, Sun, X} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useTheme} from '../context/ThemeContext';

type ThemeMode = 'light' | 'dark' | 'system';
const THEME_SEQUENCE: ThemeMode[] = ['light', 'dark', 'system'];
const themeIcon = {light: Sun, dark: Moon, system: Monitor} as const;

const Navbar = () => {
  const {theme, setTheme} = useTheme();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const navigate = useNavigate();
  const ThemeIcon = themeIcon[theme];

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const cycleTheme = () => {
    const currentIndex = THEME_SEQUENCE.indexOf(theme);
    setTheme(THEME_SEQUENCE[(currentIndex + 1) % THEME_SEQUENCE.length]);
  };

  return (
    <header className={`navbar ${scrolled ? 'scrolled' : ''}`}>
      <button type="button" onClick={() => navigate('/')} className="navbar-brand">
        <span className="navbar-brand-glyph">&gt;_</span>
        DragonOfNorth
      </button>

      <div className="navbar-links" style={{display: isMobileMenuOpen ? 'flex' : undefined}}>
        <a href="#session-game">Sessions</a>
        <a href="/architecture">Architecture</a>
      </div>

      <div className="navbar-actions">
        <button type="button" onClick={cycleTheme} className="btn btn-subtle"><ThemeIcon size={14} />{theme}</button>
        <button type="button" onClick={() => navigate('/login')} className="btn btn-primary">Login / Signup</button>
        <button type="button" onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} className="btn btn-subtle md:hidden">
          {isMobileMenuOpen ? <X size={16} /> : <Menu size={16} />}
        </button>
      </div>
    </header>
  );
};

export default Navbar;
