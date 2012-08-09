package net.liveshift.p2p;

import java.util.Set;

import net.tomp2p.peers.PeerAddress;
import net.tomp2p.peers.PeerStatusListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.download.Tuner;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.util.ExecutorPool;


public class PeerStatusNotifier implements PeerStatusListener {
	
	final private static Logger logger = LoggerFactory.getLogger(PeerStatusNotifier.class);
	
	private final Set<PeerFailureNotifier> peerFailureNotifiers;
	
	public PeerStatusNotifier(final Set<PeerFailureNotifier> peerFailureNotifiers) {
		this.peerFailureNotifiers = peerFailureNotifiers;
	}		

	@Override
	public void peerOffline(final PeerAddress peerAddress, Reason reason) {
		
		if (reason!=Reason.NOT_REACHABLE) return;
		
		if (logger.isDebugEnabled()) logger.debug("peerOffline("+peerAddress+")");
		
		//peer failed, which could be normal up to a certain level, but if it fails too much, this peer should stop insisting
		final P2PAddress p2pAddress = new P2PAddress(peerAddress);
		
		Runnable offlineRunner= new Runnable() {
			@Override
			public void run() {

				try {
					if (logger.isDebugEnabled()) logger.debug("in the runner("+peerAddress+")");

					//signals from down/uploading stuff (if signaled enough times in a time period, will remove it)
					//signals from uploading stuff (if signaled enough times in a time period, will remove it)
					for (PeerFailureNotifier peerFailureNotifier : peerFailureNotifiers)
						peerFailureNotifier.signalFailure(p2pAddress);
					
					if (logger.isDebugEnabled()) logger.debug("out of the runner("+peerAddress+")");
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					logger.error("error in offlineRunner: "+e.getMessage());
					e.printStackTrace();
				}


			}
		};
		
		ExecutorPool.getGeneralExecutorService().execute(offlineRunner);
	}

	@Override
	public void peerOnline(PeerAddress peerAddress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void peerFail(PeerAddress arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

}
