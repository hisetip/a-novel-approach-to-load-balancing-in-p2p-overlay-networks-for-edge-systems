package protocols.membership.notifications;

import babel.generic.ProtoNotification;
import network.data.Host;

public class NeighborUp extends ProtoNotification {

    public static final short NOTIFICATION_ID = 10503;

    private final Host neighbour;

    public NeighborUp(Host neighbour) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public NeighborUp(Host neighbour, short x, short y) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public Host getNeighbour() {
        return neighbour;
    }
}
