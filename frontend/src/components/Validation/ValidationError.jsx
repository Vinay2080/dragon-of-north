import React from 'react';

const ValidationError = ({errors = [], id}) => {
    if (!errors.length) {
        return null;
    }

    return (
        <ul id={id} className="mt-2 list-disc pl-5 space-y-1" role="alert" aria-live="polite">
            {errors.map((err, index) => (
                <li key={`${err}-${index}`} className="text-sm text-red-400">{err}</li>
            ))}
        </ul>
    );
};

export default ValidationError;
