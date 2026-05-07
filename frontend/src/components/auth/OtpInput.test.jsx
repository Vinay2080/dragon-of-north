import React from 'react';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import OtpInput from './OtpInput';

describe('OtpInput', () => {
    const defaultProps = {
        value: ['', '', '', '', '', ''],
        onChange: vi.fn(),
        length: 6,
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders the correct number of inputs', () => {
        render(<OtpInput {...defaultProps} />);
        const inputs = screen.getAllByRole('textbox');
        expect(inputs).toHaveLength(6);
    });

    it('renders with custom length', () => {
        render(<OtpInput {...defaultProps} value={['', '', '', '']} length={4} />);
        expect(screen.getAllByRole('textbox')).toHaveLength(4);
    });

    it('shows the provided values in inputs', () => {
        render(<OtpInput {...defaultProps} value={['1', '2', '3', '', '', '']} />);
        const inputs = screen.getAllByRole('textbox');
        expect(inputs[0]).toHaveValue('1');
        expect(inputs[1]).toHaveValue('2');
        expect(inputs[2]).toHaveValue('3');
        expect(inputs[3]).toHaveValue('');
    });

    it('calls onChange with updated value when a digit is typed', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        fireEvent.change(inputs[0], { target: { value: '5' } });
        expect(onChange).toHaveBeenCalledWith(['5', '', '', '', '', '']);
    });

    it('ignores non-digit input', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        fireEvent.change(inputs[0], { target: { value: 'a' } });
        expect(onChange).not.toHaveBeenCalled();
    });

    it('clears cell when empty string is provided', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} value={['9', '', '', '', '', '']} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        fireEvent.change(inputs[0], { target: { value: '' } });
        expect(onChange).toHaveBeenCalledWith(['', '', '', '', '', '']);
    });

    it('does not call onChange when disabled', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} disabled />);
        const inputs = screen.getAllByRole('textbox');
        fireEvent.change(inputs[0], { target: { value: '7' } });
        expect(onChange).not.toHaveBeenCalled();
    });

    it('marks inputs as aria-invalid when error is true', () => {
        render(<OtpInput {...defaultProps} error />);
        const inputs = screen.getAllByRole('textbox');
        inputs.forEach((input) => {
            expect(input).toHaveAttribute('aria-invalid', 'true');
        });
    });

    it('marks inputs as aria-invalid=false when no error', () => {
        render(<OtpInput {...defaultProps} />);
        const inputs = screen.getAllByRole('textbox');
        inputs.forEach((input) => {
            expect(input).toHaveAttribute('aria-invalid', 'false');
        });
    });

    it('handles paste of digits filling the OTP array', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        const pasteEvent = {
            preventDefault: vi.fn(),
            clipboardData: { getData: () => '123456' },
        };
        fireEvent.paste(inputs[0], pasteEvent);
        expect(onChange).toHaveBeenCalledWith(['1', '2', '3', '4', '5', '6']);
    });

    it('handles paste of fewer digits than length', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        const pasteEvent = {
            preventDefault: vi.fn(),
            clipboardData: { getData: () => '123' },
        };
        fireEvent.paste(inputs[0], pasteEvent);
        expect(onChange).toHaveBeenCalledWith(['1', '2', '3', '', '', '']);
    });

    it('ignores paste when disabled', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} disabled />);
        const inputs = screen.getAllByRole('textbox');
        const pasteEvent = {
            preventDefault: vi.fn(),
            clipboardData: { getData: () => '123456' },
        };
        fireEvent.paste(inputs[0], pasteEvent);
        expect(onChange).not.toHaveBeenCalled();
    });

    it('ignores paste when clipboard has no digits', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        const pasteEvent = {
            preventDefault: vi.fn(),
            clipboardData: { getData: () => 'abc' },
        };
        fireEvent.paste(inputs[0], pasteEvent);
        expect(onChange).not.toHaveBeenCalled();
    });

    it('handles backspace on filled cell — clears it', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} value={['1', '2', '', '', '', '']} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        fireEvent.keyDown(inputs[1], { key: 'Backspace' });
        expect(onChange).toHaveBeenCalledWith(['1', '', '', '', '', '']);
    });

    it('handles backspace on empty cell — clears previous cell', () => {
        const onChange = vi.fn();
        render(<OtpInput {...defaultProps} value={['1', '', '', '', '', '']} onChange={onChange} />);
        const inputs = screen.getAllByRole('textbox');
        fireEvent.keyDown(inputs[1], { key: 'Backspace' });
        expect(onChange).toHaveBeenCalledWith(['', '', '', '', '', '']);
    });

    it('calls onComplete when autoSubmit and all digits filled', () => {
        const onChange = vi.fn();
        const onComplete = vi.fn();
        render(
            <OtpInput
                value={['1', '2', '3', '4', '5', '']}
                onChange={onChange}
                autoSubmit
                onComplete={onComplete}
            />
        );
        const inputs = screen.getAllByRole('textbox');
        fireEvent.change(inputs[5], { target: { value: '6' } });
        expect(onComplete).toHaveBeenCalledWith('123456');
    });

    it('does not call onComplete when autoSubmit but not all digits filled', () => {
        const onChange = vi.fn();
        const onComplete = vi.fn();
        render(
            <OtpInput
                value={['1', '', '', '', '', '']}
                onChange={onChange}
                autoSubmit
                onComplete={onComplete}
            />
        );
        const inputs = screen.getAllByRole('textbox');
        fireEvent.change(inputs[1], { target: { value: '2' } });
        expect(onComplete).not.toHaveBeenCalled();
    });

    it('renders inputs with correct aria-label', () => {
        render(<OtpInput {...defaultProps} idPrefix="test" />);
        for (let i = 1; i <= 6; i++) {
            expect(screen.getByLabelText(`OTP digit ${i}`)).toBeInTheDocument();
        }
    });

    it('renders inputs with correct ids', () => {
        render(<OtpInput {...defaultProps} idPrefix="code" />);
        for (let i = 0; i < 6; i++) {
            expect(document.getElementById(`code-${i}`)).toBeInTheDocument();
        }
    });
});
