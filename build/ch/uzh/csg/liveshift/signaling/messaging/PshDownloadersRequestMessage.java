package net.liveshift.signaling.messaging;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.liveshift.core.PeerId;
import net.liveshift.util.Utils;


public class PshDownloadersRequestMessage extends AbstractMessage {
	
	private final Set<PeerId> peerIds;

	public PshDownloadersRequestMessage(final PeerId myPeerId, final PeerId receiver, final Set<PeerId> peerIds) {
		super(myPeerId, receiver);
		
		this.peerIds = peerIds;
	}
	
	public PshDownloadersRequestMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<BigInteger, PeerId> peerIdCatalog, int offset) {
		super(messageId, sender);
		
		int numPeerIds = Utils.byteArrayToInteger(byteArray, 24);
		
		offset = 28;
		this.peerIds = new HashSet<PeerId>();
		for (int i = 0; i < numPeerIds; i++) {
			byte[] peerDhtIt  = new byte[20];
			System.arraycopy(byteArray, offset+20*i, peerDhtIt, 0, 20);
			this.peerIds.add(peerIdCatalog.get(new BigInteger(peerDhtIt)));
		}
	}

	@Override
	public byte[] toByteArray() {
		int numPeerIds = this.peerIds.size();
		byte[] out = new byte[28+numPeerIds*20];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'J';
		
		offset = Utils.integerToByteArray(numPeerIds, out, offset);
		for (PeerId peerId : this.peerIds) {
			offset = peerId.getDhtId().getIdAsByteArray(out, offset);
		}

		return out;
	}
	
	public Set<PeerId> getPeerIds() {
		return this.peerIds;
	}
}
