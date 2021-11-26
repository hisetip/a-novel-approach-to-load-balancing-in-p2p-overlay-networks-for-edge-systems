package protocols.app.timers;

import babel.generic.ProtoTimer;

public class BroadcastTimer extends ProtoTimer {
    public static final short TIMER_ID = 31201;

    public BroadcastTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
