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
        <div
            className="mb-6 rounded-xl border border-gray-200 bg-white p-4 shadow-sm dark:border-neutral-700 dark:bg-neutral-900 dark:shadow-lg dark:shadow-purple-500/10">
            <p className="mb-3 text-xs uppercase tracking-[0.12em] text-gray-500 dark:text-neutral-400">Authentication
                flow</p>
            <div className="grid grid-cols-2 gap-2">
                {steps.map((step, index) => {
                    const active = index === currentIndex;
                    const done = index < currentIndex;
                    const stepStateClass = done
                        ? 'auth-step auth-step--done'
                        : active
                            ? 'auth-step auth-step--active'
                            : 'auth-step auth-step--inactive';

                    return (
                        <div key={step.key} className={stepStateClass}>
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
