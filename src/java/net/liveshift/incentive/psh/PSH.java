package net.liveshift.incentive.psh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.liveshift.core.PeerId;
import net.liveshift.download.NeighborList;
import net.liveshift.signaling.messaging.PshCheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PSH<K> extends TFT<K>
{
	final private static Logger logger = LoggerFactory.getLogger(PSH.class);
	
	// bloomfilter is 1K
//	final static int MAX_BLOOM_BITS = 8096;
	final static int MAX_ENTRIES_PER_MESSAGE = 100;

	private byte[] bloomfilter = new byte[0];
	private boolean changeBloomFilter = false;
	final private AtomicInteger pshAccount = new AtomicInteger(0);

	private NeighborList neighborList;

	public PSH(int timeoutForOldAmount)
	{
		super(timeoutForOldAmount);
		
		addTFTListener(new TFTListener<K>()
		{
			@Override
			public void notifyChange(K key, float amount)
			{
				// notifyChange is called within synchronized(lock)
				if (amount > 0)
					// we need to recreate bloomfilter only for those that we
					// downloaded from
					changeBloomFilter = true;
			}
		});
	}

	public boolean applyCheckTarget(K sender, PshCheck<K> check)
	{
		logger.debug("applying check, sender="+sender+", check="+check);

		if (check.getAmount() <= 0)
			return false;
		// TODO pass public key of intermediate peer
		if (!check.verify(null))
			return false;

		// target downloaded from the source (shift credits)
		increaseDownload(sender, check.getAmount());
		// target did not download from the target anymore (shift credits)
		increaseDownload(check.getIntermediate(), -check.getAmount());
		// here we have a PSH success
		pshAccount.incrementAndGet();
		
		return true;
	}

	public boolean applyCheckSender(K recipient, PshCheck<K> check)
	{
		logger.debug("applying check, recipient="+recipient+", check="+check);

		if (check.getAmount() <= 0)
			return false;
		
		// TODO pass public key of intermediate peer
		
		if (!check.verify(null))
			return false;
		
		// target downloaded from us (shift credits)
		increaseDownload(check.getTarget(), -check.getAmount());
		// intermediate did not download from us anymore (shift credits)
		increaseDownload(check.getIntermediate(), check.getAmount());
	
		return true;
	}

	public Map<K, Float> getNonInterestingNeighbors(int threshold, Set<PeerId> nonInterestingNeighorsNeighbors)
	{
		Map<K, Float> map = new HashMap<K, Float>();
		
		int counter=0;
		for (Iterator<K> iterator = accounting.keySet().iterator(); iterator.hasNext() && counter < MAX_ENTRIES_PER_MESSAGE;) {
			K key = iterator.next();
			// add those peers to the queue where I have debts
			float amountForTrade = getAmountForTrade0(key); 
			if (amountForTrade >= threshold && !this.isPeerInteresting((PeerId) key))
			{
				logger.debug("adding peer "+key+" since "+amountForTrade+" >= "+threshold);
				map.put(key, amountForTrade);  //TODO decide about amountForTrade
				counter++;
			}
		}
		
		return map;
	
	}

	public Collection<K> getInterestingPeersToSendChecks(Map<K, Float> neighborsNeighbors, int threshold, K excludeTarget)
	{
		if (neighborsNeighbors == null)
			throw new IllegalArgumentException("neighborsNeighbors cannot be null");
		if (threshold <= 0)
			throw new IllegalArgumentException("threshold has to be > 0");

		Collection<K> retVal = new ArrayList<K>();
		synchronized (accounting)
		{
			for (K candidate : accounting.keySet())
			{
				// get the ones that I have uploaded to
				if (getAmountForTrade0(candidate) <= -threshold)
				{
					if (!candidate.equals(excludeTarget))
					{
						if (neighborsNeighbors.containsKey(candidate))
						{
							logger.debug("found path, adding "+candidate);

							retVal.add(candidate);
						}
					}
				}
			}
		}
		return retVal;
	}

	// -1 both fail
	// -2 source fail
	// -3 target fail
	public float balanceAccount(K source, K target, int size, boolean atLeast)
	{
		logger.debug("balancing accounts from "+source+" to "+target+", size="+size+" and atL="+atLeast);

		float fromSource = getAmountForTrade0(source);
		float fromTarget = -getAmountForTrade0(target);
		float maxTrade = Math.min(fromSource, fromTarget);
		if (fromSource <= size)
		{
			if (fromTarget <= size)
				return -1;
			else
				return -2;
		}
		else if (fromTarget <= size)
			return -3;
		else
		{
			float amount = atLeast ? maxTrade : size;
			increaseDownload(source, -amount);
			increaseDownload(target, amount);
			return amount;
		}
	
	}
	
	public int getPSHAccount()
	{
		return pshAccount.get();
	}
	
	public Set<K> getNonInterestingPeers() {
		//returns peers that we'd like to move credits from (non-interesting, from which we have credits to reclaim)
		Set<K> nonInterestingPeers = new HashSet<K>();
		int counter=0;
		for (Iterator<K> iterator = accounting.keySet().iterator(); iterator.hasNext() && counter < MAX_ENTRIES_PER_MESSAGE;) {
			K e = iterator.next();
			
			if (!this.isPeerInteresting((PeerId) e))
				nonInterestingPeers.add(e );
			
		}
		return nonInterestingPeers;
	}

	private boolean isPeerInteresting(PeerId peerId) {
		
		if (this.neighborList==null) {
			logger.error("NEIGHBOR LIST MUST BE SET!!!!!!!!!!");
			System.err.println("NEIGHBOR LIST MUST BE SET in PSH");
			return true;
		}
		if (peerId==null) {
			logger.error("peerId must be SET!!!!!!!!!!");
			System.err.println("peerId must be SET");
			return true;
		}
		return this.neighborList.isInteresting(peerId);
	}

	public void setNeighborList(NeighborList neighborList) {
		this.neighborList = neighborList;
	}

}
