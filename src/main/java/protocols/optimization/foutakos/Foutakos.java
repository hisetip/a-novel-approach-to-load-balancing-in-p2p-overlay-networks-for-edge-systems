package protocols.optimization.foutakos;

import babel.exceptions.HandlerRegistrationException;
import babel.generic.GenericProtocol;
import babel.generic.ProtoNotification;
import network.data.Host;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import protocols.optimization.foutakos.notifications.RequestedNumberOfNeighborsNotification;
import protocols.optimization.resest.notifications.ResourceHistogramNotification;

import java.io.IOException;
import java.sql.Array;
import java.util.*;

public class Foutakos extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(Foutakos.class);

    public final static short PROTOCOL_ID = 24000;
    public final static String PROTOCOL_NAME = "Foutakos";

    int defaultNumberOfNeighbors;
    String mode;
    float rateOfOptimization;
    int minNumberOfNeighbors;
    int lastDesiredNumberOfNeighbors;
    int myResources;
    Map<Integer, Integer> histogram;
    List<Integer> desiredNumNeighArr;
    Map<Integer, Integer> realHistogram;

    public Foutakos(Properties properties) throws HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);

        //init variables
        this.defaultNumberOfNeighbors = Integer.parseInt(properties.getProperty("defaultNumberNeighbors"));
        this.mode = properties.getProperty("mode");
        this.rateOfOptimization = Float.parseFloat(properties.getProperty("rateOfOptimization"));
        this.minNumberOfNeighbors = Integer.parseInt(properties.getProperty("minNumberOfNeighbors"));
        this.lastDesiredNumberOfNeighbors = this.defaultNumberOfNeighbors;
        this.myResources = -1;
        this.histogram = new HashMap<>();
        this.desiredNumNeighArr = new ArrayList<>();
        this.desiredNumNeighArr.add(this.defaultNumberOfNeighbors);

        // Subscribe to notifs
        subscribeNotification(ResourceHistogramNotification.NOTIFICATION_ID, this::uponResourceHistogramNotification);
    }


    /*--------------------------------- Notification Handlers ---------------------------------------- */

    private void uponResourceHistogramNotification(ResourceHistogramNotification notification, short sourceProto) {
        logger.debug("Received ResourceHistogramNotification.");

        this.myResources = notification.getMyResources();
        this.histogram = notification.getHistogram();
        this.realHistogram = notification.getRealHistogram();

        final float percentile = computePercentileEstimatedHist();
        final float realPercentile = computePercentileRealHist();

        int numberOfNeighComputed = optimizationFunction(percentile);

        requestSpecificNumberOfNeighbors(numberOfNeighComputed);

        int numOfNeighItShouldHave = optimizationFunction(realPercentile);
        if (numOfNeighItShouldHave < this.minNumberOfNeighbors) {
            numOfNeighItShouldHave = this.minNumberOfNeighbors;
        }
        logger.info("The number of neighbors it should have: {}", numOfNeighItShouldHave);
    }


    /*--------------------------------- Aux. Functions ---------------------------------------- */

    private int optimizationFunction(float p) {
        final float optimizationFun = 2 * this.defaultNumberOfNeighbors * this.rateOfOptimization * p
                + this.defaultNumberOfNeighbors
                - this.defaultNumberOfNeighbors * this.rateOfOptimization;

        logger.debug("Optimization function for percentile p: {}", Math.round(optimizationFun));
        return Math.round(optimizationFun);
    }

    private float computePercentileEstimatedHist() {
        int belowMyResources = 0;
        int totalSamples = 0;
        for ( Map.Entry<Integer, Integer> entry : this.histogram.entrySet()) {
            int k = entry.getKey();
            int v = entry.getValue();
            if (k <= this.myResources) {
                belowMyResources += v;
            }
            totalSamples += v;
        }

        float percentile = (float) belowMyResources / (float) totalSamples;
        logger.debug("Computed percentile of estimation: {}", percentile);

        return percentile;
    }

    private float computePercentileRealHist() {
        int belowMyResources = 0;
        int totalSamples = 0;
        for ( Map.Entry<Integer, Integer> entry : this.realHistogram.entrySet()) {
            int k = entry.getKey();
            int v = entry.getValue();
            if (k <= this.myResources) {
                belowMyResources += v;
            }
            totalSamples += v;
        }

        float percentile = (float) belowMyResources / (float) totalSamples;
        logger.debug("Computed real percentile: {}", percentile);

        return percentile;
    }

    private void requestSpecificNumberOfNeighbors(int n) {
        int numNeigh = n;
        if (numNeigh < this.minNumberOfNeighbors) {
            numNeigh = this.minNumberOfNeighbors;
            logger.debug("Processing request with min number of neighbors.");
        }
        logger.debug("Computed desired number of neighbors: {}", numNeigh);

        this.lastDesiredNumberOfNeighbors = numNeigh;
        this.desiredNumNeighArr.add(numNeigh);

        int numToSend = checkIfShouldSendNotif();
        if (numToSend != -1) {
            logger.debug("Sent RequestdNumberOfNeighborsNotification: {}", numToSend);
            triggerNotification(new RequestedNumberOfNeighborsNotification(numToSend));
            logger.info("The number of neighbors it requested: {}", numToSend);
        } else {
            logger.debug("Did not send notif.");
        }
    }

    private int checkIfShouldSendNotif() {
        switch (this.mode) {
            case "simple":
                return this.lastDesiredNumberOfNeighbors;
            case "average":
                int l = this.desiredNumNeighArr.size();

                int avg;
                if (l < 6) {
                    int sumRes = 0;
                    for (int i = 1; i < l; i++) {
                        sumRes += this.desiredNumNeighArr.get(i);
                    }
                    avg = Math.round((float)sumRes / (float) (l-1));
                } else {
                    int sumRe = 0;
                    for (int j = l - 5; j < l; j++) {
                        sumRe += this.desiredNumNeighArr.get(j);
                    }
                    avg = Math.round((float)sumRe / 5.0F);
                }

                logger.debug("Last desired numbers: {}", desiredNumNeighArr);
                return avg;
            default:
                logger.error("Don't know that mode! Use either 'simple' or 'average'.");
                return -1;
        }
    }


    /*--------------------------------- Init ---------------------------------------- */
    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {
        logger.info("Starting up with defaultNumberOfNeighbors {}, minNumberOfNeighbors {}, mode {} and rateOfOptimization {}.",
                defaultNumberOfNeighbors, minNumberOfNeighbors, mode, rateOfOptimization);
    }
}
