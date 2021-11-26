package protocols.app.timers;

import babel.generic.ProtoTimer;

public class StopTimer extends ProtoTimer {
    public static final short TIMER_ID = 31203;

    public StopTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
