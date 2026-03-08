import Navbar from '../components/Navbar';
import HeroSection from '../components/HeroSection';
import SessionSecurityGame from '../components/SessionSecurityGame';
import FeaturesSection from '../components/FeaturesSection';
import CallToAction from '../components/CallToAction';
import Footer from '../components/Footer';

const LandingPage = () => {
    return (
        <div className="min-h-screen bg-gradient-to-b from-slate-950 via-slate-900 to-slate-950 text-slate-100">
            <Navbar />

            <main className="mx-auto w-full max-w-[1600px] px-4 pb-12 pt-20 sm:px-6 lg:px-8">
                <section className="mb-12 sm:mb-16">
                    <HeroSection />
                </section>

                <section className="mb-12 sm:mb-16">
                    <SessionSecurityGame />
                </section>

                <section className="mb-12 sm:mb-16">
                    <FeaturesSection />
                </section>

                <section className="mb-12 sm:mb-16">
                    <CallToAction />
                </section>
            </main>

            <Footer />
        </div>
    );
};

export default LandingPage;
