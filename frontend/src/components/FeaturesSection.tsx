import {useState} from 'react';

const features = [
  {title: 'Session Tracking', description: 'Monitor every authenticated device and geolocation path.'},
  {title: 'Secure Tokens', description: 'Inspect JWT payloads, signature validation, and token lifecycle states.'},
  {title: 'Instant Revocation', description: 'Revoke suspicious sessions immediately from the security dashboard.'},
];

const FeaturesSection = () => {
  const [active, setActive] = useState(0);

  return (
    <section className="reveal">
      <h2>Core Security Features</h2>
      <p style={{margin: '8px 0 24px'}}>Purpose-built controls for session-first account security.</p>
      <div className="feature-list">
        {features.map((feature, index) => (
          <div key={feature.title} className={`feature-item ${active === index ? 'active' : ''}`} onClick={() => setActive(index)}>
            {feature.title}
            <div className="feature-item-detail">{feature.description}</div>
          </div>
        ))}
      </div>
    </section>
  );
};

export default FeaturesSection;
