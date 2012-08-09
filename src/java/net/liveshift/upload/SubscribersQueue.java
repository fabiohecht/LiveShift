package net.liveshift.upload;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.SuperPriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class SubscribersQueue extends SuperPriorityBlockingQueue<Subscriber> {

	final private static Logger logger = LoggerFactory.getLogger(SubscribersQueue.class);
	
	private final Map<Subscriber, ScheduledFuture<?>> scheduledTimeoutFutures;
	//private final Map<UploadSlotRequest, ScheduledFuture<?>> scheduledPingFutures;

	private final Set<Subscriber> takenElements;  //requests already granted (not in Q anymore but some logic also applies to it)

	public SubscribersQueue(final int uploadQueueSize, final boolean uploadQueuePreempt, final VideoSignaling videoSignaling) {
		super(uploadQueueSize, uploadQueuePreempt);
		this.scheduledTimeoutFutures = new HashMap<Subscriber, ScheduledFuture<?>>();
		//this.scheduledPingFutures = new HashMap<UploadSlotRequest, ScheduledFuture<?>>();
		this.takenElements = new HashSet<Subscriber>();
		
		super.addPreemptedElementsListener(new PreemptedElementsListener<Subscriber>() {
			
			@Override
			public void elementRemoved(Subscriber subscriber) {
				
				logger.debug("preempting subscriber "+subscriber);
				
				videoSignaling.sendSubscribed(subscriber.getSegmentIdentifier(), null, 5000, subscriber.getRequesterPeerId(), true);

				cleanupAfterRemove(subscriber);
			}
		});
	}
	
	@Override
	protected boolean preemptAndOffer(Subscriber e) {

		//if the peer has a place in the queue, it may add another one
		for (Subscriber subscriber : this.backingCollection)
			if (e.getRequesterPeerId().equals(subscriber.getRequesterPeerId())) {
				backingCollection.add(e);
				lock.notifyAll();
				
				return true;
			}
		
		//also if peer already has slot, it may add a new request without preempying any
		for (Subscriber subscriber : this.takenElements)
			if (e.getRequesterPeerId().equals(subscriber.getRequesterPeerId())) {
				backingCollection.add(e);
				lock.notifyAll();
				
				return true;
			}
		
		// check for lower-priority entries
		SortedSet<Subscriber> sortedSet = new TreeSet<Subscriber>(this.backingCollection);
		for (Subscriber subscriber : sortedSet.headSet(e)) {
			//e is less than uploadSlotRequest
			if (!this.takenElements.contains(e)) {
				//not granted, preempt it!
				this.preempt(subscriber);

				backingCollection.add(e);
				lock.notifyAll();
				
				return true;
			}
		}
		
		//sorry, no element to preempt and not added 
		return false;
	}
	
	@Override
	public boolean offer(final Subscriber subscriber) {
		if (super.offer(subscriber)) {
			
			//schedules its timeout and pings
			this.scheduleTimeout(subscriber);
			//this.schedulePings(uploadSlotRequest);
			return true;
		}
		else
			return false;
	}

	public Subscriber take() throws InterruptedException {
		
		Subscriber subscriber = null;
		
		while (subscriber==null)			
			subscriber = this.pollLast();
		
		//unschedules its timeout
		this.unscheduleFutures(subscriber);
		
		return subscriber;
	}
	
	@Override
	public Subscriber pollLast() throws InterruptedException
	{
		while (true)
		{
			synchronized (lock)
			{
				
				Subscriber last = null;
				for (Subscriber e : super.backingCollection)
				{
					if ((last == null || e.compareTo(last) > 0) && e.mayBeGranted())
						last = e;
				}
				
				if (last != null)
				{
					this.takenElements.add(last);
					boolean remove = super.backingCollection.remove(last);
					assert (remove);
					
					if (last.setPeerBlocked())					
						return last;
				}
				lock.wait(200);
			}
		}
	}

	@Override
	public boolean remove(final Subscriber subscriber) {

		boolean out = super.remove(subscriber);
		this.cleanupAfterRemove(subscriber);
		return out;
	}
	
	private void cleanupAfterRemove(final Subscriber subscriber) {
		this.takenElements.remove(subscriber);
		this.unscheduleFutures(subscriber);
	}
	
	void removeFromTakenElements(final Subscriber subscriber) {
		synchronized (lock)
		{
			this.takenElements.remove(subscriber);
		}
	}
	
	private void scheduleTimeout(final Subscriber subscriber) {
		
		if (logger.isDebugEnabled()) logger.debug("scheduling timeout of " + subscriber);
		
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) logger.debug(subscriber + " has timed out!!");
					
					remove(subscriber);
					
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception
					// happened
					e.printStackTrace();
				}
			}
		};
		
		long timeoutTimeMillis = subscriber.getTimeToTimeoutMillis();
		try {
			ScheduledFuture<?> scheduledTimeoutFuture = ExecutorPool.getScheduledExecutorService().schedule(runner, timeoutTimeMillis, TimeUnit.MILLISECONDS);
			this.scheduledTimeoutFutures.put(subscriber, scheduledTimeoutFuture);
		}
		catch (RejectedExecutionException e) {
			logger.error("error scheduling timeout task: "+e.getMessage());
			e.printStackTrace();
		}
	}
