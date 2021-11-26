package protocols.membership.notifications;

import babel.generic.ProtoNotification;
import network.data.Host;

public class ChannelCreated extends ProtoNotification {

    public static final short NOTIFICATION_ID = 10501;

    private final int channelId;

    public ChannelCreated(int channelId) {
        super(NOTIFICATION_ID);
        this.channelId = channelId;
    }

    public int getChannelId() {
        return channelId;
    }
}
