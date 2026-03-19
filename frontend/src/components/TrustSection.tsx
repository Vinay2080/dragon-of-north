import {Reveal} from './Reveal';

const TrustSection = () => {
    const technologies = ['Spring Boot', 'JWT Authentication', 'Redis', 'PostgreSQL'];
    const capabilities = [
        'Real-time session tracking',
        'Device-level visibility',
        'Instant session revocation',
    ];

    return (
        <Reveal>
            <section className="trust-section py-12 sm:py-16">
                <div className="mx-auto max-w-4xl">
                    <p className="trust-label">Built with Enterprise-Grade Technologies</p>

                    {/* Technologies Row */}
                    <div className="trust-tech">
                        {technologies.map((tech, index) => (
                            <div key={index} className="trust-tech-item">
                                <span className="trust-tech-label">{tech}</span>
                            </div>
                        ))}
                    </div>

                    {/* Separator */}
                    <div className="flex justify-center mb-8">
                        <div className="h-px w-24 bg-gradient-to-r from-transparent via-slate-400/30 to-transparent"/>
                    </div>

                    {/* Capabilities Row */}
                    <div className="trust-features">
                        {capabilities.map((capability, index) => (
                            <div key={index} className="trust-capability-item">
                                <span className="trust-capability-label">{capability}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </section>
        </Reveal>
    );
};

export default TrustSection;

