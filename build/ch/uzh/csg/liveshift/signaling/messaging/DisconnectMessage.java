package net.liveshift.signaling.messaging;

import java.util.Map;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.Utils;

public class DisconnectMessage extends AbstractMessage {

	final private SegmentIdentifier segmentIdentifier;
	
	//the peer receiving this message should stop uploading to the peer sending this message
	final private boolean stopUploading;
	
	//the peer receiving this message should stop downloading to the peer sending this message
	final private boolean stopDownloading;

	public DisconnectMessage(final SegmentIdentifier segmentIdentifier, final boolean stopUploading, final boolean stopDownloading, final PeerId myPeerId, final PeerId receiver) {
		super(myPeerId, receiver);
		
		this.segmentIdentifier = segmentIdentifier;
		this.stopUploading = stopUploading;
		this.stopDownloading = stopDownloading;
	}

	public DisconnectMessage(final byte messageId, PeerId sender, final byte[] byteArray, final Map<Integer,Channel> channelCatalog, int offset) {
		super(messageId, sender);
		offset+=2;
		this.segmentIdentifier = new SegmentIdentifier(channelCatalog.get(Utils.byteArrayToInteger(byteArray, offset+4)), byteArray[offset+8], Utils.byteArrayToLong(byteArray, offset+9));
		this.stopUploading = (byteArray[offset+17] & 0x01) > 0;
		this.stopDownloading = (byteArray[offset+17] & 0x2) > 0;
	}
	
	@Override
	public byte[] toByteArray() {
		byte[] out = new byte[20];
		
		int offset = super.toByteArray(out, 0);
		out[offset++] = 'D';
		offset = this.segmentIdentifier.toByteArray(out, offset);
		out[offset] = (byte) (this.stopUploading?1:0);
		out[offset] += (byte) (this.stopDownloading?2:0);
			
		return out;
	}

	public SegmentIdentifier getSegmentIdentifier() {
		return this.segmentIdentifier;
	}
	
	@Override
	public String toString() {
		String out = super.toString() + " si:"+this.segmentIdentifier+", SU="+this.stopUploading+", SD="+this.stopDownloading;
		
		return out;
	}

	public boolean stopUploading() {
		return this.stopUploading;
	}
	public boolean stopDownloading() {
		return this.stopDownloading;
	}
}
