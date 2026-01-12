import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Regex for basic email validation.
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Regex for basic phone validation (E.164 format roughly, or 10-15 digits).
 */
const PHONE_REGEX = /^\+?[0-9]{10,15}$/;

const API_BASE_URL = 'http://localhost:8080'; // Update this to match your backend port

const AuthIdentifierPage = () => {
  const [identifier, setIdentifier] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [blockedMessage, setBlockedMessage] = useState('');
  const navigate = useNavigate();

  const detectIdentifierType = (value) => {
    if (EMAIL_REGEX.test(value)) return 'EMAIL';
    if (PHONE_REGEX.test(value)) return 'PHONE';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setBlockedMessage('');

    let processedIdentifier = identifier.trim();
    const identifierType = detectIdentifierType(processedIdentifier);
    
    // If it's a phone number and doesn't start with +, add +91 country code
    if (identifierType === 'PHONE' && !processedIdentifier.startsWith('+')) {
      // Remove any existing country code if present (e.g., 91 at start)
      processedIdentifier = `+91${processedIdentifier.replace(/^\+?91?/, '')}`;
    }

    if (!identifierType) {
      setError('Please enter a valid email or phone number.');
      return;
    }

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/identifier/status`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          identifier: processedIdentifier,
          identifier_type: identifierType,
        }),
      });

      if (!response.ok) {
        // Handle non-2xx responses
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Network response was not ok');
      }

      const result = await response.json();

      // Read response using snake_case keys as per requirement
      if (result.api_response_status === 'success') {
        const status = result.data.app_user_status;

        switch (status) {
          case 'NOT_EXIST':
            navigate('/signup');
            break;
          case 'CREATED':
            navigate('/otp');
            break;
          case 'VERIFIED':
            navigate('/dashboard');
            break;
          case 'BLOCKED':
            setBlockedMessage('Your account is blocked. Please contact support.');
            break;
          default:
            setError(`Unexpected user status: ${status}`);
        }
      } else {
        setError(result.message || 'Something went wrong');
      }
    } catch (err) {
      setError(err.message || 'Failed to connect to the server. Please try again later.');
      console.error('API Error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '400px', margin: '0 auto' }}>
      <h2>Sign In / Sign Up</h2>
      {blockedMessage ? (
        <div style={{ 
          padding: '1rem', 
          backgroundColor: '#fee2e2', 
          color: '#b91c1c', 
          borderRadius: '4px',
          border: '1px solid #fecaca'
        }}>
          {blockedMessage}
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <div style={{ marginBottom: '1rem' }}>
            <label htmlFor="identifier" style={{ display: 'block', marginBottom: '0.5rem' }}>
              Email or Phone Number
            </label>
            <input
              id="identifier"
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              disabled={loading}
              placeholder="e.g., user@example.com or +1234567890"
              style={{ 
                width: '100%', 
                padding: '0.5rem', 
                boxSizing: 'border-box',
                borderRadius: '4px',
                border: '1px solid #ccc'
              }}
              required
            />
          </div>

          {error && (
            <div style={{ color: '#dc2626', fontSize: '0.875rem', marginBottom: '1rem' }}>
              {error}
            </div>
          )}

          <button 
            type="submit" 
            disabled={loading || !identifier.trim()}
            style={{
              width: '100%',
              padding: '0.75rem',
              backgroundColor: loading ? '#94a3b8' : '#2563eb',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontWeight: 'bold'
            }}
          >
            {loading ? 'Processing...' : 'Continue'}
          </button>
        </form>
      )}
    </div>
  );
};

export default AuthIdentifierPage;
