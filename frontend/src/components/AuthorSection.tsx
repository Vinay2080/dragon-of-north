import { FileText, Github } from 'lucide-react';
import { Reveal, RevealList, RevealItem } from './Reveal';

const AuthorSection = () => {
    return (
        <section className="mx-auto mt-14 w-full max-w-7xl px-2 sm:px-0">
            <Reveal className="rounded-3xl border border-cyan-300/20 bg-white/[0.04] p-6 backdrop-blur-xl sm:p-8">
                <h3 className="text-2xl font-semibold text-white sm:text-3xl">About the Developer</h3>

                <div className="mt-4 space-y-2">
                    <p className="text-lg font-medium text-cyan-100">Vinay Patil</p>
                    <p className="max-w-3xl text-sm leading-relaxed text-slate-300 sm:text-base">
                        Backend-focused developer building session-aware authentication systems using Java and Spring.
                    </p>
                </div>

                <RevealList className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2">
                    <RevealItem
                        as="a"
                        href="https://github.com/Vinay2080/dragon-of-north"
                        target="_blank"
                        rel="noreferrer"
                        className="group rounded-2xl border border-cyan-300/25 bg-slate-900/40 p-4 transition hover:border-cyan-200/55 hover:bg-slate-900/55"
                    >
                        <div className="flex items-center gap-3 text-cyan-100">
                            <Github size={18} />
                            <span className="text-sm font-semibold">GitHub</span>
                        </div>
                        <p className="mt-2 text-xs text-slate-400 group-hover:text-slate-300">View Dragon of North repository</p>
                    </RevealItem>

                    <RevealItem
                        as="a"
                        href="https://drive.google.com/drive/folders/1_BbNhqGcIX6vSbhclkxCZ-m42Wh2Ayfl"
                        target="_blank"
                        rel="noreferrer"
                        className="group rounded-2xl border border-cyan-300/25 bg-slate-900/40 p-4 transition hover:border-cyan-200/55 hover:bg-slate-900/55"
                    >
                        <div className="flex items-center gap-3 text-cyan-100">
                            <FileText size={18} />
                            <span className="text-sm font-semibold">Resume</span>
                        </div>
                        <p className="mt-2 text-xs text-slate-400 group-hover:text-slate-300">Open resume folder</p>
                    </RevealItem>
                </RevealList>
            </Reveal>
        </section>
    );
};

export default AuthorSection;
