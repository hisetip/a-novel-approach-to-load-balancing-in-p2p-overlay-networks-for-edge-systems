package protocols.membership.hyparview.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.net.UnknownHostException;

public class JoinMessage extends ProtoMessage {
    public final static short MSG_CODE = 16105;

    private final int fromResources;


    public JoinMessage(int fromResources) {
        super(JoinMessage.MSG_CODE);
        this.fromResources = fromResources;
    }

    @Override
    public String toString() {
        return "JoinMessage{fromResources = " + this.fromResources + "}";
    }

    public int getFromResources() {
        return fromResources;
    }

    public static final ISerializer<JoinMessage> serializer = new ISerializer<JoinMessage>() {
        @Override
        public void serialize(JoinMessage m, ByteBuf out) {
            out.writeInt(m.fromResources);
        }

        @Override
        public JoinMessage deserialize(ByteBuf in) throws UnknownHostException {
            int fR = in.readInt();
            return new JoinMessage(fR);
        }
    };
}
