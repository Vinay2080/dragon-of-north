import React from 'react';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = {hasError: false};
    }

    static getDerivedStateFromError() {
        return {hasError: true};
    }

    componentDidCatch(error, errorInfo) {
        // eslint-disable-next-line no-console
        console.error('UI crash captured by ErrorBoundary', error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="min-h-screen flex items-center justify-center bg-slate-950 text-white p-8">
                    <div className="max-w-md text-center rounded-xl border border-slate-700 bg-slate-900 p-6">
                        <h2 className="text-xl font-semibold">Something went wrong</h2>
                        <p className="mt-2 text-sm text-slate-300">An unexpected error occurred. Please refresh the page.</p>
                        <button
                            className="mt-4 rounded bg-blue-600 px-4 py-2 text-sm"
                            onClick={() => window.location.reload()}
                        >
                            Reload page
                        </button>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
