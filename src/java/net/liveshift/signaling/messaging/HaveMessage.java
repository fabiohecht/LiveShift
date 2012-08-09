package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;



public class HaveMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	final private int blockNumber;
	final boolean doHave; //TODO: this to false means NOT HAVE ANYMORE -- possible if deleted from storage
	final int rate;

	public HaveMessage(SegmentIdentifier segmentIdentifier, int blockNumber, final boolean doHave, int rate, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.segmentIdentifier = segmentIdentifier;
		this.blockNumber = blockNumber;
		this.doHave = doHave;
		this.rate = rate;
	}
	
	public HaveMessage(byte messageId, PeerId sender, byte[] byteArray, Map<Integer,Channel> channelCatalog, int offset) {
		super(messageId, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.blockNumber = Utils.byteArrayToInteger(byteArray, offset+17);
		this.doHave = byteArray[offset+21]==1;
		this.rate = Utils.byteArrayToInteger(byteArray, offset+22);
	}
	
	@Override
	public byte[] toByteArray() {
		
		byte[] out = new byte[28];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'H';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		offset = Utils.integerToByteArray(this.blockNumber, out, offset);
		out[offset++] = (byte) (this.doHave?1:0);
		offset = Utils.integerToByteArray(this.rate, out, offset);
		
		return out;
	}
	
	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	public int getBlockNumber() {
		return this.blockNumber;
	}
	
	public boolean doHave() {
		return this.doHave;
	}
	
	public int getRate() {
		return this.rate;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " si:"+this.segmentIdentifier+ " b#"+this.blockNumber+" H=" +this.doHave+ " m="+this.rate;
		
		return out;
	}
}
