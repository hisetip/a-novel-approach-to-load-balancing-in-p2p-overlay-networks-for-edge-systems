package protocols.other.timer;

import babel.generic.ProtoTimer;

public class TimerTimer extends ProtoTimer {

    public static final short TIMER_ID=101;

    public TimerTimer(){
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
