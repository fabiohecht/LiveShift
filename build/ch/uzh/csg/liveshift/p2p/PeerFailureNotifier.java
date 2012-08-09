package net.liveshift.p2p;

public interface PeerFailureNotifier {
	public void signalFailure(P2PAddress p2pAddress);
}
