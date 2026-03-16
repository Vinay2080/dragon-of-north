import Navbar from '../components/Navbar';
import HeroSection from '../components/HeroSection';
import SessionSecurityGame from '../components/SessionSecurityGame';
import FeaturesSection from '../components/FeaturesSection';
import CallToAction from '../components/CallToAction';
import Footer from '../components/Footer';
import AuthorSection from '../components/AuthorSection';

const LandingPage = () => {
  return (
    <div className="dashboard-shell">
      <div className="banner">Security-first authentication demo • JWT, OAuth2, OTP • Live session control</div>
      <Navbar />
      <main>
        <HeroSection />
        <section id="session-game" style={{maxWidth: '1280px', margin: '0 auto', padding: '0 32px 32px'}}>
          <SessionSecurityGame />
        </section>
        <section style={{maxWidth: '1280px', margin: '0 auto', padding: '0 32px 32px'}}>
          <FeaturesSection />
        </section>
        <section style={{maxWidth: '1280px', margin: '0 auto', padding: '0 32px 32px'}}><CallToAction /></section>
        <section style={{maxWidth: '1280px', margin: '0 auto', padding: '0 32px 32px'}}><AuthorSection /></section>
      </main>
      <Footer />
    </div>
  );
};

export default LandingPage;
