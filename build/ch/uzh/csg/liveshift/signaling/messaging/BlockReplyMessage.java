package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;
import net.liveshift.video.PacketData;

public class BlockReplyMessage extends AbstractMessage {

	final private SegmentBlock segmentBlock;
	final private BlockReplyCode blockReplyCode; 
	
	public enum BlockReplyCode {GRANTED, DONT_HAVE, NO_SLOT, REJECTED}


	public BlockReplyMessage(BlockReplyCode blockReplyCode, SegmentBlock segmentBlock, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.blockReplyCode = blockReplyCode;
		this.segmentBlock = segmentBlock;
	}

	public BlockReplyMessage(final byte messageId, Byte messageIdReply, PeerId sender, final byte[] byteArray, final Map<Integer, Channel> channelCatalog, int offset) {
		super(messageId, messageIdReply, sender);
		offset+=6;
		this.blockReplyCode = byteArray[offset]==1?BlockReplyCode.GRANTED:byteArray[offset]==2?BlockReplyCode.DONT_HAVE:byteArray[offset]==3?BlockReplyCode.NO_SLOT:BlockReplyCode.REJECTED;
		offset++;
		SegmentIdentifier segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset)), byteArray[offset+4], Utils.byteArrayToLong(byteArray, offset+5));
		offset+=13;
		int blockNumber = Utils.byteArrayToInteger(byteArray, offset);
		offset+=4;
		byte hopCount = byteArray[offset++];
		
		int numPackets = Utils.byteArrayToInteger(byteArray, offset);
		offset+=4;
		PacketData[] packets = new PacketData[numPackets];
		for (int i=0; i<numPackets; i++) {
			long timeMillis = Utils.byteArrayToLong(byteArray, offset);
			offset+=8;
			long sequenceNo = Utils.byteArrayToLong(byteArray, offset);
			offset+=8;
			byte substream = byteArray[offset++];
			int videoDataLength = Utils.byteArrayToInteger(byteArray, offset);
			offset+=4;
			byte[] videoData = new byte[videoDataLength];
			System.arraycopy(byteArray, offset, videoData, 0, videoDataLength);
			offset+=videoDataLength;
			
			packets[i] = new PacketData(videoData, timeMillis, sequenceNo, substream);
		}
		
		this.segmentBlock = new SegmentBlock(segmentIdentifier, blockNumber, packets, hopCount);
	}
	
	@Override
	public byte[] toByteArray() {

		PacketData[] packets = this.segmentBlock.getPackets();
		int totalSize = 29;
		for (PacketData packet : packets) {
			totalSize += 21+packet.getVideoData().length;
		}
		
		byte[] out = new byte[totalSize];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'C';
		out[offset++] = (byte)(this.blockReplyCode==BlockReplyCode.GRANTED?1:this.blockReplyCode==BlockReplyCode.DONT_HAVE?2:this.blockReplyCode==BlockReplyCode.NO_SLOT?3:4);
		offset = this.segmentBlock.getSegmentIdentifier().toByteArray(out, offset);
		offset = Utils.integerToByteArray(this.segmentBlock.getBlockNumber(), out, offset);
		out[offset++] = this.segmentBlock.getHopCount();
		offset = Utils.integerToByteArray(packets.length, out, offset);
		for (PacketData packet : packets) {
			offset = Utils.longToByteArray(packet.getTimeMS(), out, offset);
			offset = Utils.longToByteArray(packet.getSequence(), out, offset);
			out[offset++] = packet.getSubstream();
			
			byte[] videoData = packet.getVideoData();
			offset = Utils.integerToByteArray(videoData.length, out, offset);
			
			System.arraycopy(videoData, 0, out, offset, videoData.length);
			offset+=videoData.length;
		}
		
		return out;
	}

	public BlockReplyCode getReplyCode() {
		return this.blockReplyCode;
	}
	public SegmentBlock getSegmentBlock() {
		return this.segmentBlock;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " brc:"+blockReplyCode+" sb:"+this.segmentBlock;
		
		return out;
	}
}
