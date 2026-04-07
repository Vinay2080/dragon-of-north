const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;

export const validateAvatarFile = (file) => {
    if (!file) {
        return {isValid: false, message: 'Please choose an image to continue.'};
    }

    const normalizedType = (file.type || '').toLowerCase().trim();
    if (!ALLOWED_IMAGE_TYPES.has(normalizedType)) {
        return {isValid: false, message: 'Use a JPG, PNG, or WEBP image.'};
    }

    if (file.size > MAX_IMAGE_SIZE_BYTES) {
        return {isValid: false, message: 'Please upload an image smaller than 2MB.'};
    }

    return {isValid: true, message: ''};
};

const loadImage = (source) => {
    return new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => resolve(image);
        image.onerror = () => reject(new Error('Image could not be loaded.'));
        image.src = source;
    });
};

export const createObjectUrl = (file) => {
    return URL.createObjectURL(file);
};

export const revokeObjectUrl = (objectUrl) => {
    if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
    }
};

export const cropImageToBlob = async ({imageSrc, cropAreaPixels, mimeType = 'image/jpeg'}) => {
    if (!imageSrc || !cropAreaPixels) {
        throw new Error('Crop preview is unavailable. Please adjust the crop and try again.');
    }

    const image = await loadImage(imageSrc);
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(cropAreaPixels.width));
    canvas.height = Math.max(1, Math.round(cropAreaPixels.height));

    const context = canvas.getContext('2d');
    if (!context) {
        throw new Error('Preview is unavailable in this browser.');
    }

    context.drawImage(
        image,
        cropAreaPixels.x,
        cropAreaPixels.y,
        cropAreaPixels.width,
        cropAreaPixels.height,
        0,
        0,
        canvas.width,
        canvas.height
    );

    return new Promise((resolve, reject) => {
        canvas.toBlob((blob) => {
            if (!blob) {
                reject(new Error('Could not create cropped image preview.'));
                return;
            }
            resolve(blob);
        }, mimeType, 0.92);
    });
};

