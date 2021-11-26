package protocols.optimization.resest.timers;

import babel.generic.ProtoTimer;

public class HeartbeatTimer extends ProtoTimer {
    public static final short TIMER_CODE = 23204;

    public HeartbeatTimer() {
        super(TIMER_CODE);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
