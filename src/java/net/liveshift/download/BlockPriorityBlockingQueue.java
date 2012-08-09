package net.liveshift.download;

import java.util.Collection;

import net.liveshift.configuration.Configuration;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.SuperPriorityBlockingQueue;



public class BlockPriorityBlockingQueue<E extends BlockRequest> extends SuperPriorityBlockingQueue<BlockRequest> {

	private static final long	RECHECK_MAX_FREQUENCY_MS = (long) (Configuration.SEGMENTBLOCK_SIZE_MS * .5);  //because the situation may change without notifying the wait down there

	public BlockPriorityBlockingQueue() {
		super();
	}

	public BlockRequest take(Neighbor neighbor) throws InterruptedException
	{
		if (neighbor==null)
			return null;
		
		while (true)
		{
			synchronized (super.lock)
			{
				BlockRequest result = pollFirst(super.backingCollection, neighbor);
				if (result != null)
					return result;
				else
					super.lock.wait(RECHECK_MAX_FREQUENCY_MS);
			}
		}
	}
	private static <E extends BlockRequest> E pollFirst(Collection<E> backingList, Neighbor neighbor)
	
	{
		if (neighbor.getSegmentIdentifier()==null || neighbor.getBlockMap()==null)
			return null;
		
		E first = null;
		for (E e : backingList)
		{
			if (neighbor.getSegmentIdentifier().equals(e.getSegmentIdentifier()) && neighbor.getBlockMap().get(e.getBlockNumber()) && (first == null || e.compareTo(first) < 0))
				first = e;
		}
		if (first != null)
		{
			boolean remove = backingList.remove(first);
			assert (remove);
		}
		return first;
	}


	@Override
	public String toString() {
		return super.toString();
	}

	public void notifyLock() {
		synchronized (super.lock) {
			super.lock.notifyAll();
		}
	}

	public boolean hasInterestingBlocks(Neighbor neighbor) {
		for (BlockRequest blockRequest : super.snapshot()) {
			
			SegmentIdentifier segmentIdentifier1 = neighbor.getSegmentIdentifier();
			SegmentIdentifier segmentIdentifier2 = blockRequest.getSegmentIdentifier();
			ProbabilisticSegmentBlockMap neighborBlockMap = neighbor.getBlockMap();
			
			if (segmentIdentifier1==null || segmentIdentifier2==null || neighborBlockMap==null) {
				return false;
			}
			
			if (segmentIdentifier1.equals(segmentIdentifier2) && neighborBlockMap.get(blockRequest.getBlockNumber()))
				return true;
		}
		return false;
	}

	public boolean isEmpty() {
		return this.backingCollection.isEmpty();
	}
}
