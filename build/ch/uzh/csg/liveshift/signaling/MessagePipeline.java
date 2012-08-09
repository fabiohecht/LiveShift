package net.liveshift.signaling;

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.time.Clock;
import net.liveshift.util.SuperPriorityBlockingQueue;


public class MessagePipeline extends SuperPriorityBlockingQueue<AbstractMessage> {

	final private static Logger logger = LoggerFactory.getLogger(MessagePipeline.class);

	protected final MessageFlowControl flowControl;

	public MessagePipeline(MessageFlowControl flowControl) {
		this.flowControl = flowControl;
	}

	@Override
	public AbstractMessage pollFirst() throws InterruptedException
	{
		while (true)
		{
			synchronized (lock)
			{
				AbstractMessage result = pollFirst(backingCollection);
				if (result != null) {
					
					return result;
				}
				else
					lock.wait(25);  //needs to check if waiting ones are done waiting somehow, that's why the timeout
			}
		}
	}
	
	private AbstractMessage pollFirst(Collection<AbstractMessage> backingList)
	{
		AbstractMessage first = first(backingList);
		if (first != null)
		{
			boolean remove = backingList.remove(first);
			assert (remove);
		}
		return first;
	}
	
	private AbstractMessage first(Collection<AbstractMessage> backingList)
	{
		AbstractMessage first = null;
		Iterator<AbstractMessage> iter = backingList.iterator();
		
		long referenceTime = Clock.getMainClock().getTimeInMillis(false);
		
		while (iter.hasNext())
		{
			AbstractMessage message = iter.next();
			
			switch (this.flowControl.acceptMessage(message, referenceTime)) {
				case REJECTED:
					message.setProcessed(); //sets processed to drop it immediately
					return message;
				case WAIT:
					continue;
			}
			
			if (first == null || message.compareTo(first) < 0)
				first = message;	

		}
		return first;
	}

}
