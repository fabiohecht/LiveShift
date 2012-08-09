package net.liveshift.incentive.psh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.liveshift.core.PeerId;
import net.liveshift.signaling.PshVideoSignaling;


/*
 * Controls the PSH process that makes PSH credit transfers between peers
 */
public class PshTrader
{
	final private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	final private Map<PeerId, Long> addresses = new HashMap<PeerId, Long>();
	final private Set<PeerId> blackList = new HashSet<PeerId>();
	final private static int CHECK_INTERVAL_MILLIS = 30 * 1000;
	private volatile ScheduledFuture<?> scheduledFuture;
	final private PSH<PeerId> history;
	private PshVideoSignaling pshVideoSignaling;

	public PshTrader(final PshVideoSignaling pshVideoSignaling, final PSH<PeerId> history)
	{
		this.history = history;
		this.pshVideoSignaling = pshVideoSignaling;
	}
	
	public void addTFTListener(TFTListener<PeerId> listener)
	{
		history.addTFTListener(listener);
	}

	public void startTrading()
	{
		scheduledFuture = executor.scheduleWithFixedDelay(new Worker(this.pshVideoSignaling), 0, 1, TimeUnit.SECONDS);
	}

	public void stopTrading()
	{
		scheduledFuture.cancel(true);
	}

	public void addPeerForTrading(PeerId peerId)
	{
		synchronized (addresses)
		{
			if (!addresses.containsKey(peerId) && !blackList.contains(peerId))
			{
				addresses.put(peerId, 0L);
			}
		}
	}

	public void shutdown()
	{
		executor.shutdownNow();
	}

	private class Worker implements Runnable
	{
		final private PshVideoSignaling pshVideoSignaling;

		public Worker(final PshVideoSignaling pshVideoSignaling)
		{
			this.pshVideoSignaling = pshVideoSignaling;
		}

		@Override
		public void run()
		{
			try {
				List<Runnable> refreshTimings = new ArrayList<Runnable>();
				synchronized (addresses)
				{
					if(addresses.size()==0)
						System.err.println("No addresses found, did you add peers with addPeerForTrading?");
					else
					{
						for (final Map.Entry<PeerId, Long> entry : addresses.entrySet())
						{
							if (System.currentTimeMillis() > entry.getValue() + CHECK_INTERVAL_MILLIS)
							{
								//sends message with non-interesting peers, querying for intermediate peers
								pshVideoSignaling.sendPshDownloadersRequest(entry.getKey(), history.getNonInterestingPeers());
								
								refreshTimings.add(new Runnable()
								{
									@Override
									public void run()
									{
										addresses.put(entry.getKey(), System.currentTimeMillis());
									}
								});
							}
						}
						for (Runnable runnable : refreshTimings)
							runnable.run();
					}
				}
			} catch (Exception e) {
				// just so it doesn't die silently if an unhandled exception happened
				e.printStackTrace();
			}


		}
	}
	
}
