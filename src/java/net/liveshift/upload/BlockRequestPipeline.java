package net.liveshift.upload;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import net.liveshift.signaling.MessageFlowControl;
import net.liveshift.signaling.MessagePipeline;
import net.liveshift.signaling.MessageFlowControl.FlowControlResponse;
import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.time.Clock;

public class BlockRequestPipeline extends MessagePipeline {

	public static final long	UNAVAILABLE_BLOCK_TIME_LIMIT_MILLIS	= 1000L;
	private UploadSlotManager uploadSlotManager;

	public BlockRequestPipeline(final MessageFlowControl flowControl) {
		super(flowControl);
	}
	
	public void registerUploadSlotManager(final UploadSlotManager uploadSlotManager) {
		this.uploadSlotManager = uploadSlotManager;
	}

	public BlockRequestMessage pollFirst(final Subscriber subscriber) throws InterruptedException
	{
		while (true)
		{
			synchronized (lock)
			{
				BlockRequestMessage result = pollFirst(subscriber, backingCollection);
				if (result != null) {
					return result;
				}
				else
					lock.wait(25);  //needs to check if waiting ones are done waiting somehow, that's why the timeout
			}
		}
	}
	
	private BlockRequestMessage pollFirst(final Subscriber subscriber, final Collection<AbstractMessage> backingCollection)
	{
		BlockRequestMessage first = first(subscriber, backingCollection);
		if (first != null)
		{
			boolean remove = backingCollection.remove(first);
			assert (remove);
		}
		return first;
	}
	
	private BlockRequestMessage first(final Subscriber subscriber, final Collection<AbstractMessage> backingCollection)
	{
		if (subscriber==null)
			return null;

		BlockRequestMessage first = null;
		Iterator<AbstractMessage> iter = backingCollection.iterator();
		
		long referenceTime = Clock.getMainClock().getTimeInMillis(false);
		
		// gets local data which was requested
		SegmentStorage segmentStorage = this.uploadSlotManager.getVideoSignaling().getSegmentStorage();

		while (iter.hasNext())
		{
			AbstractMessage message = iter.next();
			BlockRequestMessage blockRequestMessage = (BlockRequestMessage)message;
			
			switch (this.flowControl.acceptMessage(message, referenceTime)) {
				case REJECTED:
					message.setProcessed(); //sets processed to drop it immediately
					return blockRequestMessage;
				case WAIT:
					continue;
			}

			if (subscriber.getRequesterPeerId().equals(blockRequestMessage.getSender()) &&
					subscriber.getSegmentIdentifier().equals(blockRequestMessage.getSegmentIdentifier())) {				

				//checks if requested block is held locally
				Segment localSegment = segmentStorage.getSegment(blockRequestMessage.getSegmentIdentifier());
				boolean availableLocally;
				if (localSegment==null)
					availableLocally=false;
				else {
					SegmentBlockMap localSegmentBlockMap = localSegment.getSegmentBlockMap();
					availableLocally = (localSegment != null && localSegmentBlockMap != null && !localSegmentBlockMap.isEmpty() && localSegmentBlockMap.get(blockRequestMessage.getBlockNumber()));
				}
				
				if (!availableLocally) {
					//grants some time until it's rejected
					if (referenceTime-blockRequestMessage.getReceiveTimeMillis() > UNAVAILABLE_BLOCK_TIME_LIMIT_MILLIS)
						return blockRequestMessage;  //returns it so it can be handled properly (reply DONT_HAVE, unless it's lucky enough that it arrives in the meantime)
				}
				else {
					if (first == null || message.compareTo(first) < 0)
						first = blockRequestMessage;
				}
			}

		}

		return first;
	}

	/**
	 * removes all elements into collection
	 * @param collection
	 */
	public void drainTo(final Collection<BlockRequestMessage> collection, final Subscriber subscriber) {
		
		if (subscriber==null||subscriber.getRequesterPeerId()==null || subscriber.getSegmentIdentifier()==null)
			return;
		
		synchronized (lock)
		{
			Iterator<AbstractMessage> iter = backingCollection.iterator();
			long referenceTime = Clock.getMainClock().getTimeInMillis(false);
			
			while (iter.hasNext()) {
				AbstractMessage message = iter.next();
				BlockRequestMessage blockRequestMessage = (BlockRequestMessage)message;

				if (blockRequestMessage!=null && 
						this.flowControl.acceptMessage(message, referenceTime)==FlowControlResponse.ACCEPTED &&
						subscriber.getRequesterPeerId().equals(blockRequestMessage.getSender()) &&
						subscriber.getSegmentIdentifier().equals(blockRequestMessage.getSegmentIdentifier())) {

					collection.add(blockRequestMessage);
					iter.remove();
				}
			}
		}
	}
	
	/**
	 * removes all elements into collection and returns the collection (as a hashset)
	 */
	public Collection<BlockRequestMessage> getAll(final Subscriber subscriber) {
		Collection<BlockRequestMessage> collection = new HashSet<BlockRequestMessage>();
		this.drainTo(collection, subscriber);
		return collection;
	}
	
	public Collection<BlockRequestMessage> getAllUngranted() {
		
		Collection<BlockRequestMessage> out = new HashSet<BlockRequestMessage>();
		long referenceTime = Clock.getMainClock().getTimeInMillis(false);
		
		synchronized (lock)
		{
			Iterator<AbstractMessage> iter = backingCollection.iterator();
			
			while (iter.hasNext()) {
				AbstractMessage message = iter.next();
				BlockRequestMessage blockRequestMessage = (BlockRequestMessage)message;
				
				if (blockRequestMessage!=null && this.flowControl.acceptMessage(message, referenceTime)!=FlowControlResponse.WAIT && null==this.uploadSlotManager.getGrantedUploadSlotRequest(blockRequestMessage.getSender(), blockRequestMessage.getSegmentIdentifier())) {
					out.add(blockRequestMessage);
					iter.remove();
				}
			}
		}
		
		return out;
	}
}
