package protocols.membership.hyparviewresest.timers;


import babel.generic.ProtoTimer;

public class ShuffleTimer extends ProtoTimer {
    public static final short TimerCode = 12202;

    public ShuffleTimer() {
        super(ShuffleTimer.TimerCode);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
