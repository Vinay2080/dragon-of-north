import {describe, expect, it} from 'vitest';
import {validateAvatarFile} from './avatarCrop';

describe('validateAvatarFile', () => {
    it('rejects missing file', () => {
        const result = validateAvatarFile(null);
        expect(result.isValid).toBe(false);
    });

    it('rejects unsupported mime type', () => {
        const file = new File(['hello'], 'avatar.gif', {type: 'image/gif'});
        const result = validateAvatarFile(file);
        expect(result.isValid).toBe(false);
    });

    it('accepts supported file under 2MB', () => {
        const file = new File(['hello'], 'avatar.png', {type: 'image/png'});
        const result = validateAvatarFile(file);
        expect(result.isValid).toBe(true);
    });
});

