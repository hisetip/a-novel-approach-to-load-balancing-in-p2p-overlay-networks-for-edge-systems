package protocols.optimization.resest.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HistogramMessage extends ProtoMessage {
    public final static short MSG_CODE = 23101;

    private final Host originalSender;
    private final Map<String, Boolean> seenPeers;
    private final Map<Integer, Integer> histogram;
    private int ttl;

    public HistogramMessage(Host originalSender, Map<String, Boolean> seenPeers, Map<Integer, Integer> histogram, int ttl) {
        super(MSG_CODE);
        this.originalSender = originalSender;
        this.seenPeers = seenPeers;
        this.histogram = histogram;
        this.ttl = ttl;
    }

    public Host getOriginalSender() {
        return originalSender;
    }

    public Map<String, Boolean> getSeenPeers() {
        return seenPeers;
    }

    public Map<Integer, Integer> getHistogram() {
        return histogram;
    }

    public int getTtl() {
        return ttl;
    }

    public void decrementTtl() {
        this.ttl--;
    }

    @Override
    public String toString() {
        return "HistogramMessage{" +
                "originalSender="+originalSender +
                ", seenPeers=" + seenPeers +
                ", histogram=" + histogram +
                ", ttl=" + ttl +
                '}';
    }

    public static final ISerializer<HistogramMessage> serializer = new ISerializer<HistogramMessage>() {
        @Override
        public void serialize(HistogramMessage msg, ByteBuf out) throws IOException {
            Host.serializer.serialize(msg.originalSender, out);

            out.writeInt(msg.seenPeers.size());
            msg.seenPeers.forEach((s,b)-> {
                byte[] sB = s.getBytes(StandardCharsets.UTF_8);
                int lenSB = sB.length;
                out.writeInt(lenSB);
                out.writeBytes(sB);
                out.writeBoolean(b);
            });

            out.writeInt(msg.histogram.size());
            msg.histogram.forEach((i1,i2)-> {
                out.writeInt(i1);
                out.writeInt(i2);
            });

            out.writeInt(msg.ttl);
        }

        @Override
        public HistogramMessage deserialize(ByteBuf in) throws IOException {
            Host originalSender = Host.serializer.deserialize(in);

            int sizeSeenPeers = in.readInt();
            Map<String, Boolean> seenPeers = new HashMap<>(sizeSeenPeers, 1);
            for(int i = 0; i < sizeSeenPeers; i ++) {
                int sl = in.readInt();
                String s = in.readBytes(sl).toString(StandardCharsets.UTF_8);
                boolean b = in.readBoolean();
                seenPeers.put(s, b);
            }

            int sizeHistogram = in.readInt();
            Map<Integer, Integer> histogram = new HashMap<>(sizeHistogram, 1);
            for(int i = 0; i < sizeHistogram; i ++) {
                int i1 = in.readInt();
                int i2 = in.readInt();
                histogram.put(i1, i2);
            }

            int ttl = in.readInt();
            return new HistogramMessage(originalSender, seenPeers, histogram, ttl);
        }

    };
}
