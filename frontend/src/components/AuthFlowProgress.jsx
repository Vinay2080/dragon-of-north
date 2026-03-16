import React from 'react';

const AuthFlowProgress = ({currentStep}) => {
  const steps = [
    {key: 'identifier', label: 'Identify', hint: 'Enter email or phone'},
    {key: 'signup', label: 'Create Account', hint: 'Set a secure password'},
    {key: 'otp', label: 'Verify OTP', hint: 'Confirm ownership'},
    {key: 'login', label: 'Login', hint: 'Access dashboard'},
  ];
  const currentIndex = steps.findIndex(step => step.key === currentStep);

  return (
    <div style={{marginBottom: '24px'}}>
      <p className="hero-eyebrow">Authentication Flow</p>
      <div className="feature-list">
        {steps.map((step, index) => {
          const active = index === currentIndex;
          const done = index < currentIndex;
          return (
            <div key={step.key} className={`feature-item ${active || done ? 'active' : ''}`}>
              <p>{index + 1}. {step.label}</p>
              <p className="feature-item-detail" style={{display: 'block'}}>{step.hint}</p>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AuthFlowProgress;
