import React from 'react';

const Skeleton = ({className = ''}) => (
    <div className={`animate-pulse rounded-md bg-slate-800/80 ${className}`}/>
);

export default Skeleton;
