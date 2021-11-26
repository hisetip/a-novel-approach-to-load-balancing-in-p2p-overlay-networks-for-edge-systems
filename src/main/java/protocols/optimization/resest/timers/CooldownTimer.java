package protocols.optimization.resest.timers;

import babel.generic.ProtoTimer;

public class CooldownTimer extends ProtoTimer {
    public static final short TIMER_CODE = 23203;

    public CooldownTimer() {
        super(TIMER_CODE);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
