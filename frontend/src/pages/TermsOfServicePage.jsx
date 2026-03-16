import DocsLayout from '../components/DocsLayout';

const TermsOfServicePage = () => (
    <DocsLayout
        title="Terms of Service"
        subtitle="Effective date: March 16, 2026. These Terms of Service govern your use of DragonOfNorth and form a binding agreement between you and DragonOfNorth."
    >
        <section className="space-y-6 rounded-2xl border border-white/10 bg-white/[0.03] p-6 text-sm leading-7 text-slate-200">
            <div>
                <h3 className="text-xl font-semibold text-white">1. Acceptance of Terms</h3>
                <p className="mt-2">By accessing or using DragonOfNorth at https://dragonofnorth.dev (the "Service"), you agree to these Terms of Service ("Terms"). If you do not agree, do not use the Service.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">2. Eligibility and Account Registration</h3>
                <p className="mt-2">You must provide accurate account information and keep your credentials secure. You are responsible for activities that occur under your account. You may register using email/password or Google OAuth, subject to any applicable third-party terms.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">3. Service Description</h3>
                <p className="mt-2">DragonOfNorth provides a web-based authentication platform and related account services. We may modify, improve, or discontinue features at any time, including as needed for security, legal compliance, or platform reliability.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">4. Acceptable Use</h3>
                <p className="mt-2">You agree not to:</p>
                <ul className="mt-2 list-disc space-y-1 pl-6">
                    <li>Use the Service for unlawful, fraudulent, or abusive activities.</li>
                    <li>Attempt to gain unauthorized access to systems, accounts, or data.</li>
                    <li>Interfere with the security, integrity, or availability of the Service.</li>
                    <li>Bypass rate limits, security controls, or authentication safeguards.</li>
                    <li>Upload, transmit, or distribute malicious code, bots, or harmful automation.</li>
                    <li>Use the Service in a manner that violates applicable laws or third-party rights.</li>
                </ul>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">5. Security and Authentication</h3>
                <p className="mt-2">We use industry-standard security practices, including token-based authentication, verification mechanisms, logging, and abuse prevention controls. You must maintain the confidentiality of your login credentials and notify us promptly of any suspected unauthorized account activity.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">6. Third-Party Services</h3>
                <p className="mt-2">Certain functionality relies on third-party services, including Google OAuth for identity verification and cloud infrastructure providers for hosting. DragonOfNorth is not responsible for third-party services and their independent policies, outages, or actions.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">7. Fees and Commercial Terms</h3>
                <p className="mt-2">Unless explicitly stated otherwise, access to the current Service features is provided on a general-use basis. If paid features are introduced, additional terms and pricing disclosures will apply.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">8. Intellectual Property</h3>
                <p className="mt-2">The Service, including software, branding, documentation, and content provided by DragonOfNorth, is protected by applicable intellectual property laws. Subject to these Terms, we grant you a limited, non-exclusive, revocable right to use the Service for its intended purpose.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">9. Service Availability and Changes</h3>
                <p className="mt-2">We aim to provide reliable access but do not guarantee uninterrupted or error-free operation. The Service may be unavailable from time to time due to maintenance, updates, incidents, third-party outages, or events beyond our control.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">10. Suspension and Termination</h3>
                <p className="mt-2">We may suspend or terminate access to the Service, with or without notice, if we reasonably believe you violated these Terms, created security risk, or used the Service unlawfully. You may stop using the Service at any time and request account deletion by contacting admin@dragonofnorth.dev.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">11. Privacy</h3>
                <p className="mt-2">Our collection and use of personal information are described in our Privacy Policy at /privacy, which is incorporated into these Terms by reference.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">12. Disclaimers</h3>
                <p className="mt-2">To the maximum extent permitted by law, the Service is provided "as is" and "as available" without warranties of any kind, express or implied, including implied warranties of merchantability, fitness for a particular purpose, and non-infringement.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">13. Limitation of Liability</h3>
                <p className="mt-2">To the maximum extent permitted by law, DragonOfNorth and its operators, affiliates, and service providers will not be liable for indirect, incidental, special, consequential, exemplary, or punitive damages, or for loss of data, profits, goodwill, or business interruption arising from or related to your use of the Service. Our total liability for claims relating to the Service will not exceed the greater of USD $100 or the amount you paid us in the 12 months preceding the claim.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">14. Indemnification</h3>
                <p className="mt-2">You agree to indemnify and hold harmless DragonOfNorth from and against claims, liabilities, damages, losses, and expenses arising out of your misuse of the Service, violation of these Terms, or infringement of applicable law or third-party rights.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">15. Governing Law and Dispute Resolution</h3>
                <p className="mt-2">These Terms are governed by applicable laws of the jurisdiction in which DragonOfNorth operates, without regard to conflict-of-law principles. You agree to attempt good-faith resolution by contacting admin@dragonofnorth.dev before initiating formal proceedings.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">16. Changes to These Terms</h3>
                <p className="mt-2">We may update these Terms from time to time. Updated Terms will be posted at /terms with a revised effective date. Continued use of the Service after updates constitutes acceptance of the updated Terms.</p>
            </div>

            <div>
                <h3 className="text-xl font-semibold text-white">17. Contact Information</h3>
                <p className="mt-2">For legal notices, support requests, or questions about these Terms, contact admin@dragonofnorth.dev.</p>
            </div>
        </section>
    </DocsLayout>
);

export default TermsOfServicePage;
