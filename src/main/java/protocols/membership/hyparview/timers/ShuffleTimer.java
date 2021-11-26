package protocols.membership.hyparview.timers;


import babel.generic.ProtoTimer;

public class ShuffleTimer extends ProtoTimer {
    public static final short TimerCode = 16202;

    public ShuffleTimer() {
        super(ShuffleTimer.TimerCode);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
