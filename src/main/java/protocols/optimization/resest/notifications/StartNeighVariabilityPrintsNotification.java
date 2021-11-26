package protocols.optimization.resest.notifications;

import babel.generic.ProtoNotification;

public class StartNeighVariabilityPrintsNotification extends ProtoNotification {
    public static final short NOTIFICATION_ID = 23502;

    public StartNeighVariabilityPrintsNotification() {
        super(NOTIFICATION_ID);
    }
}