/*
	private void schedulePings(final UploadSlotRequest uploadSlotRequest) {
		
		if (logger.isDebugEnabled()) logger.debug("scheduling pings for " + uploadSlotRequest);

		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) logger.debug("pinging " + uploadSlotRequest);
					
					videoSignaling.sendPing(uploadSlotRequest.getRequesterPeerId());
					
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception
					// happened
					e.printStackTrace();
				}
			}
		};
		
		try {
			ScheduledFuture<?> scheduledPingFuture = ExecutorPool.getScheduledExecutorService().schedule(runner, 5, TimeUnit.SECONDS);
			this.scheduledPingFutures.put(uploadSlotRequest, scheduledPingFuture);
		}
		catch (RejectedExecutionException e) {
			logger.error("error scheduling timeout task: "+e.getMessage());
			e.printStackTrace();
		}
	}
*/
	private void unscheduleFutures(final Subscriber subscriber) {
		//unschedules timeout
		ScheduledFuture<?> scheduledFuture = this.scheduledTimeoutFutures.get(subscriber);
		if (scheduledFuture!=null)
			scheduledFuture.cancel(false);
		/*
		//unschedules ping
		scheduledFuture = this.scheduledPingFutures.get(uploadSlotRequest);
		if (scheduledFuture!=null)
			scheduledFuture.cancel(false);
		*/
	}

	public Collection<Subscriber> snapshotWithTaken() {
		Collection<Subscriber> snapshot = null;
		synchronized (lock)
		{
			snapshot = super.snapshot();
			snapshot.addAll(this.takenElements);
		}
		return snapshot;
	}

	public Subscriber setInterestedGetSubscriber(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final boolean interested) {
		
		if (logger.isDebugEnabled()) logger.debug("setInterestedGetSubscriber("+peerId+","+segmentIdentifier+","+interested+")");
		
		synchronized (lock)
		{
			//finds request, changes property
			Subscriber subscriber = this.get(peerId, segmentIdentifier);

			if (subscriber!=null) {
				subscriber.setInterested(interested);
				
				//unschedules timeout
				ScheduledFuture<?> scheduledFuture = this.scheduledTimeoutFutures.get(subscriber);
				if (scheduledFuture!=null)
					scheduledFuture.cancel(false);
				
				//applies new timeout
				this.scheduleTimeout(subscriber);
			}
			//notifies wait
			lock.notifyAll();
			
			return subscriber;
		}		
	}

	public void setInterested(final Subscriber subscriber, final boolean interested) {
		
		if (logger.isDebugEnabled()) logger.debug("setInterested("+subscriber+","+interested+")");
		
		if (subscriber==null)
			return;
		
		synchronized (lock)
		{
			//changes property
			subscriber.setInterested(interested);
			
			//unschedules timeout
			ScheduledFuture<?> scheduledFuture = this.scheduledTimeoutFutures.get(subscriber);
			if (scheduledFuture!=null)
				scheduledFuture.cancel(false);

			//applies new timeout
			this.scheduleTimeout(subscriber);
			
			//notifies wait
			lock.notifyAll();
		}		
	}

	public Subscriber get(final PeerId peerId, final SegmentIdentifier segmentIdentifier) {
		
		synchronized (lock)
		{
			//finds request, changes property
			for (Subscriber subscriber : super.backingCollection)
				if (subscriber.getRequesterPeerId().equals(peerId) && subscriber.getSegmentIdentifier().equals(segmentIdentifier))
					return subscriber;
			
			return null;
		}
	}

	public boolean hasGrantableUploadSlotRequests() {
		synchronized (lock)
		{
			//finds request, changes property
			for (Subscriber subscriber : super.backingCollection)
				if (subscriber.mayBeGranted())
					return true;
			
			return false;
		}
	}
}
