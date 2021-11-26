package protocols.membership.hyparviewintegrated.timers;


import babel.generic.ProtoTimer;

public class HelloTimeout extends ProtoTimer {
    public static final short TimerCode = 16201;

    public HelloTimeout() {
        super(HelloTimeout.TimerCode);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}
