package protocols.dissemination.flood.timers;

import babel.generic.ProtoTimer;

public class PrintNeighDeltaTimer extends ProtoTimer {
    public static final short TIMER_CODE = 21201;

    public PrintNeighDeltaTimer() {
        super(TIMER_CODE);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
