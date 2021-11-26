package protocols.other.timer;

import babel.generic.GenericProtocol;
import babel.exceptions.HandlerRegistrationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class TimerProto extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(TimerProto.class);

    public TimerProto() {
        super("TimerTest", (short) 100);
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        registerTimerHandler(TimerTimer.TIMER_ID, this::handleTimerTimer);
        setupPeriodicTimer(new TimerTimer(), 1000, 300);
    }

    private void handleTimerTimer(TimerTimer timer, long timerId) {
        logger.info("timer ran");
    }

}
