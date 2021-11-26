package protocols.app.timers;

import babel.generic.ProtoTimer;

public class StartTimer extends ProtoTimer {
    public static final short TIMER_ID = 31202;

    public StartTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
