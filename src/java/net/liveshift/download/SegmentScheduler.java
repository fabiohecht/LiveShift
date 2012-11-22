package net.liveshift.download;

import net.liveshift.core.PeerId;
import net.liveshift.storage.SegmentIdentifier;

public interface SegmentScheduler {
	void scheduleNextSegment(SegmentIdentifier segmentIdentifier);

	public abstract void maybeScheduleBlock(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final int blockNumber, final boolean doHave);
}
