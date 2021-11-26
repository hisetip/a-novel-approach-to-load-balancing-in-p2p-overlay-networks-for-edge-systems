package protocols.optimization.resest.messages;

import babel.generic.ProtoMessage;
import io.netty.buffer.ByteBuf;
import network.ISerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HistogramAnswerMessage extends ProtoMessage {
    public final static short MSG_CODE = 23102;

    private final Map<Integer, Integer> histogram;
    private final float mean;

    public HistogramAnswerMessage(Map<Integer, Integer> histogram, float mean) {
        super(MSG_CODE);
        this.histogram = histogram;
        this.mean = mean;
    }

    public Map<Integer, Integer> getHistogram() {
        return histogram;
    }

    public float getMean() {
        return mean;
    }

    @Override
    public String toString() {
        return "HistogramAnswerMessage{" +
                "histogram=" + histogram +
                ", mean=" + mean +
                '}';
    }

    public static final ISerializer<HistogramAnswerMessage> serializer = new ISerializer<HistogramAnswerMessage>() {
        @Override
        public void serialize(HistogramAnswerMessage msg, ByteBuf out) throws IOException {
            out.writeInt(msg.histogram.size());
            msg.histogram.forEach((i1,i2)-> {
                out.writeInt(i1);
                out.writeInt(i2);
            });

            out.writeFloat(msg.mean);
        }

        @Override
        public HistogramAnswerMessage deserialize(ByteBuf in) throws IOException {
            int sizeHistogram = in.readInt();
            Map<Integer, Integer> histogram = new HashMap<>(sizeHistogram, 1);
            for(int i = 0; i < sizeHistogram; i ++) {
                int i1 = in.readInt();
                int i2 = in.readInt();
                histogram.put(i1, i2);
            }

            float mean = in.readFloat();

            return new HistogramAnswerMessage(histogram, mean);
        }

    };
}
