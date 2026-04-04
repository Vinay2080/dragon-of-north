import React from 'react';
import SecuritySection from './SecuritySection';

const ProfileSettings = ({authProvider}) => {
    return <SecuritySection authProvider={authProvider}/>;
};

export default ProfileSettings;

