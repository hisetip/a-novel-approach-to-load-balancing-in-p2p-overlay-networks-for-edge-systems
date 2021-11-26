package protocols.membership.hyparviewintegrated.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;

public class HelloMessage extends ProtoMessage {
    public final static short MSG_CODE = 16103;

    //prio == true -> high; prio == false --> low
    private boolean priority;

    private final int fromResources;

    public HelloMessage(boolean priority, int fromResources) {
        super(HelloMessage.MSG_CODE);
        this.priority = priority;
        this.fromResources = fromResources;
    }

    @Override
    public String toString() {
        return "HelloMessage{" +
                "priority=" + priority +
                "fromResources=" + fromResources +
                '}';
    }

    public int getFromResources() {
        return fromResources;
    }

    public boolean isPriority() {
        return priority;
    }

    public static final ISerializer<HelloMessage> serializer = new ISerializer<HelloMessage>() {
        @Override
        public void serialize(HelloMessage m, ByteBuf out) {
            out.writeBoolean(m.priority);
            out.writeInt(m.fromResources);
        }

        @Override
        public HelloMessage deserialize(ByteBuf in) throws UnknownHostException {
            boolean priority = in.readBoolean();
            int fR = in.readInt();

            return new HelloMessage(priority, fR);
        }
    };
}
