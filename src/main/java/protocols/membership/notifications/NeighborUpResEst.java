package protocols.membership.notifications;

import babel.generic.ProtoNotification;
import network.data.Host;

public class NeighborUpResEst extends ProtoNotification {

    public static final short NOTIFICATION_ID = 10505;

    private final Host neighbour;

    public NeighborUpResEst(Host neighbour) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public NeighborUpResEst(Host neighbour, short x, short y) {
        super(NOTIFICATION_ID);
        this.neighbour = neighbour;
    }

    public Host getNeighbour() {
        return neighbour;
    }
}
