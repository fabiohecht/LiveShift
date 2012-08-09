package net.liveshift.signaling.messaging;

import java.math.BigInteger;
import java.util.Map;

import net.liveshift.core.PeerId;


public class PshCheckRequestMessage extends AbstractMessage {

	final PeerId intermediate;
	final PeerId target;

	public PshCheckRequestMessage(final PeerId intermediate, final PeerId target, final PeerId myPeerId) {
		super(myPeerId, intermediate);
		
		this.intermediate = intermediate;
		this.target = target;
	}
	
	public PshCheckRequestMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<BigInteger, PeerId> peerCatalog, int offset) {
		super(messageId, sender);
		
		byte[] intermediateDhtIt  = new byte[20];
		System.arraycopy(byteArray, offset+24, intermediateDhtIt, 0, 20);
		this.intermediate = peerCatalog.get(new BigInteger(intermediateDhtIt));

		byte[] targetDhtIt  = new byte[20];
		System.arraycopy(byteArray, offset+44, targetDhtIt, 0, 20);
		this.target = peerCatalog.get(new BigInteger(targetDhtIt));
	}

	@Override
	public byte[] toByteArray() {
		byte[] out = new byte[64];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'L';
		
		offset = this.intermediate.getDhtId().getIdAsByteArray(out, offset);
		offset = this.target.getDhtId().getIdAsByteArray(out, offset);
		
		return out;
	}


	public PeerId getIntermediate() {
		return this.intermediate;
	}
	public PeerId getTarget() {
		return this.target;
	}

	@Override
	public String toString() {
		String out = super.toString() + " int:"+this.intermediate+ " tar:"+this.target;
		
		return out;
	}
}
