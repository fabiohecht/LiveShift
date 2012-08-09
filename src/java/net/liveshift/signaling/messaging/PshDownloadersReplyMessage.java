package net.liveshift.signaling.messaging;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.liveshift.core.PeerId;
import net.liveshift.util.Utils;



public class PshDownloadersReplyMessage extends AbstractMessage {

//	final private byte[] bloomFilterDownloaders;
	private final Map<PeerId, Float> peerIds;

	public PshDownloadersReplyMessage(final Map<PeerId, Float> peerIds, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.peerIds = peerIds;
	}
	
	public PshDownloadersReplyMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<BigInteger, PeerId> peerIdCatalog, int offset) {
		super(messageId, sender);

		int numPeerIds = Utils.byteArrayToInteger(byteArray, 24);
		
		offset = 28;
		this.peerIds = new HashMap<PeerId, Float>();
		for (int i = 0; i < numPeerIds; i++) {
			byte[] peerDhtIt  = new byte[20];
			System.arraycopy(byteArray, offset, peerDhtIt, 0, 20);
			offset+=20;
			this.peerIds.put(peerIdCatalog.get(new BigInteger(peerDhtIt)), Utils.byteArrayToFloat(byteArray, offset));
			offset+=4;
		}
		
	}

	@Override
	public byte[] toByteArray() {
		int peerIdsSize = this.peerIds.size();
		byte[] out = new byte[28+peerIdsSize*24];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'K';
		
		offset = Utils.integerToByteArray(peerIdsSize, out, offset);
		for (Entry<PeerId, Float> entry : this.peerIds.entrySet()) {
			offset = entry.getKey().getDhtId().getIdAsByteArray(out, offset);
			offset = Utils.floatToByteArray(entry.getValue(), out, offset);
		}
		
		return out;
	}

	
	public Map<PeerId, Float> getPeerIds() {
		return this.peerIds;
	}

}
