package net.liveshift.dummy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import net.liveshift.core.Channel;
import net.liveshift.core.DHTConnectionException;
import net.liveshift.core.PeerId;
import net.liveshift.download.Tuner;
import net.liveshift.p2p.DHTInterface;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.storage.SegmentIdentifier;
import net.tomp2p.futures.FutureTracker;



public class DummyDht implements DHTInterface {

	private PeerId	myId;

	public DummyDht(PeerId	myId) {
		this.myId=myId;
	}
	
	@Override
	public void disconnect() throws InterruptedException {
		// TODO Auto-generated method stub

	}

	
	@Override
	public PeerId getMyId() {
		return this.myId ;
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publishChannel(Channel channel) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unPublishChannel(Channel channel) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addPeerOfflineListener(Tuner tuner, VideoSignaling videoSignaling) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<Channel> getChannelSet() throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FutureTracker getPeerList(SegmentIdentifier segmentIdentifier, int howMany) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FutureTracker publishSegment(SegmentIdentifier segmentIdentifier) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName, InetAddress publicAddress,
			int publicPorts, boolean doUpnp) throws DHTConnectionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(InetSocketAddress localSocket, InetSocketAddress bootstrapSocket, String iface, String peerName, boolean doUpnp)
			throws DHTConnectionException {
		// TODO Auto-generated method stub
		return false;
	}

}
