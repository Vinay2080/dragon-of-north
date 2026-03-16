const HeroSection = () => {
  return (
    <section className="hero">
      <div>
        <div className="hero-eyebrow">DragonOfNorth Security Platform</div>
        <h1>Control Every Session. Validate Every Login Flow.</h1>
        <p className="hero-subtitle">
          A security-focused authentication demo for device sessions, JWT/OAuth2 architecture,
          and monitoring workflows.
        </p>
        <div className="hero-actions">
          <a href="#session-game" className="btn btn-hero">Open Session Game</a>
          <a href="https://dragon-api.duckdns.org/swagger-ui/index.html#/" target="_blank" rel="noreferrer" className="btn btn-hero-ghost">API Documentation</a>
        </div>
        <p className="hero-footnote">jwt=<code className="token-value">eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</code></p>
      </div>

      <div className="card card-security reveal">
        <div>
          <h3>Live Session Snapshot</h3>
          <p>Current IP <span className="ip-addr">10.24.16.88</span></p>
          <p>Last activity <span className="timestamp">2026-03-16T09:41:00Z</span></p>
        </div>
        <div>
          <span className="badge badge-success">Active session</span>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
