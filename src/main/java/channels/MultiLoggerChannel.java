package channels;

import babel.channels.multi.MultiChannel;
import babel.generic.ProtoMessage;
import channel.ChannelListener;
import channel.base.SingleThreadedBiChannel;
import network.AttributeValidator;
import network.Connection;
import network.ISerializer;
import network.data.Attributes;
import network.data.Host;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jol.info.ClassLayout;
import utils.Translate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MultiLoggerChannel extends SingleThreadedBiChannel<ProtoMessage, ProtoMessage> implements AttributeValidator {

    private static final Logger logger = LogManager.getLogger(MultiLoggerChannel.class);

    private static MultiLoggerChannel multiChannelInstance = null;
    private static MultiChannel multiChannel;

    long totalbytes = 0;

    private long numberOfSentMessagesOverlays;
    private long numberOfReceivedMessagesOverlays;
    private long numberOfSentBytesOverlays;
    private long numberOfReceivedBytesOverlays;

    private long numberOfSentMessagesFlood;
    private long numberOfReceivedMessagesFlood;

    private Map<Connection<ProtoMessage>, Long> bytes;

    private String msgName(ProtoMessage msg) {
        return msg.getClass().getSimpleName();
    }

    public static MultiLoggerChannel getInstance(ISerializer<ProtoMessage> serializer,
                                                 ChannelListener<ProtoMessage> list,
                                                 short protoId,
                                                 Properties properties)  throws IOException {
        if(multiChannelInstance == null)
            multiChannelInstance = new MultiLoggerChannel(serializer, list, protoId, properties);

        multiChannel = MultiChannel.getInstance(serializer, list, protoId, properties, multiChannelInstance);
        return multiChannelInstance;
    }


    private MultiLoggerChannel(ISerializer<ProtoMessage> serializer,
                               ChannelListener<ProtoMessage> list,
                               short protoId,
                               Properties properties) throws IOException {
        super("MultiLoggerChannel");

        bytes = new HashMap<>();
        numberOfSentMessagesOverlays = 0;
        numberOfReceivedMessagesOverlays = 0;
        numberOfSentBytesOverlays = 0;
        numberOfReceivedBytesOverlays = 0;
        numberOfSentMessagesFlood = 0;
        numberOfReceivedMessagesFlood = 0;
    }

    @Override
    protected void onInboundConnectionUp(Connection<ProtoMessage> con) {
        multiChannel.inboundConnectionUp(con);
    }

    @Override
    protected void onInboundConnectionDown(Connection<ProtoMessage> con, Throwable cause) {
        multiChannel.inboundConnectionDown(con, cause);
        bytes.remove(con);
    }

    @Override
    protected void onServerSocketBind(boolean success, Throwable cause) {
        if (!success)
            logger.error("Server socket bind failed: " + cause);
    }

    @Override
    protected void onServerSocketClose(boolean success, Throwable cause) {
        multiChannel.serverSocketClose(success, cause);
    }

    @Override
    protected void onOutboundConnectionUp(Connection<ProtoMessage> conn) {
        multiChannel.outboundConnectionUp(conn);
    }

    @Override
    protected void onOutboundConnectionDown(Connection<ProtoMessage> conn, Throwable cause) {
        multiChannel.outboundConnectionDown(conn, cause);
    }

    @Override
    protected void onOutboundConnectionFailed(Connection<ProtoMessage> conn, Throwable cause) {
        multiChannel.outboundConnectionFailed(conn, cause);
    }

    @Override
    protected void onSendMessage(ProtoMessage msg, Host peer, int connection) {
        if(!msgName(msg).equals("IHaveMessage")) {
            logger.debug("Sending msg {} for Proto {} to {}", msgName(msg), Translate.ProtoIdToName(msg.getSourceProto()), peer);
            if(msg.getSourceProto() == 21000) {
                numberOfSentMessagesFlood++;
                if(numberOfSentMessagesFlood % 1000 == 0) {
                    logger.info("Number of messages sent by flood: {}", numberOfSentMessagesFlood);
                }
            } else {
                numberOfSentMessagesOverlays++;
                try {
                    numberOfSentBytesOverlays += ClassLayout.parseInstance(msg).instanceSize();
                } catch (Exception e) {
                    numberOfSentBytesOverlays = -1;
                }
                if(numberOfSentMessagesOverlays % 100 == 0) {
                    logger.info("Number of messages sent by overlays: {}", numberOfSentMessagesOverlays);
                    logger.info("Number of bytes sent by overlays: {}", numberOfSentBytesOverlays);
                }
            }
        }
        multiChannel.sendMessage(msg, peer, connection);
    }

    @Override
    protected void onCloseConnection(Host peer, int connection) {
        multiChannel.closeConnection(peer, connection);
    }

    @Override
    protected void onDeliverMessage(ProtoMessage msg, Connection<ProtoMessage> conn) {
        if(!msgName(msg).equals("IHaveMessage")) {
            logger.debug("Received msg {} for Proto {} from {} bytes {}", msgName(msg), Translate.ProtoIdToName(msg.getSourceProto()), conn.getPeer(),  conn.getReceivedAppBytes() - bytes.getOrDefault(conn, new Long(0)));
            if(msg.getSourceProto() == 21000) {
                numberOfReceivedMessagesFlood++;
                if(numberOfReceivedMessagesFlood % 1000 == 0) {
                    logger.info("Number of messages received by flood: {}", numberOfReceivedMessagesFlood);
                }
            } else {
                numberOfReceivedMessagesOverlays++;
                numberOfReceivedBytesOverlays += (conn.getReceivedAppBytes() - bytes.getOrDefault(conn, 0L));
                if(numberOfReceivedMessagesOverlays % 100 == 0) {
                    logger.info("Number of messages received by overlays: {}", numberOfReceivedMessagesOverlays);
                    logger.info("Number of bytes received by overlays: {}", numberOfReceivedBytesOverlays);
                }
            }
        }
        bytes.put(conn, conn.getReceivedAppBytes());
        multiChannel.deliverMessage(msg, conn);
    }

    @Override
    protected void onOpenConnection(Host peer) {
        multiChannel.openConnection(peer);
    }

    @Override
    public boolean validateAttributes(Attributes attr) {
        return multiChannel.validateAttributes(attr);
    }
}