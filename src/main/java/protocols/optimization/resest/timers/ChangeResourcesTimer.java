package protocols.optimization.resest.timers;

import babel.generic.ProtoTimer;

public class ChangeResourcesTimer extends ProtoTimer {
    public static final short TIMER_CODE = 23205;

    public ChangeResourcesTimer() {
        super(TIMER_CODE);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
