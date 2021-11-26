package protocols.app;

import babel.generic.GenericProtocol;
import protocols.app.timers.BroadcastTimer;
import protocols.app.timers.StartTimer;
import protocols.app.timers.StopTimer;
import protocols.dissemination.flood.Flood;
import protocols.dissemination.notifications.DeliverMessageNotification;
import babel.exceptions.HandlerRegistrationException;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.dissemination.requests.BroadcastRequest;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class Application extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(Application.class);


    public static final String PROTO_NAME = "Application";
    public static final short PROTO_ID = 31000;

    private final int peerID;
    private int seqNum = 0;
    private final Host self;

    private final int payloadSize;
    private final int serviceInterval;
    private final Random rnd;
    private long broadcastTimerID;
    private final int runningTime;

    private boolean broadcastFlag;

    public Application(Host self, Properties properties) throws HandlerRegistrationException {
        super(PROTO_NAME, PROTO_ID);
        this.self = self;

        this.payloadSize = Integer.parseInt(properties.getProperty("payloadSize"));
        this.serviceInterval = Math.round((1/Float.parseFloat(properties.getProperty("messagesPerSecond")))*1000);
        this.runningTime = Integer.parseInt(properties.getProperty("runningTime"));
        this.peerID = Integer.parseInt(properties.getProperty("peerID"));
        this.rnd = new Random();
        this.broadcastTimerID = -1L;
        this.broadcastFlag = false;

        subscribeNotification(DeliverMessageNotification.NOTIFICATION_ID, this::uponDeliver);

        registerTimerHandler(StartTimer.TIMER_ID, this::uponStart);
        registerTimerHandler(BroadcastTimer.TIMER_ID, this::uponSendMsg);
        registerTimerHandler(StopTimer.TIMER_ID, this::uponStop);
    }

    private void uponDeliver(DeliverMessageNotification notif, short sourceProto) {
        byte[] msg;
        if(payloadSize > 0) {
            msg = new byte[notif.getContent().length - payloadSize];
            System.arraycopy(notif.getContent(), payloadSize-1, msg, 0, notif.getContent().length - payloadSize);
        } else
            msg = notif.getContent();

        logger.info("Received the message: '{}'", new String(msg));
    }

    private void uponStart(StartTimer timer, long timerId) {
        logger.info("Starting sending messages every {} ms...", this.serviceInterval);
        this.broadcastTimerID = setupPeriodicTimer(new BroadcastTimer(), this.serviceInterval, this.serviceInterval);
        setupTimer(new StopTimer(), this.runningTime* 1000L);
        broadcastFlag = true;
    }

    private void uponSendMsg(BroadcastTimer timer, long timerId) {
        if (!broadcastFlag) {
            return;
        }
        String tosend = String.format("%d_%d_", peerID, seqNum++);
        logger.info("flooding '{}'", tosend);

        byte[] toSend;
        byte[] payload = new byte[payloadSize];
        rnd.nextBytes(payload);
        toSend = new byte[(payloadSize + tosend.getBytes().length)];
        System.arraycopy(payload, 0, toSend, 0, payloadSize);
        System.arraycopy(tosend.getBytes(), 0, toSend, payloadSize - 1, tosend.getBytes().length);

        this.broadcastTimerID = timerId;
        sendRequest(new BroadcastRequest(toSend), Flood.PROTO_ID);
    }

    private void uponStop(StopTimer timer, long timerId) {
        logger.info("Stopping broadcasts. Broadcast timer ID is {}.", this.broadcastTimerID);
        broadcastFlag = false;
        if (cancelTimer(this.broadcastTimerID) == null) {
            logger.warn("Could not cancel timer. However, we got the flag to save us, no worries.");
        }
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        long realStartUpTime = (Long.parseLong(props.getProperty("startT"))*1000 + Long.parseLong(props.getProperty("startTime"))*1000) - System.currentTimeMillis();
        Random r = new Random();
        int low = 1;
        int high = 4500;
        int result = r.nextInt(high-low) + low;
        setupTimer(new StartTimer(), realStartUpTime + result);
        logger.info("Using payload-size {} and service-interval {}", payloadSize, serviceInterval);
        logger.debug("startT={} startTime={} systemCT={}", Long.parseLong(props.getProperty("startT"))*1000, Long.parseLong(props.getProperty("startTime"))*1000, System.currentTimeMillis());
    }
}
