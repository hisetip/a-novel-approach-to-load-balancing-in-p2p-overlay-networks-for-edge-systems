package protocols.dissemination.flood;

import babel.exceptions.HandlerRegistrationException;
import babel.generic.GenericProtocol;
import babel.generic.ProtoMessage;
import babel.generic.ProtoNotification;
import babel.generic.ProtoTimer;
import protocols.dissemination.flood.timers.PrintNeighDeltaTimer;
import protocols.dissemination.notifications.DeliverMessageNotification;
import protocols.membership.notifications.NeighborDown;
import protocols.membership.notifications.NeighborUp;
import protocols.dissemination.flood.messages.GossipMessage;
import protocols.dissemination.flood.utils.HashProducer;
import protocols.dissemination.requests.BroadcastRequest;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.optimization.resest.notifications.StartNeighVariabilityPrintsNotification;

import java.io.IOException;
import java.util.*;

public class Flood extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(Flood.class);

    public static final String PROTO_NAME = "Flood";
    public static final short PROTO_ID = 21000;


    private final Host myself;
    private final Set<Host> neighbours;
    private final Set<Integer> received;

    private final HashProducer hashProducer;

    private List<Integer> numberOfNeighsOverTime;

    public Flood(String channelName, Properties properties, Host myself) throws IOException, HandlerRegistrationException {
        super(PROTO_NAME, PROTO_ID);

        this.hashProducer = new HashProducer(myself);

        this.myself = myself;
        neighbours = new HashSet<>();
        received = new HashSet<>();

        this.numberOfNeighsOverTime = new ArrayList<>();

        int channelId = createChannel(channelName, properties);


        /*--------------------- Register Request Handlers ----------------------------- */
        registerRequestHandler(BroadcastRequest.REQUEST_ID, this::uponBroadcast);

        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(GossipMessage.MSG_ID, GossipMessage.serializer);
        /*---------------------- Register Message Handlers -------------------------- */
        registerMessageHandler(channelId, GossipMessage.MSG_ID, this::uponReceiveGossip, this::uponFailSendingGossipMessage);

        /*--------------------- Register Timer Handlers ----------------------------- */
        registerTimerHandler(PrintNeighDeltaTimer.TIMER_CODE, this::uponPrintNeighDeltaTimer);

        /*--------------------- Register Notification Handlers ----------------------------- */
        subscribeNotification(NeighborUp.NOTIFICATION_ID, this::uponNeighbourUp);
        subscribeNotification(NeighborDown.NOTIFICATION_ID, this::uponNeighbourDown);
        subscribeNotification(StartNeighVariabilityPrintsNotification.NOTIFICATION_ID, this::uponStartPrints);
    }


    /*--------------------------------- Messages ---------------------------------------- */
    private void uponReceiveGossip(GossipMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received from " + from.toString());
        if(received.add(msg.getMid())) {
            triggerNotification(new DeliverMessageNotification(msg.getContent()));
            neighbours.forEach(host ->
            {
                logger.debug("For {}", host);
                if(!host.equals(from)) {
                    sendMessage(msg, host);
                    logger.debug("Sent msg to {}", host);
                }
            });
        }
    }


    /*--------------------------------- Requests ---------------------------------------- */
    private void uponBroadcast(BroadcastRequest request, short sourceProto) {
        int mid = hashProducer.hash(request.getMsg());
        GossipMessage msg = new GossipMessage(mid, 0, sourceProto, request.getMsg());
        uponReceiveGossip(msg, myself, PROTO_ID, -1);

    }

    /*--------------------------------- Notifications ---------------------------------------- */
    private void uponNeighbourUp(NeighborUp notification, short sourceProto) {
        neighbours.add(notification.getNeighbour());
        logger.debug("----------");
        logger.debug("NU Neighbors:");
        for (Host neighbour : neighbours) {
            logger.debug("{}", neighbour.toString());
        }
        logger.debug("----------");
    }

    private void uponNeighbourDown(NeighborDown notification, short sourceProto) {
        neighbours.remove(notification.getNeighbour());
        closeConnection(notification.getNeighbour());
        logger.debug("----------");
        logger.debug("ND Neighbors:");
        for (Host neighbour : neighbours) {
            logger.debug("{}", neighbour.toString());
        }
        logger.debug("----------");
    }

    private void uponStartPrints(StartNeighVariabilityPrintsNotification notification, short sourceProto) {
        setupPeriodicTimer(new PrintNeighDeltaTimer(), 60000, 60000);
        numberOfNeighsOverTime.add(neighbours.size());
        logger.info("----------");
        logger.info("uponStartPrints Neighbors:");
        for (Host neighbour : neighbours) {
            logger.info("{}", neighbour.toString());
        }
        logger.info("----------");
    }

    /*--------------------------------- Timers ---------------------------------------- */
    private void uponPrintNeighDeltaTimer(PrintNeighDeltaTimer timer, long timerId) {
        numberOfNeighsOverTime.add(neighbours.size());
        int delta = numberOfNeighsOverTime.get(numberOfNeighsOverTime.size() - 1) - numberOfNeighsOverTime.get(numberOfNeighsOverTime.size() - 2);

        if (delta > 0) {
            logger.info("Number of added neighbors: {}", delta);
            logger.info("Number of removed neighbors: {}", 0);
        } else if (delta < 0) {
            logger.info("Number of added neighbors: {}", 0);
            logger.info("Number of removed neighbors: {}", delta);
        } else {
            logger.info("Number of added neighbors: {}", 0);
            logger.info("Number of removed neighbors: {}", 0);
        }
        logger.info("----------");
        logger.info("uponPrintNeighDeltaTimer Neighbors:");
        for (Host neighbour : neighbours) {
            logger.info("{}", neighbour.toString());
        }
        logger.info("----------");
    }

    private void uponFailSendingGossipMessage(GossipMessage gossipMessage, Host host, short i, Throwable throwable, int i1) {
        //logger.error("Message {} to {} failed, reason: {}", gossipMessage, host, throwable);
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        logger.info("Starting flood...");
    }
}
