package protocols.membership.hyparviewintegrated.utils;

import com.sun.tools.javac.util.Pair;
import network.data.Host;

import java.util.Set;

public interface IView {

    public void setCapacity(int newCapacity);

    public void setOther(IView other, Set<Host> pending);

    public Host addPeer(Host peer);

    public boolean removePeer(Host peer);

    public boolean containsPeer(Host peer);

    public Host dropRandom();

    public Set<Host> getRandomSample(int sampleSize);

    public Set<Host> getPeers();

    public Host getRandom();

    public Host getRandomDiff(Host from);

    public boolean fullWithPending(Set<Host> pending);

    public boolean isFull();

    public void addHostResource(Host h, int resource);

    public Pair<Host, Integer> dropLeastPowerful();

    public Pair<Host, Integer> dropMostPowerful();
}
