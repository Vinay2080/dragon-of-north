import {motion} from 'framer-motion';

const Footer = () => {
    return (
        <motion.footer
            initial={{opacity: 0, y: 16}}
            whileInView={{opacity: 1, y: 0}}
            viewport={{once: true, amount: 0.15}}
            transition={{duration: 0.5, ease: 'easeOut'}}
            className="mt-14 border-t border-cyan-300/15 bg-slate-950/90"
        >
            <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 px-6 py-8 sm:flex-row sm:items-start sm:justify-between sm:px-10 lg:px-16">
                <div className="max-w-md">
                    <h4 className="text-lg font-semibold text-slate-100">Dragon of North</h4>
                    <p className="mt-2 text-sm leading-relaxed text-slate-400">
                        Session-aware authentication infrastructure to monitor devices, reduce account risk, and enforce zero-trust access.
                    </p>
                </div>

                <div className="space-y-2 text-sm">
                    <a
                        href="https://github.com"
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex text-cyan-200 transition hover:text-cyan-100"
                    >
                        GitHub
                    </a>
                    <p className="text-slate-400">Built by Dragon of North Team</p>
                </div>
            </div>
        </motion.footer>
    );
};

export default Footer;
