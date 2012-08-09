package net.liveshift.p2p;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import net.liveshift.core.Channel;
import net.liveshift.core.DHTConnectionException;
import net.liveshift.core.PeerId;
import net.liveshift.download.Tuner;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.storage.SegmentIdentifier;
import net.tomp2p.futures.FutureTracker;

/**
 *
 * @author Fabio Victora Hecht, draft
 */
public interface DHTInterface
{	
	boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName, InetAddress publicAddress, int publicPorts, boolean doUpnp) throws DHTConnectionException;
	boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName, boolean doUpnp) throws DHTConnectionException;
	
	public Set<Channel> getChannelSet() throws IOException, ClassNotFoundException;

	public boolean publishChannel(final Channel channel) throws IOException;
	public void unPublishChannel(final Channel channel);
	
	public FutureTracker publishSegment(final SegmentIdentifier segmentIdentifier) throws IOException;
	//public FutureTracker unPublishSegment(final SegmentIdentifier segmentIdentifier) throws IOException;
	
	public FutureTracker getPeerList(final SegmentIdentifier segmentIdentifier, final int howMany) throws IOException, ClassNotFoundException;

	//public boolean publishPeerId();
	//public boolean getPeerId(PeerId peerId);
	public PeerId getMyId();

	void addPeerOfflineListener(final Tuner tuner, final VideoSignaling videoSignaling);
	
	public boolean isConnected();
	public void disconnect() throws InterruptedException;

}
