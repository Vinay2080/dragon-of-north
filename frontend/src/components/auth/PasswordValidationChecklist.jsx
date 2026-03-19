import React, {useMemo} from 'react';

const PasswordValidationChecklist = ({password = ''}) => {
    const requirements = useMemo(() => [
        {
            key: 'length',
            label: 'At least 8 characters',
            test: (pwd) => pwd.length >= 8,
        },
        {
            key: 'uppercase',
            label: 'At least one uppercase letter',
            test: (pwd) => /[A-Z]/.test(pwd),
        },
        {
            key: 'lowercase',
            label: 'At least one lowercase letter',
            test: (pwd) => /[a-z]/.test(pwd),
        },
        {
            key: 'number',
            label: 'At least one number',
            test: (pwd) => /\d/.test(pwd),
        },
        {
            key: 'special',
            label: 'At least one special character',
            test: (pwd) => /[!@#$%^&*(),.?":{}|<>]/.test(pwd),
        },
    ], []);

    // Show checklist only if the user has started typing
    if (!password) return null;

    return (
        <div className="password-validation-checklist" role="status" aria-live="polite" aria-atomic="true">
            <ul className="space-y-2">
                {requirements.map((req) => {
                    const met = req.test(password);
                    return (
                        <li
                            key={req.key}
                            className={`password-requirement ${met ? 'met' : 'unmet'}`}
                        >
                            <span className="requirement-icon" aria-hidden="true">
                                {met ? '✔' : '✖'}
                            </span>
                            <span className="requirement-text">{req.label}</span>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default PasswordValidationChecklist;


