const CallToAction = () => (
  <section className="card card-promo reveal">
    <h2>Security Monitoring, Built for Auth Demos</h2>
    <p style={{marginTop: '12px'}}>Test JWT, OAuth2, OTP, session invalidation, and device control without losing visual clarity.</p>
    <div style={{marginTop: '24px', display: 'flex', gap: '12px'}}>
      <a href="/login" className="btn btn-hero">Start Demo</a>
      <a href="/architecture" className="btn btn-hero-ghost">View Architecture</a>
    </div>
  </section>
);

export default CallToAction;
