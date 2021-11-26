package protocols.membership.notifications;

import babel.generic.ProtoNotification;
import network.data.Host;

public class NeighborDown extends ProtoNotification {

    public static final short NOTIFICATION_ID = 10502;

    private final Host neighbour;

    public NeighborDown(Host neighbour) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public NeighborDown(Host neighbour, short x, short y) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public Host getNeighbour() {
        return neighbour;
    }
}
