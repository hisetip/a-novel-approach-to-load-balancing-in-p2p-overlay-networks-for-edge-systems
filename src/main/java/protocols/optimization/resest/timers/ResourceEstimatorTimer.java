package protocols.optimization.resest.timers;

import babel.generic.ProtoTimer;

public class ResourceEstimatorTimer extends ProtoTimer {
    public static final short TIMER_CODE = 23201;

    public ResourceEstimatorTimer() {
        super(TIMER_CODE);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
