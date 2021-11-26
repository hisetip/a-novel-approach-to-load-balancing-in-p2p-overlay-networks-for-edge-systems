package protocols.membership.hyparviewintegrated.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;

public class JoinReplyMessage extends ProtoMessage {
    public final static short MSG_CODE = 16106;

    private final int fromResources;

    public JoinReplyMessage(int fromResources) {
        super(MSG_CODE);
        this.fromResources = fromResources;
    }

    public int getFromResources() {
        return fromResources;
    }

    @Override
    public String toString() {
        return "JoinReplyMessage{fromResources = " + this.fromResources + "}";
    }

    public static final ISerializer<JoinReplyMessage> serializer = new ISerializer<JoinReplyMessage>() {
        @Override
        public void serialize(JoinReplyMessage m, ByteBuf out) {
            out.writeInt(m.fromResources);
        }

        @Override
        public JoinReplyMessage deserialize(ByteBuf in) throws UnknownHostException {
            int fR = in.readInt();
            return new JoinReplyMessage(fR);
        }
    };
}
