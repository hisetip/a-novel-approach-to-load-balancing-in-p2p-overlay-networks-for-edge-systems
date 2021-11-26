package protocols.membership.hyparviewintegrated.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;

public class HelloReplyMessage extends ProtoMessage {
    public final static short MSG_CODE = 16104;

    private final boolean reply;
    private final int fromResources;

    public HelloReplyMessage(boolean reply, int fromResources) {
        super(HelloReplyMessage.MSG_CODE);
        this.reply = reply;
        this.fromResources = fromResources;
    }

    @Override
    public String toString() {
        return "HelloReplyMessage{" +
                "reply=" + reply +
                "fromResources=" + fromResources +
                '}';
    }

    public int getFromResources() {
        return fromResources;
    }

    public boolean isTrue() {
        return reply;
    }

    public static final ISerializer<HelloReplyMessage> serializer = new ISerializer<HelloReplyMessage>() {
        @Override
        public void serialize(HelloReplyMessage m, ByteBuf out) {
            out.writeBoolean(m.reply);
            out.writeInt(m.fromResources);
        }

        @Override
        public HelloReplyMessage deserialize(ByteBuf in) throws UnknownHostException {
            boolean reply = in.readBoolean();
            int fR = in.readInt();

            return new HelloReplyMessage(reply, fR);
        }

    };
}
