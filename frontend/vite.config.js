import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// https://vite.dev/config/
export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            'framer-motion': path.resolve(__dirname, 'src/shims/framer-motion.tsx'),
            'lucide-react': path.resolve(__dirname, 'src/shims/lucide-react.tsx'),
            '@xyflow/react': path.resolve(__dirname, 'src/shims/xyflow-react.jsx'),
        },
    },
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
    },
});
