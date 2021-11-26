package protocols.optimization.resest.timers;

import babel.generic.ProtoTimer;

public class StartupTimer extends ProtoTimer {
    public static final short TIMER_CODE = 23202;

    public StartupTimer() {
        super(TIMER_CODE);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
