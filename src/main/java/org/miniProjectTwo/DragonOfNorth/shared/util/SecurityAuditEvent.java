package org.miniProjectTwo.DragonOfNorth.shared.util;

/**
 * Canonical security/audit taxonomy for auth/session/MFA flows.
 */
public final class SecurityAuditEvent {

    private SecurityAuditEvent() {
    }

    public static final String AUTH_LOGIN_SUCCESS = "auth.login.success";
    public static final String AUTH_LOGIN_FAILED = "auth.login.failed";
    public static final String AUTH_LOGOUT_SUCCESS = "auth.logout.success";
    public static final String AUTH_LOGOUT_FAILED = "auth.logout.failed";
    public static final String AUTH_REFRESH_SUCCESS = "auth.refresh.success";
    public static final String AUTH_REFRESH_FAILED = "auth.refresh.failed";
    public static final String AUTH_REFRESH_REPLAY_DETECTED = "auth.refresh.replay_detected";

    public static final String AUTH_MFA_CHALLENGE_ISSUED = "auth.mfa.challenge.issued";
    public static final String AUTH_MFA_CHALLENGE_FAILED = "auth.mfa.challenge.failed";
    public static final String AUTH_MFA_CHALLENGE_VERIFIED = "auth.mfa.challenge.verified";
    public static final String AUTH_MFA_CHALLENGE_INVALIDATED = "auth.mfa.challenge.invalidated";
    public static final String AUTH_MFA_CHALLENGE_REPLAY_DETECTED = "auth.mfa.challenge.replay_detected";
    public static final String AUTH_MFA_SUSPICIOUS_CONTEXT_MISMATCH = "auth.mfa.suspicious.context_mismatch";
    public static final String AUTH_SESSION_BINDING_FAILURE = "auth.session.binding_failure";

    public static final String AUTH_MFA_STEPUP_CHALLENGE_ISSUED = "auth.mfa.stepup.challenge.issued";
    public static final String AUTH_MFA_STEPUP_COMPLETED = "auth.mfa.stepup.completed";
    public static final String AUTH_MFA_STEPUP_REQUIRED = "auth.mfa.stepup.required";

    public static final String AUTH_SESSION_CREATED = "auth.session.created";
    public static final String AUTH_SESSION_REVOKED = "auth.session.revoked";
    public static final String AUTH_SESSION_ROTATE_FAILED = "auth.session.rotate.failed";
    public static final String AUTH_SESSION_SUSPICIOUS = "auth.session.suspicious";
    public static final String AUTH_ABUSE_RATE_LIMITED = "auth.abuse.rate_limited";
    public static final String AUTH_ABUSE_CHALLENGE_FLOOD = "auth.abuse.challenge_flood";
    public static final String AUTH_ABUSE_MFA_BRUTEFORCE = "auth.abuse.mfa_bruteforce";

}
