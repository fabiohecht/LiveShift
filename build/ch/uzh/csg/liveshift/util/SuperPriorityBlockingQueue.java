package net.liveshift.util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.liveshift.upload.PreemptedElementsListener;

/**
 * This class is a priority blocking queue/dequeue, which offer at the moment
 * only offer() and take(). However, this can be extended easily as its backed
 * by a TreeSet. The class is thread safe and uses locking to not interfere with
 * each other.
 * 
 * @author Thomas Bocek
 * 
 * @param <E>
 */
public class SuperPriorityBlockingQueue<E extends Comparable<E>> {
	protected final Collection<E> backingCollection;
	protected final Object lock = new Object();
	private final Collection<PreemptedElementsListener<E>> preemptedElementsListeners = new ArrayList<PreemptedElementsListener<E>>();
	private final int queueSize;
	private final boolean preemptive;

	/**
	 * Creates a new queue. This queue is backed by an array, so the compare()
	 * method may change at any time, since always all entries are compared. The
	 * comparator is used only for checking the priority, equals on the element
	 * is used to check if element is present in the list. The queue has a fixed
	 * size, and cannot have more entries. However, elements with a higher
	 * priority can make it to the list, while other elements with lowest
	 * priority gets thrown out.
	 * 
	 * @param queueSize
	 * @param priorityComparator
	 * @param preemptive
	 */
	
	public SuperPriorityBlockingQueue(int queueSize, boolean preemptive)
	{
		this.backingCollection = new HashSet<E>(queueSize);
		this.queueSize = queueSize;
		this.preemptive = preemptive;
	}

	/**
	 * Creates a new queue, without size limit. It can grow to Integer.MAX_VALUE
	 * 
	 */
	public SuperPriorityBlockingQueue() {
		this.backingCollection = new HashSet<E>();
		this.queueSize = Integer.MAX_VALUE;
		this.preemptive = false;  //it actually does not matter, since it will never get full (up to Integer.MAX_VALUE)
	}


	/**
	 * Puts an element into the queue under the following conditions: e is not
	 * null, e is not in the list (checked with equals(...)), adding e does not
	 * exceed queue size *and* priority is higher than other elements in the
	 * queue (checked with the comparator). In the last case, the element with
	 * the lowest priority get thrown out and the listeners are notified. This
	 * method might be costly. It is expensive if the element is not in the
	 * queue and the queue has full size, then we iterate over the queue to find
	 * elements with lower priority. If element is already in the list, it's not
	 * added again but the method returns true.
	 * 
	 * @param e The element to add
	 * @return True if element was added successfully or if it was already there
	 */
	public boolean offer(E e)
	{
		if (e == null)
			throw new IllegalAccessError("cannot add null");
		synchronized (lock)
		{
			// this iterates over all entries
			if (backingCollection.contains(e))
				return true;
			if (backingCollection.size() >= queueSize)
			{
				if (!this.isPreemptive())
					return false;
				else
				{
					return this.preemptAndOffer(e);
				}
			}
			else
			{
				backingCollection.add(e);
				lock.notifyAll();
				return true;
			}
		}
	}
	
	protected boolean preemptAndOffer(E e) {

		// check for lower-priority entries
		E lowestPriority = first(backingCollection);
		if (lowestPriority.compareTo(e) < 0)
		{
			// lowest priority from Q has lower priority then the one being added
			this.preempt(lowestPriority);
			
			backingCollection.add(e);
			lock.notifyAll();
			
			return true;
		}
		else
			return false;
	}
	
	protected void preempt(E e) {
		boolean removed = this.backingCollection.remove(e);
		assert (removed);
		notifyPreemptedElementsListeners(e);
	}

	/**
	 * Takes the element with the highest priority and removes it. Otherwise it
	 * waits until an element is available. Be aware that this is a costly
	 * operation as we iterate over the complete queue.
	 * 
	 * @return The element with the highest priority
	 * @throws InterruptedException
	 */
	public E pollLast() throws InterruptedException
	{
		while (true)
		{
			synchronized (lock)
			{
				E result = pollLast(backingCollection);
				if (result != null) {
					
					return result;
				}
				else
					lock.wait();
			}
		}
	}

	/**
	 * Takes the element with the lowest priority and removes it. Otherwise it
	 * waits until an element is available. Be aware that this is a costly
	 * operation as we iterate over the complete queue.
	 * 
	 * @return The element with the lowest priority
	 * @throws InterruptedException
	 */
	public E pollFirst() throws InterruptedException
	{
		while (true)
		{
			synchronized (lock)
			{
				E result = pollFirst(backingCollection);
				if (result != null) {
					
					return result;
				}
				else
					lock.wait();
			}
		}
	}

