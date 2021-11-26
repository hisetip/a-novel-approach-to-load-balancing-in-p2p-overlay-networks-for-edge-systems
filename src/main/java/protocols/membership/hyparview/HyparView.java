package protocols.membership.hyparview;

import babel.exceptions.HandlerRegistrationException;
import babel.generic.GenericProtocol;
import channel.tcp.TCPChannel;
import channel.tcp.events.*;
import com.sun.tools.javac.util.Pair;
import network.data.Host;
import protocols.membership.notifications.ChannelCreated;
import protocols.membership.notifications.NeighborDown;
import protocols.membership.notifications.NeighborUp;
import protocols.membership.hyparview.messages.*;
import protocols.membership.hyparview.timers.HelloTimeout;
import protocols.membership.hyparview.timers.ShuffleTimer;
import protocols.membership.hyparview.utils.IView;
import protocols.membership.hyparview.utils.View;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.optimization.foutakos.notifications.RequestedNumberOfNeighborsNotification;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;


public class HyparView extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(HyparView.class);

    public final static short PROTOCOL_ID = 16000;
    public final static String PROTOCOL_NAME = "HyParView";
    private static final int MAX_BACKOFF = 60000;

    private final short ARWL; //param: active random walk length
    private final short PRWL; //param: passive random walk length

    private final short shuffleTime; //param: timeout for shuffle
    private final short originalTimeout; //param: timeout for hello msgs
    private short timeout;

    private final short kActive; //param: number of active nodes to exchange on shuffle
    private final short kPassive; //param: number of passive nodes to exchange on shuffle

    private final String hpvRemovalMode;

    protected int channelId;
    protected final Host myself;

    protected IView active;
    protected IView passive;

    protected Set<Host> pending;
    private final Map<Short, Host[]> activeShuffles;

    private short seqNum = 0;

    protected final Random rnd;

    private int myResources;

    public HyparView(String channelName, Properties properties, Host myself) throws IOException, HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);
        this.myself = myself;

        int maxActive = Integer.parseInt(properties.getProperty("ActiveView", "7")); //param: maximum active nodes (degree of random overlay)
        int maxPassive = Integer.parseInt(properties.getProperty("PassiveView", "30")); //param: maximum passive nodes
        this.ARWL = Short.parseShort(properties.getProperty("ARWL", "3")); //param: active random walk length
        this.PRWL = Short.parseShort(properties.getProperty("PRWL", "6")); //param: passive random walk length

        this.shuffleTime = Short.parseShort(properties.getProperty("shuffleTime", "8000")); //param: timeout for shuffle
        this.timeout = this.originalTimeout = Short.parseShort(properties.getProperty("helloBackoff", "7000")); //param: timeout for hello msgs

        this.kActive = Short.parseShort(properties.getProperty("kActive", "2")); //param: number of active nodes to exchange on shuffle
        this.kPassive = Short.parseShort(properties.getProperty("kPassive", "3")); //param: number of passive nodes to exchange on shuffle

        this.rnd = new Random();
        this.active = new View(maxActive, myself, rnd);
        this.passive = new View(maxPassive, myself, rnd);

        this.pending = new HashSet<>();
        this.activeShuffles = new TreeMap<>();

        this.active.setOther(passive, pending);
        this.passive.setOther(active, pending);

        this.myResources = -1;
        this.hpvRemovalMode = properties.getProperty("hpvRemovalMode");

        channelId = createChannel(channelName, properties);

        /*-------------------- Subscribe to notifications ------------------------------- */
        subscribeNotification(RequestedNumberOfNeighborsNotification.NOTIFICATION_ID, this::uponRequestNumberOfNeighborsNotification);

        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(JoinMessage.MSG_CODE, JoinMessage.serializer);
        registerMessageSerializer(JoinReplyMessage.MSG_CODE, JoinReplyMessage.serializer);
        registerMessageSerializer(ForwardJoinMessage.MSG_CODE, ForwardJoinMessage.serializer);
        registerMessageSerializer(HelloMessage.MSG_CODE, HelloMessage.serializer);
        registerMessageSerializer(HelloReplyMessage.MSG_CODE, HelloReplyMessage.serializer);
        registerMessageSerializer(DisconnectMessage.MSG_CODE, DisconnectMessage.serializer);
        registerMessageSerializer(ShuffleMessage.MSG_CODE, ShuffleMessage.serializer);
        registerMessageSerializer(ShuffleReplyMessage.MSG_CODE, ShuffleReplyMessage.serializer);

        /*---------------------- Register Message Handlers -------------------------- */
        registerMessageHandler(channelId, JoinMessage.MSG_CODE, this::uponReceiveJoin);
        registerMessageHandler(channelId, JoinReplyMessage.MSG_CODE, this::uponReceiveJoinReply);
        registerMessageHandler(channelId, ForwardJoinMessage.MSG_CODE, this::uponReceiveForwardJoin);
        registerMessageHandler(channelId, HelloMessage.MSG_CODE, this::uponReceiveHello);
        registerMessageHandler(channelId, HelloReplyMessage.MSG_CODE, this::uponReceiveHelloReply);
        registerMessageHandler(channelId, DisconnectMessage.MSG_CODE, this::uponReceiveDisconnect, this::uponDisconnectSent);
        registerMessageHandler(channelId, ShuffleMessage.MSG_CODE, this::uponReceiveShuffle);
        registerMessageHandler(channelId, ShuffleReplyMessage.MSG_CODE, this::uponReceiveShuffleReply, this::uponShuffleReplySent);

        /*--------------------- Register Timer Handlers ----------------------------- */
        registerTimerHandler(ShuffleTimer.TimerCode, this::uponShuffleTime);
        registerTimerHandler(HelloTimeout.TimerCode, this::uponHelloTimeout);

        /*-------------------- Register Channel Event ------------------------------- */
        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

    }


    /*--------------------------------- Notifications ---------------------------------------- */

    private void uponRequestNumberOfNeighborsNotification(RequestedNumberOfNeighborsNotification notification, short sourceProto) {
        logger.debug("Received new neighbor request {}, current cache size is {}", notification.getRequestedNum(), this.active.getPeers().size());
        this.active.setCapacity(notification.getRequestedNum());

        if (notification.getRequestedNum() < this.active.getPeers().size()) {
            trimNeighbors(this.active.getPeers().size() - notification.getRequestedNum());
        }
    }

    private void trimNeighbors(int toTrim) {
        int newNeighLength = this.active.getPeers().size() - toTrim;

        if (newNeighLength < 4) {
            logger.error("Less than 4 neighbors after trimming!!");
            return;
        }

        for (int i = 0; i < toTrim; i++) {
            if (this.hpvRemovalMode.equals("random")) {
                logger.debug("Dropping random neighbor.");
                Host droppedNeigh = this.active.dropRandom();
                handleDropFromActive(droppedNeigh);
            } else if (this.hpvRemovalMode.equals("leastPowerful")) {
                Pair<Host, Integer> p = this.active.dropLeastPowerful();
                logger.debug("Removing by leastPowerful - host: {}, resource: {}.", p.fst, p.snd);
                handleDropFromActive(p.fst);
                return;
            } else if (this.hpvRemovalMode.equals("mostPowerful")) {
                Pair<Host, Integer> p = this.active.dropMostPowerful();
                logger.debug("Removing by mostPowerful - host: {}, resource: {}.", p.fst, p.snd);
                handleDropFromActive(p.fst);
                return;
            } else {
                logger.error("Don't know that removal mode!");
            }
        }
    }


    /*--------------------------------- Messages ---------------------------------------- */
    protected void handleDropFromActive(Host dropped) {
        if(dropped != null) {
            triggerNotification(new NeighborDown(dropped, (short) -1, (short)-1));
            sendMessage(new DisconnectMessage(), dropped);
            logger.debug("Sent DisconnectMessage to {}", dropped);
            passive.addPeer(dropped);
            logger.trace("Added to {} passive{}", dropped, passive);
        }
    }

    private void uponReceiveJoin(JoinMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        Host h = active.addPeer(from);
        active.addHostResource(from, msg.getFromResources());
        logger.trace("Added to {} active{}", from, active);
        triggerNotification(new NeighborUp(from, (short)-1, (short)-1));
        sendMessage( new JoinReplyMessage(myResources), from);
        logger.debug("Sent JoinReplyMessage to {}", from);
        handleDropFromActive(h);

        for(Host peer : active.getPeers()) {
            if(!peer.equals(from)) {
                sendMessage(new ForwardJoinMessage(ARWL, from, msg.getFromResources()), peer);
                logger.debug("Sent ForwardJoinMessage to {}", peer);
            }

        }
    }

    private void uponReceiveJoinReply(JoinReplyMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        if(!active.containsPeer(from)) {
            passive.removePeer(from);
            pending.remove(from);

            Host h = active.addPeer(from);
            active.addHostResource(from, msg.getFromResources());
            logger.trace("Added to {} active{}", from, active);
            triggerNotification(new NeighborUp(from, (short) -1, (short) -1));
            handleDropFromActive(h);
        }
    }

    private void uponReceiveForwardJoin(ForwardJoinMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        if(msg.decrementTtl() == 0 || active.getPeers().size() == 1) {
            if(!msg.getNewHost().equals(myself) && !active.containsPeer(msg.getNewHost())) {
                passive.removePeer(msg.getNewHost());
                pending.remove(msg.getNewHost());

                Host h = active.addPeer(msg.getNewHost());
                active.addHostResource(msg.getNewHost(), msg.getHostResources());
                logger.trace("Added to {} active{}", msg.getNewHost(), active);
                triggerNotification(new NeighborUp(msg.getNewHost(), (short) -1, (short) -1));
                sendMessage(new JoinReplyMessage(myResources), msg.getNewHost());
                logger.debug("Sent JoinReplyMessage to {}", msg.getNewHost());
                handleDropFromActive(h);
            }
        } else {
            if(msg.getTtl() == PRWL)  {
                passive.addPeer(msg.getNewHost());
                logger.trace("Added to {} passive{}", from, passive);
            }
            Host next = active.getRandomDiff(from);
            if(next != null) {
                sendMessage(msg, next);
                logger.debug("Sent ForwardJoinMessage to {}", next);
            }
        }
    }

    private void uponReceiveHello(HelloMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        if(msg.isPriority()) {
            if(!active.containsPeer(from)) {
                pending.remove(from);
                logger.trace("Removed from {} pending{}", from, pending);
                passive.removePeer(from);
                logger.trace("Removed from {} passive{}", from, passive);
                Host h = active.addPeer(from);
                active.addHostResource(from, msg.getFromResources());
                logger.trace("Added to {} active{}", from, active);
                triggerNotification(new NeighborUp(from, (short) -1, (short) -1));
                handleDropFromActive(h);
            }
            sendMessage(new HelloReplyMessage(true, myResources), from);
            logger.debug("Sent HelloReplyMessage to {}", from);

        } else {
            pending.remove(from);
            logger.trace("Removed from {} pending{}", from, pending);
            if(!active.fullWithPending(pending) || active.containsPeer(from)) {
                if(!active.containsPeer(from)) {
                    passive.removePeer(from);
                    logger.trace("Removed from {} passive{}", from, passive);
                    active.addPeer(from);
                    active.addHostResource(from, msg.getFromResources());
                    logger.trace("Added to {} active{}", from, active);
                    triggerNotification(new NeighborUp(from, (short) -1, (short) -1));
                }
                sendMessage(new HelloReplyMessage(true, myResources), from);
                logger.debug("Sent HelloReplyMessage to {}", from);
            } else {
                sendMessage(new HelloReplyMessage(false, myResources), from, TCPChannel.CONNECTION_IN);
                logger.debug("Sent HelloReplyMessage to {}", from);
            }
        }
    }

    private void uponReceiveHelloReply(HelloReplyMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        pending.remove(from);
        logger.trace("Removed from {} pending{}", from, pending);
        if(msg.isTrue()) {
            if(!active.containsPeer(from)) {
                timeout = originalTimeout;
                Host h = active.addPeer(from);
                active.addHostResource(from, msg.getFromResources());
                logger.trace("Added to {} active{}", from, active);
                triggerNotification(new NeighborUp(from, (short) -1, (short) -1));
                handleDropFromActive(h);
            }
        } else if(!active.containsPeer(from)){
            passive.addPeer(from);
            closeConnection(from);
            logger.trace("Added to {} passive{}", from, passive);
            if(!active.fullWithPending(pending)) {
                setupTimer(new HelloTimeout(), timeout);
            }
        }
    }

    protected void uponReceiveDisconnect(DisconnectMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        if(active.containsPeer(from)) {
            active.removePeer(from);
            logger.trace("Removed from {} active{}", from, active);
            handleDropFromActive(from);

            if(active.getPeers().isEmpty()) {
                timeout = originalTimeout;
            }

            if(!active.fullWithPending(pending)){
                setupTimer(new HelloTimeout(), timeout);
            }
        }
    }

    private void uponDisconnectSent(DisconnectMessage msg, Host host, short destProto, int channelId) {
        logger.trace("Sent {} to {}", msg, host);
        closeConnection(host);
    }

    private void uponReceiveShuffle(ShuffleMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        if(msg.decrementTtl() > 0 && active.getPeers().size() > 1) {
            Host next = active.getRandomDiff(from);
            sendMessage(msg, next);
            logger.debug("Sent ShuffleMessage to {}", next);
        } else if(!msg.getOrigin().equals(myself)) {
            logger.trace("Processing {}, passive{}", msg, passive);
            Set<Host> peers = new HashSet<>();
            peers.addAll(active.getRandomSample(msg.getFullSample().size()));
            Host[] hosts = peers.toArray(new Host[0]);
            int i = 0;
            for (Host host : msg.getFullSample()) {
                if (!host.equals(myself) && !active.containsPeer(host) && passive.isFull() && i < peers.size()) {
                    passive.removePeer(hosts[i]);
                    i++;
                }
                passive.addPeer(host);
            }
            logger.trace("After Passive{}", passive);
            sendMessage(new ShuffleReplyMessage(peers, msg.getSeqnum()), msg.getOrigin());
            logger.debug("Sent ShuffleReplyMessage to {}", msg.getOrigin());
        } else
            activeShuffles.remove(msg.getSeqnum());
    }

    private void uponShuffleReplySent(ShuffleReplyMessage msg, Host host, short destProto, int channelId) {
        if(!active.containsPeer(host) && !pending.contains(host)) {
            logger.trace("Disconnecting from {} after shuffleReply", host);
            closeConnection(host);
        }
    }

    private void uponReceiveShuffleReply(ShuffleReplyMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        Host[] sent = activeShuffles.remove(msg.getSeqnum());
        List<Host> sample = msg.getSample();
        sample.add(from);
        int i = 0;
        logger.trace("Processing {}, passive{}", msg, passive);
        for (Host h : sample) {
            if(!h.equals(myself) && !active.containsPeer(h) && passive.isFull() && i < sent.length) {
                passive.removePeer(sent[i]);
                i ++;
            }
            passive.addPeer(h);
        }
        logger.trace("After Passive{}", passive);
    }

    /*--------------------------------- Timers ---------------------------------------- */
    private void uponShuffleTime(ShuffleTimer timer, long timerId) {
        if(!active.fullWithPending(pending)){
            setupTimer(new HelloTimeout(), timeout);
        }

        Host h = active.getRandom();
        if(h != null) {
            Set<Host> peers = new HashSet<>();
            peers.addAll(active.getRandomSample(kActive));
            peers.addAll(passive.getRandomSample(kPassive));
            activeShuffles.put(seqNum, peers.toArray(new Host[0]));
            sendMessage(new ShuffleMessage(myself, peers, PRWL, seqNum), h);
            logger.debug("Sent ShuffleMessage to {}", h);
            seqNum = (short) ((short) (seqNum % Short.MAX_VALUE) + 1);
        }
    }

    private void uponHelloTimeout(HelloTimeout timer, long timerId) {
        if(!active.fullWithPending(pending)){
            Host h = passive.dropRandom();
            if(h != null && pending.add(h)) {
                logger.trace("Sending HelloMessage to {}, pending {}, active {}, passive {}", h, pending, active, passive);
                sendMessage(new HelloMessage(getPriority(), myResources), h);
                logger.debug("Sent HelloMessage to {}", h);
                timeout = (short) (Math.min(timeout * 2, MAX_BACKOFF));
            } else if(h != null)
                passive.addPeer(h);
        }
    }

    private boolean getPriority() {
        return active.getPeers().size() + pending.size() == 1;
    }

    /* --------------------------------- Channel Events ---------------------------- */

    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        logger.trace("Host {} is down, active{}, cause: {}", event.getNode(), active, event.getCause());
        if(active.removePeer(event.getNode())) {
            triggerNotification(new NeighborDown(event.getNode(), (short)-1, (short)-1));
            if(!active.fullWithPending(pending)){
                setupTimer(new HelloTimeout(), timeout);
            }
        } else
            pending.remove(event.getNode());
    }

    private void uponOutConnectionFailed(OutConnectionFailed event, int channelId) {
        logger.trace("Connection to host {} failed, cause: {}", event.getNode(), event.getCause());
        if(active.removePeer(event.getNode())) {
            triggerNotification(new NeighborDown(event.getNode(), (short)-1, (short)-1));
            if(!active.fullWithPending(pending)){
                setupTimer(new HelloTimeout(), timeout);
            }
        } else
            pending.remove(event.getNode());
    }

    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        logger.trace("Host (out) {} is up", event.getNode());
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.trace("Host (in) {} is up", event.getNode());
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        logger.trace("Connection from host {} is down, active{}, cause: {}", event.getNode(), active, event.getCause());
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        int peerId = Integer.parseInt(props.getProperty("peerID"));
        if(peerId == -1) {
            logger.error("Could not read peerId.");
        }
        String distribution = props.getProperty("distribution", "none");
        if(!distribution.equals("none")) {
            try (Stream<String> lines = Files.lines(Paths.get("src/main/java/protocols/optimization/resest/config/resources_"+distribution))) {
                try {
                    myResources = Integer.parseInt(lines.skip(peerId-1).findFirst().get());
                } catch (Exception e) {
                    logger.error("Could not read resource from line {}.", peerId);
                    myResources = -1;
                }
            } catch (Exception e) {
                logger.error("Could not read resource from line {}.", peerId);
                myResources = -1;
            }
        }

        logger.info("hpvRemovalMode being used: {}, myResources: {}", this.hpvRemovalMode, this.myResources);

        if (props.containsKey("contacts")) {
            try {
                String[] allContacts = props.getProperty("contacts").split(",");

                List<Host> contacts = new ArrayList<>(allContacts.length);
                for(String contact : allContacts) {
                    String[] hostElems = contact.split(":");
                    contacts.add(new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1])));
                }

                JoinMessage m = new JoinMessage(myResources);
                sendMessage(m,  contacts.get(0));
                logger.debug("Sent JoinMessage to {}", contacts.get(0));
                logger.trace("Sent " + m + " to " + contacts.get(0));
            } catch (Exception e) {
                System.err.println("Invalid contact on configuration: '" + props.getProperty("contacts"));
                e.printStackTrace();
                System.exit(-1);
            }
        }

        setupPeriodicTimer(new ShuffleTimer(), this.shuffleTime, this.shuffleTime);
    }
}
