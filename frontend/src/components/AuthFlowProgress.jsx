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
        <div className="mb-6 rounded-lg border border-slate-800 bg-slate-900/50 p-3">
            <p className="mb-3 text-xs text-slate-400">Authentication flow</p>
            <div className="grid grid-cols-2 gap-2">
                {steps.map((step, index) => {
                    const active = index === currentIndex;
                    const done = index < currentIndex;
                    return (
                        <div key={step.key} className={`rounded-md border px-2 py-2 text-xs ${done ? 'border-green-600/40 bg-green-950/40 text-green-300' : active ? 'border-blue-600/50 bg-blue-950/40 text-blue-200' : 'border-slate-800 bg-slate-950 text-slate-500'}`}>
                            <p className="font-semibold">{index + 1}. {step.label}</p>
                            <p>{step.hint}</p>
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default AuthFlowProgress;
