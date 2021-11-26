package protocols.membership.hyparviewintegrated.utils;

import io.netty.buffer.ByteBuf;
import network.ISerializer;
import network.data.Host;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostWithResources extends Host {

    private final int resources;

    public HostWithResources(InetAddress address, int port, int resources) {
        super(address, port);
        this.resources = resources;
    }

    public int getResources() {
        return resources;
    }

    public static ISerializer<HostWithResources> serializer = new ISerializer<HostWithResources>() {
        public void serialize(HostWithResources host, ByteBuf out) {
            out.writeBytes(host.getAddress().getAddress());
            out.writeShort(host.getPort());
            out.writeInt(host.resources);
        }

        public HostWithResources deserialize(ByteBuf in) throws UnknownHostException {
            byte[] addrBytes = new byte[4];
            in.readBytes(addrBytes);
            int port = in.readShort() & '\uffff';
            int resources = in.readInt();
            return new HostWithResources(InetAddress.getByAddress(addrBytes), port, resources);
        }
    };
}
