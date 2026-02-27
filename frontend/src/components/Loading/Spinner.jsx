import React from 'react';

const Spinner = ({size = 'md'}) => {
    const sizeClass = size === 'sm' ? 'h-4 w-4' : size === 'lg' ? 'h-8 w-8' : 'h-6 w-6';
    return <span className={`${sizeClass} inline-block animate-spin rounded-full border-2 border-slate-300 border-t-transparent`}/>;
};

export default Spinner;
