package protocols.membership.notifications;

import babel.generic.ProtoNotification;
import network.data.Host;

public class NeighborDownResEst extends ProtoNotification {

    public static final short NOTIFICATION_ID = 10504;

    private final Host neighbour;

    public NeighborDownResEst(Host neighbour) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public NeighborDownResEst(Host neighbour, short x, short y) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public Host getNeighbour() {
        return neighbour;
    }
}