	/**
	 * Takes the element with the highest priority and removes it. Otherwise it
	 * waits until an element is available. Be aware that this is a costly
	 * operation as we iterate over the complete queue. Ignore
	 * InterruptedExceptions
	 * 
	 * @return The element with the highest priority
	 */
	public E takeUninterruptedly()
	{
		while (true)
		{
			synchronized (lock)
			{
				E result = pollFirst(backingCollection);
				if (result != null)
					return result;
				else
				{
					try
					{
						lock.wait();
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}
	}

	/**
	 * @return Size of the queue
	 */
	public int size()
	{
		synchronized (lock)
		{
			return backingCollection.size();
		}
	}

	/**
	 * This operation is fast, and we do not need to iterate over the complete
	 * collection as the backing collection is a hashset.
	 * 
	 * @param e Element to check if it is contained
	 * @return True if the element is in the queue.
	 */
	public boolean contains(E e)
	{
		synchronized (lock)
		{
			return backingCollection.contains(e);
		}
	}


	@Override
	public String toString() {
		List<E> snapshot = this.snapshot();
		
		String out = "";
		for (E element : snapshot) {
			out += element.toString() + ":";
		}
		return out;
	}

	public List<E> snapshot()
	{
		synchronized (lock)
		{
			ArrayList<E> copy = new ArrayList<E>(backingCollection);
			Collections.sort(copy);
			return copy;
		}
	}
	
	private <E extends Comparable<E>> E last(Collection<E> backingList)
	{
		E last = null;
		for (E e : backingList)
		{
			if (last == null || e.compareTo(last) > 0)
				last = e;
		}
		return last;
	}

	private <E extends Comparable<E>> E first(Collection<E> backingList)
	{
		E first = null;
		for (E e : backingList)
		{
			if (first == null || e.compareTo(first) < 0)
				first = e;
		}
		return first;
	}

	protected <E extends Comparable<E>> E pollLast(Collection<E> backingList)
	{
		E last = last(backingList);
		if (last != null)
		{
			boolean remove = backingList.remove(last);
			assert (remove);
		}
		return last;
	}

	private <E extends Comparable<E>> E pollFirst(Collection<E> backingList)
	{
		E first = first(backingList);
		if (first != null)
		{
			boolean remove = backingList.remove(first);
			assert (remove);
		}
		return first;
	}

	protected void notifyPreemptedElementsListeners(E e)
	{
		// make a copy! or its also possible to make sure that
		// addRemovedElementsListener / removeRemovedElementsListener is never
		// called. But making a copy, we don't need to worry about it.
		List<PreemptedElementsListener<E>> listenersCopy;
		synchronized (preemptedElementsListeners)
		{
			listenersCopy = new ArrayList<PreemptedElementsListener<E>>(preemptedElementsListeners);
		}
		for (PreemptedElementsListener<E> listener : listenersCopy)
			listener.elementRemoved(e);
	}

	/**
	 * Whenever an element with a lower priority gets removed, these listener
	 * are notified.
	 * 
	 * @param preemptedElementsListener The listener to notify on an element
	 *        removal
	 */
	public void addPreemptedElementsListener(PreemptedElementsListener<E> preemptedElementsListener)
	{
		if (preemptedElementsListener == null)
			throw new IllegalAccessError("cannot add null");
		synchronized (preemptedElementsListeners)
		{
			preemptedElementsListeners.add(preemptedElementsListener);
		}
	}

	/**
	 * Removes the element removal listener.
	 * 
	 * @param removedElementsListener
	 */
	public void removeRemovedElementsListener(PreemptedElementsListener<E> removedElementsListener)
	{
		if (removedElementsListener == null)
			throw new IllegalAccessError("cannot add null");
		synchronized (preemptedElementsListeners)
		{
			preemptedElementsListeners.remove(removedElementsListener);
		}
	}

	/**
	 * Returns whether this queue is preemptive, that is, elements may be thrown
	 * out of it when it is full and an element with higher priority comes in
	 * 
	 * @return
	 */
	public boolean isPreemptive()
	{
		return this.preemptive;
	}

	/**
	 * Removes the given element from the queue
	 * 
	 * @param e
	 * @return
	 */
	public boolean remove(E e) {

		synchronized (lock)
		{
			return this.backingCollection.remove(e);
		}
	}

	public E get(final E e) {
		
		synchronized (lock)
		{
			for (E eLocal : this.backingCollection)
				if (eLocal.equals(e))
					return eLocal;
			return null;
		}
	}

	/**
	 * removes all elements into collection
	 * @param collection
	 */
	public void drainTo(Collection<E> collection) {
		synchronized (lock)
		{
			Iterator<E> iter = this.backingCollection.iterator();
			while (iter.hasNext()) {
				E e = iter.next();
				collection.add(e);
				iter.remove();
			}
		}
	}
	
	/**
	 * removes all elements into collection and returns the collection (as a hashset)
	 */
	public Collection<E> getAll() {
		Collection<E> collection = new HashSet<E>();
		this.drainTo(collection);
		return collection;
	}

}
