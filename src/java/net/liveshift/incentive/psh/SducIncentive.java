package net.liveshift.incentive.psh;

public class SducIncentive<K> extends TFT<K> {

	public SducIncentive(int timeoutForOldAmountMillis) {
		super(timeoutForOldAmountMillis);
	}
	
	@Override
	public float getAmountForTrade(K key) {
		
		float amount = super.getAmountForTrade(key);
		
		return amount;
	}
	
	@Override
	public void increaseDownload(K key, float uploadCapacity) {

		float amount = super.getAmountForTrade(key);
		
		if (amount==0) {
			amount = uploadCapacity;   //sets the upload capacity
			super.increaseDownload(key, amount);
		}
	}
	
}