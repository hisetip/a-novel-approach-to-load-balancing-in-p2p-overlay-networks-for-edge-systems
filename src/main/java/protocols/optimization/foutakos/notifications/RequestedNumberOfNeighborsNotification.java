package protocols.optimization.foutakos.notifications;

import babel.generic.ProtoNotification;

public class RequestedNumberOfNeighborsNotification extends ProtoNotification {
    public static final short NOTIFICATION_ID = 24501;

    private final int requestedNum;

    public RequestedNumberOfNeighborsNotification(int requestedNum) {
        super(NOTIFICATION_ID);
        this.requestedNum = requestedNum;
    }

    public int getRequestedNum() {
        return requestedNum;
    }
}
