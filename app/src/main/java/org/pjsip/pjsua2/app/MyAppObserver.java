package org.pjsip.pjsua2.app;

import org.pjsip.pjsua2.pjsip_status_code;

/* Interface to separate UI & engine a bit better */
public interface MyAppObserver {
    abstract void notifyRegState(pjsip_status_code code, String reason,
                                 int expiration);

    abstract void notifyIncomingCall(MyCall call);

    abstract void notifyCallState(MyCall call);

    abstract void notifyCallMediaState(MyCall call);

    abstract void notifyBuddyState(MyBuddy buddy);
}
