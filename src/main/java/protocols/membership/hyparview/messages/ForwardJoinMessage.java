package protocols.membership.hyparview.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;

import java.io.IOException;

public class ForwardJoinMessage extends ProtoMessage {
    public final static short MSG_CODE = 16102;

    private short ttl;
    private final Host newHost;
    private final int hostResources;

    public ForwardJoinMessage(short ttl, Host newHost, int hostResources) {
        super(ForwardJoinMessage.MSG_CODE);
        this.ttl = ttl;
        this.newHost = newHost;
        this.hostResources = hostResources;
    }

    @Override
    public String toString() {
        return "ForwardJoinMessage{" +
                "ttl=" + ttl +
                ", newHost=" + newHost +
                ", hostResources=" + hostResources +
                '}';
    }

    public Host getNewHost() {
        return newHost;
    }

    public short getTtl() {
        return ttl;
    }

    public short decrementTtl() {
        return ttl--; //decrement after returning
    }

    public int getHostResources() {
        return hostResources;
    }

    public static final ISerializer<ForwardJoinMessage> serializer = new ISerializer<ForwardJoinMessage>() {
        @Override
        public void serialize(ForwardJoinMessage m, ByteBuf out) throws IOException {
            out.writeShort(m.ttl);
            Host.serializer.serialize(m.newHost, out);
            out.writeInt(m.hostResources);
        }

        @Override
        public ForwardJoinMessage deserialize(ByteBuf in) throws IOException {
            short ttl = in.readShort();
            Host newHost = Host.serializer.deserialize(in);
            int hR = in.readInt();

            return new ForwardJoinMessage(ttl, newHost, hR);
        }
    };
}
