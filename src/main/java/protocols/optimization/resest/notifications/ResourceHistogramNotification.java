package protocols.optimization.resest.notifications;

import babel.generic.ProtoNotification;

import java.util.Map;

public class ResourceHistogramNotification extends ProtoNotification {
    public static final short NOTIFICATION_ID = 23501;

    private final int myResources;
    private final Map<Integer, Integer> histogram;
    private final Map<Integer, Integer> realHistogram;

    public ResourceHistogramNotification(int myResources, Map<Integer, Integer> histogram, Map<Integer, Integer> realHistogram) {
        super(NOTIFICATION_ID);
        this.myResources = myResources;
        this.histogram = histogram;
        this.realHistogram = realHistogram;
    }

    public int getMyResources() {
        return myResources;
    }

    public Map<Integer, Integer> getHistogram() {
        return histogram;
    }

    public Map<Integer, Integer> getRealHistogram() {
        return realHistogram;
    }
}
