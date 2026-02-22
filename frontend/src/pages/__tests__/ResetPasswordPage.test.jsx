import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter, Routes, Route} from 'react-router-dom';
import ResetPasswordPage from '../ResetPasswordPage';
import {apiService} from '../../services/apiService';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../services/apiService', () => ({
  apiService: {
    post: vi.fn(),
  },
}));

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  const renderPage = () => render(
    <MemoryRouter initialEntries={[{pathname: '/reset-password', state: {identifier: 'user@example.com', identifierType: 'EMAIL'}}]}>
      <Routes>
        <Route path="/reset-password" element={<ResetPasswordPage />} />
      </Routes>
    </MemoryRouter>
  );

  it('submits reset confirmation and redirects to login on success', async () => {
    apiService.post.mockResolvedValue({
      api_response_status: 'success',
      message: 'Password reset successful',
    });

    renderPage();

    await userEvent.type(screen.getByPlaceholderText('6 digit OTP'), '123456');
    const passwordInput = document.querySelector('input[type="password"]');
    await userEvent.type(passwordInput, 'NewPass@123');
    await userEvent.click(screen.getByRole('button', {name: 'Reset Password'}));

    await waitFor(() => {
      expect(apiService.post).toHaveBeenCalledWith('/api/v1/auth/password/forgot/reset', {
        identifier: 'user@example.com',
        identifier_type: 'EMAIL',
        otp: '123456',
        new_password: 'NewPass@123',
      });
    });

    vi.advanceTimersByTime(1200);
    expect(mockNavigate).toHaveBeenCalledWith('/login', {state: {identifier: 'user@example.com'}});
  });

  it('shows API failure message', async () => {
    apiService.post.mockResolvedValue({
      api_response_status: 'failed',
      message: 'Invalid OTP',
    });

    renderPage();

    await userEvent.type(screen.getByPlaceholderText('6 digit OTP'), '123456');
    const passwordInput = document.querySelector('input[type="password"]');
    await userEvent.type(passwordInput, 'NewPass@123');
    await userEvent.click(screen.getByRole('button', {name: 'Reset Password'}));

    expect(await screen.findByText('Invalid OTP')).toBeInTheDocument();
  });
});
