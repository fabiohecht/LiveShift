package net.liveshift.signaling.messaging;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;



public class PeerSuggestionMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	final private Set<PeerId> suggestedPeerIds;

	public PeerSuggestionMessage(final SegmentIdentifier segmentIdentifier, final Set<PeerId> suggestedPeerIds, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.segmentIdentifier = segmentIdentifier;
		this.suggestedPeerIds = suggestedPeerIds;
	}
	
	public PeerSuggestionMessage(byte messageId, PeerId sender, byte[] byteArray, Map<Integer, Channel> channelCatalog, int offset) throws UnknownHostException {
		super(messageId, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.suggestedPeerIds = new HashSet<PeerId>();
		offset += 17;
		int numSuggestedPeerIds = byteArray[offset++];
		for (int i=0; i<numSuggestedPeerIds ; i++) {
			this.suggestedPeerIds.add(new PeerId(byteArray, offset, offset+20));
			offset += 20 + byteArray[offset+20] + 1 + byteArray[offset+20+1+byteArray[offset+20]] + 1;
		}
	}

	@Override
	public byte[] toByteArray() {

		int size = 0;
		for (PeerId peerId : this.suggestedPeerIds) {
			size += 20+1+1;
			size += peerId.getName().getBytes().length;
			size += peerId.getDhtId().getSocketAddressByteArraySize();
		}
		
		byte[] out = new byte[6+14+size];
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'S';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		out[offset++] = (byte)this.suggestedPeerIds.size();

		for (PeerId peerId : this.suggestedPeerIds) {
			offset = peerId.getDhtId().getIdAsByteArray(out, offset);
			out[offset++] = (byte) peerId.getDhtId().getSocketAddressByteArraySize();
			offset = peerId.getDhtId().getSocketAddressAsByteArray(out, offset);
			byte[] nameAsByteArray = peerId.getName().getBytes();
			out[offset++] = (byte) nameAsByteArray.length;
			System.arraycopy(nameAsByteArray, 0, out, offset, nameAsByteArray.length);
			offset += nameAsByteArray.length;
		}
		return out;
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	public Set<PeerId> getSuggestedPeerIds() {
		return this.suggestedPeerIds;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " si:"+this.segmentIdentifier+ " suggests:"+this.suggestedPeerIds;
		
		return out;
	}
}

