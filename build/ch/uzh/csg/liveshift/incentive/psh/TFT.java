package net.liveshift.incentive.psh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TFT<K>
{
	final private static Logger logger = LoggerFactory.getLogger(TFT.class);
	
	final private int timeoutForOldAmountMillis;
	final private List<TFTListener<K>> listeners = new ArrayList<TFTListener<K>>();
	final ConcurrentMap<K, Amount> accounting = new ConcurrentHashMap<K, Amount>();

	public TFT(int timeoutForOldAmountMillis)
	{
		this.timeoutForOldAmountMillis = timeoutForOldAmountMillis;
	}

	public void addTFTListener(TFTListener<K> listener)
	{
		listeners.add(listener);
	}

	public void removeTFTListener(TFTListener<K> listener)
	{
		listeners.remove(listener);
	}

	private void notifyChange(K key, float newAmount)
	{
		for (TFTListener<K> listener : listeners)
			listener.notifyChange(key, newAmount);
	}

	public boolean isKnown(K key)
	{
		return accounting.containsKey(key);
	}
	
	public void setAmount(K key, float value) {
		Amount amount = new Amount(timeoutForOldAmountMillis);
		amount.setAmount(value);
		accounting.put(key, amount);
		notifyChange(key, value);
		if (logger.isDebugEnabled())
			logger.debug("Node:" + key + ", set new balance to " + value);
	}

	public void increaseDownload(K key, float downloadSize)
	{
		Amount amount;
		amount = accounting.get(key);
		if (amount == null)
		{
			amount = new Amount(timeoutForOldAmountMillis);
			accounting.put(key, amount);
		}
		float newAmount = amount.addAmount(downloadSize);
		notifyChange(key, newAmount);

		if (logger.isDebugEnabled())
			if (logger.isDebugEnabled()) logger.debug("Node:" + key + ", changed new balance (" + downloadSize + "):"
					+ (amount.getAmount()));
	}

	float getAmountForTrade0(K key)
	{
		Amount amount = accounting.get(key);
		return (amount != null && amount.readyToTrade()) ? amount.getAmount() : 0;
	}

	public float getAmountForTrade(K key)
	{
		Amount amount = accounting.get(key);
		return (amount != null && amount.readyToTrade()) ? amount.getAmount() : 0;
	}

	public void removeAccounting(K key)
	{
		accounting.remove(key);
	}

	public int nrDownloads(int minDownloadSize)
	{
		int counter = 0;
		for (K key : accounting.keySet())
		{
			// The ones I downloaded from
			if (getAmountForTrade0(key) >= minDownloadSize)
				counter++;
		}
	
		return counter;
	}

	public float cumulateDownloads(int minDownloadSize)
	{
		float size = 0;
		for (K key : accounting.keySet())
		{
			// The ones I downloaded from
			float amount = getAmountForTrade0(key);
			if (amount >= minDownloadSize)
				size += amount;
		}
		return size;
	}

	public int nrUploads(int minUploadSize)
	{
		int counter = 0;
		for (K key : accounting.keySet())
		{
			// The ones I downloaded from
			if (getAmountForTrade0(key) <= minUploadSize)
				counter++;
		}
		return counter;
	}

	public float cumulateUploads(int minUploadSize)
	{
		float size = 0;
		for (K key : accounting.keySet())
		{
			// The ones I downloaded from
			float amount = getAmountForTrade0(key);
			if (amount <= minUploadSize)
				size += amount;
		}
	
		return size;
	}

	public String toString(K key)
	{
		StringBuilder sb = new StringBuilder("History balance for ").append(key).append("=");
		sb.append(accounting.get(key));

		return sb.toString();
	}

	public int size()
	{
		return accounting.size();
	}

	public List<Map.Entry<K, Float>> snapshot()
	{
		List<Map.Entry<K, Float>> tmp = new ArrayList<Map.Entry<K, Float>>();
		for (final Map.Entry<K, Amount> entry : accounting.entrySet())
			if (entry.getValue().getAmount()!=0)  //ONLY SHOWS IF IT'S NOT ZERO!
			{
				tmp.add(new Map.Entry<K, Float>()
				{
					@Override
					public K getKey()
					{
						return entry.getKey();
					}

					@Override
					public Float getValue()
					{
						return entry.getValue().getAmount();
					}

					@Override
					public Float setValue(Float value)
					{
						return null;
					}
				});
			}
		return tmp;
	}
	private static class Amount
	{
		final private int timeoutForOldAmountMillis;
		private float amount;
		private long stamp = Long.MAX_VALUE;

		Amount(int timeoutForOldAmountMillis)
		{
			this.timeoutForOldAmountMillis = timeoutForOldAmountMillis;
		}

		private void update()
		{
			stamp = System.currentTimeMillis();
		}

		boolean readyToTrade()
		{
			// only trade amounts that are old!
			return System.currentTimeMillis() - stamp >= timeoutForOldAmountMillis;
		}

		public float addAmount(float amount)
		{
			this.amount += amount;
			update();
			return this.amount;
		}

		public float getAmount()
		{
			return amount;
		}
		
		public void setAmount(float amount)
		{
			this.amount = amount;
			update();
		}

		@Override
		public String toString()
		{
			return "Amount=" + getAmount();
		}
	}
	public Map<K, Float> getReputationTable() {
		Map<K, Float> map = new HashMap<K, Float>();
		for (java.util.Map.Entry<K, Amount> entry : this.accounting.entrySet())
			map.put(entry.getKey(), entry.getValue().getAmount());
		return map;
	}
}
