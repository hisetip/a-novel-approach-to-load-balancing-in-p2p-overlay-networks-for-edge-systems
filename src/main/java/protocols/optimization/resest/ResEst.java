package protocols.optimization.resest;

import babel.exceptions.HandlerRegistrationException;
import babel.generic.GenericProtocol;
import babel.generic.ProtoMessage;
import babel.generic.ProtoNotification;
import babel.generic.ProtoTimer;
import com.sun.tools.javac.util.Pair;
import jdk.internal.loader.Resource;
import jdk.javadoc.internal.tool.Start;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.membership.cyclon.Cyclon;
import protocols.membership.notifications.NeighborDownResEst;
import protocols.membership.notifications.NeighborUpResEst;
import protocols.optimization.resest.messages.HistogramAnswerMessage;
import protocols.optimization.resest.messages.HistogramMessage;
import protocols.optimization.resest.notifications.ResourceHistogramNotification;
import protocols.optimization.resest.notifications.StartNeighVariabilityPrintsNotification;
import protocols.optimization.resest.timers.*;
import utils.FileArrayProvider;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class ResEst extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(ResEst.class);

    public final static short PROTOCOL_ID = 23000;
    public final static String PROTOCOL_NAME = "ResEst";

    Host myself;
    Set<Host> neighbors;
    int myResources;
    float maxAcceptableMargin;
    Map<String, Float> tDistributionTable;
    int peerId;
    int safetyTTL;
    int confidenceLevel;
    long timerID;
    boolean canBroadcast;
    Map<Integer, Integer> realHistogram;
    int queryFrequency;
    int runningTime;
    String replaceResource;

    public ResEst(String channelName, Properties properties, Host myself) throws IOException, HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);

        // init variables
        this.myself = myself;
        neighbors = new HashSet<>();
        peerId = Integer.parseInt(properties.getProperty("peerID", "-1"));
        if(peerId == -1) {
            logger.error("Could not read peerId.");
        }
        String distribution = properties.getProperty("distribution", "");
        if(distribution.equals("none")) {
            logger.error("Could not read distribution.");
        }
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
        maxAcceptableMargin = Float.parseFloat(properties.getProperty("maxAcceptableMargin", ""));
        tDistributionTable = null;
        safetyTTL = Integer.parseInt(properties.getProperty("safetyTTL", ""));
        confidenceLevel = Integer.parseInt(properties.getProperty("confidenceLevel", ""));
        timerID = -1;
        canBroadcast = false;
        queryFrequency = Integer.parseInt(properties.getProperty("queryFrequency", ""));
        runningTime = Integer.parseInt(properties.getProperty("runningTime", ""));
        realHistogram = getRealHistogram(Integer.parseInt(properties.getProperty("numberOfNodes", "")),
                properties.getProperty("distribution", ""));
        replaceResource = null;

        int channelId = createChannel(channelName, properties);

        // Register Message Serializers
        registerMessageSerializer(HistogramMessage.MSG_CODE, HistogramMessage.serializer);
        registerMessageSerializer(HistogramAnswerMessage.MSG_CODE, HistogramAnswerMessage.serializer);

        // Register Message Handlers
        registerMessageHandler(channelId, HistogramMessage.MSG_CODE, this::uponReceiveHistogramMessage, this::uponFailSendingMessage);
        registerMessageHandler(channelId, HistogramAnswerMessage.MSG_CODE, this::uponReceiveHistogramAnswerMessage, this::uponFailSendingMessage);

        // Register Timer Handlers
        registerTimerHandler(CooldownTimer.TIMER_CODE, this::uponCooldownTimer);
        registerTimerHandler(HeartbeatTimer.TIMER_CODE, this::uponHeartbeatTimer);
        registerTimerHandler(ResourceEstimatorTimer.TIMER_CODE, this::uponResourceEstimatorTimer);
        registerTimerHandler(StartupTimer.TIMER_CODE, this::uponStartupTimer);
        registerTimerHandler(ChangeResourcesTimer.TIMER_CODE, this::uponChangeResourcesTimer);

        // Register Notification Handlers
        subscribeNotification(NeighborUpResEst.NOTIFICATION_ID, this::uponNeighborUp);
        subscribeNotification(NeighborDownResEst.NOTIFICATION_ID, this::uponNeighborDown);
    }


    /*--------------------------------- Message Handlers ---------------------------------------- */

    private void uponReceiveHistogramMessage(HistogramMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received histogram message from {} and it should return to {}.", from, msg.getOriginalSender());
        if (msg.getSeenPeers().containsKey(myself.toString())) {
            Host p = getRandomNeighbor();
            if (p == null) {
                logger.warn("No neighbors to redirect histogram message already seen by me.");
                return;
            }
            sendMessage(msg, p);
            logger.debug("Redirecting histogrammessage to {} bc had already passed by me.", p);
            return;
        }

        msg.getSeenPeers().put(myself.toString(), true);

        updateMessageHistogram(msg, myResources);

        Pair<Boolean, Float> isMarginOfErrorWithinB = isMarginOfErrorWithinBounds(msg, maxAcceptableMargin);
        boolean is = isMarginOfErrorWithinB.fst;
        float mean = isMarginOfErrorWithinB.snd;
        if (is) {
            logger.debug("Sending HistogramAnswerMessage to {}", msg.getOriginalSender());
            HistogramAnswerMessage histogramAnswerMessage = new HistogramAnswerMessage(msg.getHistogram(), mean);
            sendMessage(histogramAnswerMessage, msg.getOriginalSender());
        } else {
            msg.decrementTtl();
            if (msg.getTtl() <= 0) {
                logger.debug("TTL hit.");
            }

            Host p = getRandomNeighbor();
            if (p == null) {
                logger.warn("No neighbors to redirect histogram message.");
                return;
            }
            logger.debug("Redirecting histogrammessage with ttl {} to {} bc margin was off.", msg.getTtl(), p);
            sendMessage(msg, p);
        }
    }

    private void uponReceiveHistogramAnswerMessage(HistogramAnswerMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received histogram answermessage from {}.", from);
        int totalHops = 0;
        logger.info("------------------ Histogram Received! ----------------------");
        for ( Map.Entry<Integer, Integer> entry : msg.getHistogram().entrySet()) {
            logger.info("{} -> {}", entry.getKey(), entry.getValue());
            totalHops += entry.getValue();
        }
        logger.info("-------------------------------------------------------------");
        logger.info("Mean: {}", msg.getMean());
        logger.info("-------------------------------------------------------------");
        logger.info("Total number of hops: {}", totalHops);
        logger.info("-------------------------------------------------------------");

        //Computing histogram errors and printing them
        Map<Integer, Integer> rh = reduceHistogram(msg.getHistogram(), myResources);
        Map<Integer, Float> normalizedHistogram = normalizeHistogram(rh);
        Map<Integer, Integer> rrh = reduceHistogram(realHistogram, myResources);
        Map<Integer, Float> nrh = normalizeHistogram(rrh);
        float errHist = compareHistograms(nrh, normalizedHistogram);
        logger.info("Histogram Error: {}", errHist);
        logger.info("-------------------------------------------------------------");

        triggerNotification(new ResourceHistogramNotification(myResources, msg.getHistogram(), realHistogram));
    }

    private void uponFailSendingMessage(ProtoMessage protoMessage, Host host, short i, Throwable throwable, int i1) {
        logger.error("Failed senting message {} to {}.", protoMessage.getId(), host.toString());
    }


    /*--------------------------------- Timer Handlers ---------------------------------------- */

    private void uponResourceEstimatorTimer(ResourceEstimatorTimer timer, long timerId) {
        if (!canBroadcast) {
            return;
        }

        logger.debug("Triggered timer to send query.");
        Host p = getRandomNeighbor();
        if (p == null) {
            logger.warn("No neighbors uponResourceEstimatorTimer");
            return;
        }
        Map<String, Boolean> seenP = new HashMap<>();
        seenP.put(myself.toString(), true);
        HistogramMessage m = new HistogramMessage(myself, seenP, new HashMap<>(), this.safetyTTL);
        logger.debug("Sending resource estimation message.");
        sendMessage(m, p);
    }

    private void uponStartupTimer(StartupTimer timer, long timerId) {
        this.timerID = setupPeriodicTimer(new ResourceEstimatorTimer(), this.queryFrequency* 1000L, this.queryFrequency* 1000L);
        canBroadcast = true;
        triggerNotification(new StartNeighVariabilityPrintsNotification());
        logger.debug("Set periodic timer with ID {} and frequency {}.", this.timerID, this.queryFrequency);
        logger.info("Now running!");
        setupTimer(new CooldownTimer(), this.runningTime*1000L);

        if (this.replaceResource != null) {
            setupTimer(new ChangeResourcesTimer(), (this.runningTime*1000L)/2L);
        }
    }

    private void uponCooldownTimer(CooldownTimer timer, long timerId) {
        logger.debug("Trying to cancel the timer with ID {}.", this.timerID);
        canBroadcast = false;
        if (cancelTimer(this.timerID) == null) {
            logger.warn("Could not cancel timer. However, we got the canBroadcast flag to save us, no worries.");
        }
    }

    private void uponHeartbeatTimer(HeartbeatTimer timer, long timerId) {
        logger.debug("----------");
        logger.debug("NU Neighbors:");
        for (Host neighbor : neighbors) {
            logger.debug("{}", neighbor.toString());
        }
        logger.debug("----------");
    }

    private void uponChangeResourcesTimer(ChangeResourcesTimer timer, long timerId) {
        int oldRes = this.myResources;
        if (this.replaceResource.equals("replaceWeak")) {
            if (this.myResources < 20) {
                this.myResources = ThreadLocalRandom.current().nextInt(80, 100);
            }
        } else if (this.replaceResource.equals("replaceStrong")) {
            if (this.myResources > 80) {
                this.myResources = ThreadLocalRandom.current().nextInt(2, 21);
            }
        } else {
            logger.error("I don't know that replace resource mode!");
        }

        logger.info("Changing resources from {} to {}", oldRes, this.myResources);
    }


    /*--------------------------------- Notification Handlers ---------------------------------------- */

    private void uponNeighborDown(NeighborDownResEst notification, short sourceProto) {
        if (notification.getNeighbour() == null) {
            return;
        }
        neighbors.remove(notification.getNeighbour());
        logger.debug("Peer down - {}", notification.getNeighbour());
    }

    private void uponNeighborUp(NeighborUpResEst notification, short sourceProto) {
        if (notification.getNeighbour() == null) {
            return;
        }
        if (notification.getNeighbour().equals(myself)) {
            logger.error("Trying to add myself as neighbor!");
            return;
        }
        neighbors.add(notification.getNeighbour());
        logger.debug("Peer up - {}", notification.getNeighbour());
    }


    /*--------------------------------- Aux. Functions ---------------------------------------- */

    private Map<Integer, Integer> getRealHistogram(int numberOfNodes, String distribution) {
        Integer[] res = new Integer[numberOfNodes];
        try {
            res = FileArrayProvider.readLines("src/main/java/protocols/optimization/resest/config/resources_"+distribution);
        } catch (Exception e) {
            logger.error("Could not read the resource array in getRealHistogram(...).");
        }

        Map<Integer, Integer> rh = new HashMap<>();
        for (int i = 0; i < res.length; i++) {
            rh.put(res[i], rh.getOrDefault(res[i], 0)+1);
        }

        return rh;
    }

    private Host getRandomNeighbor() {
        if(neighbors.isEmpty()) {
            return null;
        }

        int size = neighbors.size();
        int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
        int i = 0;
        for(Host obj : neighbors)
        {
            if (i == item)
                return obj;
            i++;
        }

        logger.error("Should never get here (in getRandomNeighbor())...");
        return null;
    }

   private Pair<Boolean, Float> isMarginOfErrorWithinBounds(HistogramMessage msg, float maxInterval) {
        int valueSum = 0;
        int n = 0;

        for ( Map.Entry<Integer, Integer> entry : msg.getHistogram().entrySet()) {
            valueSum += entry.getKey() * entry.getValue();
            n += entry.getValue();
        }
        float mean = (float) valueSum / (float) n;

        if(n < 10) {
            return new Pair<>(false, mean);
        }

        float sumDif = 0.0F;
        for ( Map.Entry<Integer, Integer> entry : msg.getHistogram().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                sumDif += Math.pow((float) entry.getKey() - mean, 2);
            }
        }
        logger.debug("--------------isMarginOfErrorWithinBounds--------------");
        logger.debug("sumDif: {}, Mean: {}", sumDif, mean);
        float standardDeviation = (float) Math.sqrt(sumDif/(float) n);
        logger.debug("Standard deviation - {}, n: {}", standardDeviation, n);
        float marginOfError = (float) (getTValue(n) * (standardDeviation/Math.sqrt((float) n)));
        logger.debug("t value - {}", getTValue(n));
        logger.debug("Mean - {}", mean);

        float marginOfErrorRatio = marginOfError / mean;

        logger.debug("Margin of error - {}, margin of error ratio: {}", marginOfError, marginOfErrorRatio);
        logger.debug("Max interval - {}", maxInterval);
        logger.debug("-------------------------------------------------------");

        if (marginOfErrorRatio <= maxInterval) {
            return new Pair<>(true, mean);
        } else {
            return new Pair<>(false, mean);
        }
    }

    private float getTValue(int n) {
        if(tDistributionTable == null) {
            tDistributionTable = new HashMap<>();

            try(BufferedReader br = new BufferedReader(new FileReader("src/main/java/protocols/optimization/resest/config/t-distribution-table-" + confidenceLevel + ".txt"))) {
                int i = 1;
                for(String line; (line = br.readLine()) != null && i <= 31; i++) {
                    if(i <= 30) {
                        tDistributionTable.put(Integer.toString(i), Float.parseFloat(line));
                    } else {
                        tDistributionTable.put("z", Float.parseFloat(line));
                    }
                }
            } catch (Exception e) {
                logger.error("On getTValue, could not parse file {}", "src/main/java/protocols/optimization/resest/config/t-distribution-table-" + confidenceLevel + ".txt");
            }
        }

        if (n > 0 && n <= 30) {
            return tDistributionTable.get(Integer.toString(n));
        } else if (n > 30) {
            return tDistributionTable.get("z");
        } else {
            logger.error("Trying to get t-distribution value for negative number or 0!");
            return -1.0F;
        }
    }

    private void updateMessageHistogram(HistogramMessage h, int myRes) {
        h.getHistogram().put(myRes, h.getHistogram().getOrDefault(myRes, 0) + 1);
    }

    // Reduces to 5 classes
    private Map<Integer, Integer> reduceHistogram(Map<Integer, Integer> h, int myResource) {
        Map<Integer, Integer> newReducedHistogram = new HashMap<>();
        for ( Map.Entry<Integer, Integer> entry : h.entrySet()) {
            int k = entry.getKey();
            int v = entry.getValue();
            if (k < 0) {
                logger.error("Resource level is negative... (reduce histogram)");
            } else if (k < myResource - (int)(((float)(myResource))*0.5)) {
                newReducedHistogram.put(0, newReducedHistogram.getOrDefault(0, 0) + v);
            } else if (k < myResource - (int)(((float)(myResource))*0.1)) {
                newReducedHistogram.put(myResource - (int)(((float)(myResource))*0.5), newReducedHistogram.getOrDefault(myResource - (int)(((float)(myResource))*0.5), 0) + v);
            } else if (k < myResource + (int)(((float)(myResource))*0.1)) {
                newReducedHistogram.put(myResource - (int)(((float)(myResource))*0.1), newReducedHistogram.getOrDefault(myResource - (int)(((float)(myResource))*0.1), 0) + v);
            } else if (k < myResource + (int)(((float)(myResource))*0.5)) {
                newReducedHistogram.put(myResource + (int)(((float)(myResource))*0.1), newReducedHistogram.getOrDefault(myResource + (int)(((float)(myResource))*0.1), 0) + v);
            } else {
                newReducedHistogram.put(myResource + (int)(((float)(myResource))*0.5), newReducedHistogram.getOrDefault(myResource + (int)(((float)(myResource))*0.5), 0) + v);
            }
        }

        return newReducedHistogram;
    }

    private float compareHistograms(Map<Integer, Float> realHistogram, Map<Integer, Float> obtainedHistogram) {
        float sumErrors = 0.0F;
        for ( Map.Entry<Integer, Float> entry : realHistogram.entrySet()) {
            sumErrors += Math.abs(entry.getValue() - obtainedHistogram.getOrDefault(entry.getKey(), 0.0F));
        }
        return sumErrors/2.0F;
    }

    private Map<Integer, Float> normalizeHistogram(Map<Integer, Integer> histogram) {
        Map<Integer, Float> newNormalizedHistogram = new HashMap<>();

        int totalSamples = 0;
        for ( Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            totalSamples += entry.getValue();
        }
        for ( Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            newNormalizedHistogram.put(entry.getKey(), (float) entry.getValue() / (float) totalSamples);
        }

        return newNormalizedHistogram;
    }


    /*--------------------------------- Init ---------------------------------------- */
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        long realStartUpTime = (Long.parseLong(properties.getProperty("startT"))*1000 + Long.parseLong(properties.getProperty("startupTime"))*1000) - System.currentTimeMillis();
        setupTimer(new StartupTimer(), realStartUpTime);
        setupPeriodicTimer(new HeartbeatTimer(), 5000, 5000);

        String replace = properties.getProperty("replace", "");
        if (!replace.equals("")) {
            replaceResource = replace;
            logger.info("Using replaceResource {}", replace);
        }

        logger.info("Starting up with peerId {}, distribution {}, " +
                "maxAcceptableMargin {}, safetyTTL {}, confidenceLevel {}, " +
                "queryFrequency {}, runningTime {}, numberOfNodes {}.",
                peerId,
                properties.getProperty("distribution", ""),
                maxAcceptableMargin,
                safetyTTL,
                confidenceLevel,
                queryFrequency,
                runningTime,
                Integer.parseInt(properties.getProperty("numberOfNodes", ""))
                );
    }
}
