import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import ForgotPasswordRequestPage from '../ForgotPasswordRequestPage';
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

describe('ForgotPasswordRequestPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits reset request and navigates to reset page on success', async () => {
    apiService.post.mockResolvedValue({
      api_response_status: 'success',
      message: 'OTP sent',
    });

    render(
      <MemoryRouter>
        <ForgotPasswordRequestPage />
      </MemoryRouter>
    );

    await userEvent.selectOptions(screen.getByRole('combobox'), 'EMAIL');
    await userEvent.type(screen.getByPlaceholderText('name@example.com'), 'user@example.com');
    await userEvent.click(screen.getByRole('button', {name: 'Send OTP'}));

    await waitFor(() => {
      expect(apiService.post).toHaveBeenCalledWith('/api/v1/auth/password/forgot/request', {
        identifier: 'user@example.com',
        identifier_type: 'EMAIL',
      });
      expect(mockNavigate).toHaveBeenCalledWith('/reset-password', {
        state: {
          identifier: 'user@example.com',
          identifierType: 'EMAIL',
        },
      });
    });
  });

  it('shows error when API fails', async () => {
    apiService.post.mockResolvedValue({
      api_response_status: 'failed',
      message: 'rate limited',
    });

    render(
      <MemoryRouter>
        <ForgotPasswordRequestPage />
      </MemoryRouter>
    );

    await userEvent.type(screen.getByPlaceholderText('name@example.com'), 'user@example.com');
    await userEvent.click(screen.getByRole('button', {name: 'Send OTP'}));

    expect(await screen.findByText('rate limited')).toBeInTheDocument();
  });
});
