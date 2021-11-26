package protocols.dissemination.notifications;

import babel.generic.ProtoNotification;

public class DeliverMessageNotification extends ProtoNotification {

    public static final short NOTIFICATION_ID = 20501;

    private final byte[] content;

    public DeliverMessageNotification(byte[] content) {
        super(NOTIFICATION_ID);
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }
}
