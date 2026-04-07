import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import Cropper from 'react-easy-crop';
import {X} from 'lucide-react';
import {API_CONFIG} from '../../config';
import {apiService} from '../../services/apiService';
import {createObjectUrl, cropImageToBlob, revokeObjectUrl, validateAvatarFile} from '../../utils/avatarCrop';

const INITIAL_CROP = {x: 0, y: 0};
const INITIAL_ZOOM = 1;

const getResponseData = (result) => {
    if (result?.data && typeof result.data === 'object' && !Array.isArray(result.data)) {
        return result.data;
    }

    if (result && typeof result === 'object' && !Array.isArray(result) && !result.type) {
        return result;
    }

    return null;
};

const AvatarUploadModal = ({isOpen, onClose, onUploadSuccess, currentAvatarSrc}) => {
    const [selectedFile, setSelectedFile] = useState(null);
    const [sourceImageUrl, setSourceImageUrl] = useState('');
    const [crop, setCrop] = useState(INITIAL_CROP);
    const [zoom, setZoom] = useState(INITIAL_ZOOM);
    const [croppedAreaPixels, setCroppedAreaPixels] = useState(null);
    const [croppedPreviewUrl, setCroppedPreviewUrl] = useState('');
    const [croppedBlob, setCroppedBlob] = useState(null);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadSuccess, setUploadSuccess] = useState(false);
    const [uploadError, setUploadError] = useState('');
    const [fileError, setFileError] = useState('');
    const [previewError, setPreviewError] = useState('');
    const [isBuildingPreview, setIsBuildingPreview] = useState(false);

    const inputRef = useRef(null);
    const isFlowActiveRef = useRef(false);
    const previewGenerationIdRef = useRef(0);
    const sourceImageUrlRef = useRef('');
    const croppedPreviewUrlRef = useRef('');

    useEffect(() => {
        sourceImageUrlRef.current = sourceImageUrl;
    }, [sourceImageUrl]);

    useEffect(() => {
        croppedPreviewUrlRef.current = croppedPreviewUrl;
    }, [croppedPreviewUrl]);

    const resetTemporaryState = useCallback(() => {
        revokeObjectUrl(sourceImageUrlRef.current);
        revokeObjectUrl(croppedPreviewUrlRef.current);
        sourceImageUrlRef.current = '';
        croppedPreviewUrlRef.current = '';
        setSelectedFile(null);
        setSourceImageUrl('');
        setCrop(INITIAL_CROP);
        setZoom(INITIAL_ZOOM);
        setCroppedAreaPixels(null);
        setCroppedPreviewUrl('');
        setCroppedBlob(null);
        setIsUploading(false);
        setUploadSuccess(false);
        setUploadError('');
        setFileError('');
        setPreviewError('');
        setIsBuildingPreview(false);
        previewGenerationIdRef.current += 1;
        if (inputRef.current) {
            inputRef.current.value = '';
        }
    }, []);

    const closeModal = useCallback(() => {
        resetTemporaryState();
        onClose();
    }, [onClose, resetTemporaryState]);

    useEffect(() => {
        if (!isOpen) {
            return;
        }

        isFlowActiveRef.current = true;
        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        return () => {
            isFlowActiveRef.current = false;
            document.body.style.overflow = previousOverflow;
            resetTemporaryState();
        };
    }, [isOpen, resetTemporaryState]);

    const onCropComplete = useCallback((_, areaPixels) => {
        setCroppedAreaPixels(areaPixels);
    }, []);

    useEffect(() => {
        if (!sourceImageUrl || !croppedAreaPixels) {
            setCroppedBlob(null);
            setCroppedPreviewUrl((previousUrl) => {
                revokeObjectUrl(previousUrl);
                return '';
            });
            return;
        }

        const generationId = previewGenerationIdRef.current + 1;
        previewGenerationIdRef.current = generationId;
        setIsBuildingPreview(true);
        setPreviewError('');

        const buildPreview = async () => {
            try {
                const blob = await cropImageToBlob({
                    imageSrc: sourceImageUrl,
                    cropAreaPixels: croppedAreaPixels,
                    mimeType: selectedFile?.type || 'image/jpeg',
                });

                if (!isFlowActiveRef.current || previewGenerationIdRef.current !== generationId) {
                    return;
                }

                const previewUrl = createObjectUrl(blob);
                setCroppedBlob(blob);
                setCroppedPreviewUrl((previousUrl) => {
                    revokeObjectUrl(previousUrl);
                    return previewUrl;
                });
            } catch (error) {
                if (!isFlowActiveRef.current || previewGenerationIdRef.current !== generationId) {
                    return;
                }

                setCroppedBlob(null);
                setCroppedPreviewUrl((previousUrl) => {
                    revokeObjectUrl(previousUrl);
                    return '';
                });
                setPreviewError(error?.message || 'Preview is unavailable for this image. Try another file.');
            } finally {
                if (isFlowActiveRef.current && previewGenerationIdRef.current === generationId) {
                    setIsBuildingPreview(false);
                }
            }
        };

        void buildPreview();
    }, [croppedAreaPixels, selectedFile?.type, sourceImageUrl]);

    const handleFileSelect = (event) => {
        const file = event.target.files?.[0] || null;
        setUploadError('');
        setUploadSuccess(false);
        setPreviewError('');

        if (!file) {
            setSelectedFile(null);
            setFileError('');
            return;
        }

        const validationResult = validateAvatarFile(file);
        if (!validationResult.isValid) {
            setFileError(validationResult.message);
            setSelectedFile(null);
            return;
        }

        const nextSourceImageUrl = createObjectUrl(file);
        setSourceImageUrl((previousUrl) => {
            revokeObjectUrl(previousUrl);
            return nextSourceImageUrl;
        });
        setCroppedPreviewUrl((previousUrl) => {
            revokeObjectUrl(previousUrl);
            return '';
        });

        setFileError('');
        setSelectedFile(file);
        setCrop(INITIAL_CROP);
        setZoom(INITIAL_ZOOM);
        setCroppedAreaPixels(null);
        setCroppedBlob(null);
    };

    const canSubmit = useMemo(() => {
        return Boolean(selectedFile && croppedBlob && !isUploading && !isBuildingPreview);
    }, [croppedBlob, isBuildingPreview, isUploading, selectedFile]);

    const uploadAvatar = async () => {
        if (isUploading) {
            return;
        }

        if (!selectedFile) {
            setUploadError('Please choose an image first.');
            return;
        }

        if (!croppedBlob) {
            setUploadError('Preview is not ready yet. Please adjust the crop and try again.');
            return;
        }

        setIsUploading(true);
        setUploadError('');
        setUploadSuccess(false);

        const fileExtension = (selectedFile.name?.split('.').pop() || 'jpg').toLowerCase();
        const uploadFileName = `avatar-crop.${fileExtension}`;
        const formData = new FormData();
        formData.append('file', croppedBlob, uploadFileName);

        try {
            const result = await apiService.postFormData(API_CONFIG.ENDPOINTS.PROFILE_IMAGE_UPLOAD, formData);

            if (!isFlowActiveRef.current) {
                return;
            }

            if (apiService.isErrorResponse(result)) {
                setUploadError(result.backendMessage || result.message || 'Could not upload your avatar. Please try again.');
                return;
            }

            const responseData = getResponseData(result);
            const nextAvatarUrl = responseData?.avatarUrl || responseData?.avatar_url || '';

            if (!nextAvatarUrl) {
                setUploadError('Upload succeeded, but preview URL was not returned. Please retry.');
                return;
            }

            setUploadSuccess(true);
            onUploadSuccess(nextAvatarUrl);
            closeModal();
        } catch {
            if (!isFlowActiveRef.current) {
                return;
            }
            setUploadError('Could not upload your avatar. Please try again.');
        } finally {
            if (isFlowActiveRef.current) {
                setIsUploading(false);
            }
        }
    };

    if (!isOpen) {
        return null;
    }

    return (
        <div className="protected-gate__overlay" onClick={closeModal}>
            <div
                className="protected-gate__modal w-[min(760px,100%)]"
                onClick={(event) => event.stopPropagation()}
                role="dialog"
                aria-modal="true"
                aria-labelledby="avatar-upload-title"
            >
                <div className="relative mb-4">
                    <h2 id="avatar-upload-title" className="protected-gate__title">Update avatar</h2>
                    <button
                        type="button"
                        aria-label="Close modal"
                        onClick={closeModal}
                        className="absolute right-3 top-3 h-8 w-8 flex items-center justify-center rounded-full bg-red-50 text-red-500 hover:bg-red-100 hover:text-red-600 active:bg-red-200 transition-colors duration-200 shadow-sm hover:shadow-md"
                    >
                        <X className="h-4 w-4"/>
                    </button>
                </div>

                <p className="protected-gate__description">Choose an image, adjust the crop, and confirm when the
                    preview looks right.</p>

                <div
                    className="mt-4 rounded-2xl border border-slate-200/80 bg-white/80 p-4 dark:border-slate-700 dark:bg-slate-900/70">
                    <label className="auth-label">Select image</label>
                    <input
                        ref={inputRef}
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        onChange={handleFileSelect}
                        className="mt-2 block w-full text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-teal-600/90 file:px-3 file:py-2 file:font-semibold file:text-white hover:file:bg-teal-600"
                    />
                    {fileError ? <p className="mt-2 text-sm text-rose-600 dark:text-rose-300">{fileError}</p> : null}
                </div>

                {sourceImageUrl ? (
                    <div className="mt-4 grid gap-4 lg:grid-cols-2">
                        <div
                            className="rounded-2xl border border-slate-200/80 bg-white/80 p-4 dark:border-slate-700 dark:bg-slate-900/70">
                            <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">Crop</p>
                            <div className="relative mt-3 h-64 overflow-hidden rounded-xl bg-slate-900/90">
                                <Cropper
                                    image={sourceImageUrl}
                                    crop={crop}
                                    zoom={zoom}
                                    aspect={1}
                                    cropShape="round"
                                    showGrid={false}
                                    onCropChange={setCrop}
                                    onCropComplete={onCropComplete}
                                    onZoomChange={setZoom}
                                />
                            </div>
                            <label
                                className="mt-4 block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                                Zoom
                            </label>
                            <input
                                type="range"
                                min={1}
                                max={3}
                                step={0.01}
                                value={zoom}
                                onChange={(event) => setZoom(Number(event.target.value))}
                                className="mt-2 w-full"
                            />
                        </div>

                        <div
                            className="rounded-2xl border border-slate-200/80 bg-white/80 p-4 dark:border-slate-700 dark:bg-slate-900/70">
                            <p className="text-sm font-semibold text-slate-700 dark:text-slate-200">Preview</p>
                            <div className="mt-3 flex items-center gap-4">
                                <div
                                    className="h-20 w-20 overflow-hidden rounded-full border border-slate-300 bg-slate-100 dark:border-slate-600 dark:bg-slate-800">
                                    {croppedPreviewUrl ? (
                                        <img src={croppedPreviewUrl} alt="Cropped avatar preview"
                                             className="h-full w-full object-cover"/>
                                    ) : (
                                        <div
                                            className="flex h-full w-full items-center justify-center text-xs text-slate-500">No
                                            preview</div>
                                    )}
                                </div>
                                <div
                                    className="h-20 w-20 overflow-hidden rounded-full border border-slate-300 bg-slate-100 dark:border-slate-600 dark:bg-slate-800">
                                    {currentAvatarSrc ? (
                                        <img src={currentAvatarSrc} alt="Current avatar"
                                             className="h-full w-full object-cover"/>
                                    ) : (
                                        <div
                                            className="flex h-full w-full items-center justify-center text-xs text-slate-500">Current</div>
                                    )}
                                </div>
                            </div>
                            <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">Left: new cropped avatar.
                                Right: current avatar.</p>
                            {isBuildingPreview ? <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Building
                                preview...</p> : null}
                            {previewError ?
                                <p className="mt-2 text-sm text-rose-600 dark:text-rose-300">{previewError}</p> : null}
                        </div>
                    </div>
                ) : null}

                {uploadError ? <p className="mt-4 text-sm text-rose-600 dark:text-rose-300">{uploadError}</p> : null}
                {uploadSuccess ? <p className="mt-4 text-sm text-emerald-600 dark:text-emerald-300">Avatar updated
                    successfully.</p> : null}

                <div className="mt-6 flex items-center justify-end gap-2">
                    <button
                        type="button"
                        onClick={closeModal}
                        className="h-11 rounded-2xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-teal-400/25 dark:border-slate-700 dark:bg-slate-900/70 dark:text-slate-200 dark:hover:bg-slate-800"
                    >
                        Cancel
                    </button>
                    <button
                        type="button"
                        onClick={uploadAvatar}
                        disabled={!canSubmit}
                        className="h-11 rounded-2xl border border-teal-400/60 bg-[linear-gradient(135deg,#14B8A6,#0EA5E9)] px-4 text-sm font-semibold text-white shadow-[0_16px_30px_rgba(20,184,166,0.24)] transition-all hover:-translate-y-0.5 hover:shadow-[0_18px_34px_rgba(20,184,166,0.32)] focus:outline-none focus:ring-2 focus:ring-teal-400/50 disabled:cursor-not-allowed disabled:opacity-60 dark:border-teal-400/30"
                    >
                        {isUploading ? 'Uploading...' : 'Upload avatar'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default AvatarUploadModal;



