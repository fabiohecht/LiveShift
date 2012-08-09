package net.liveshift.incentive.psh;

import net.liveshift.time.Clock;

public class FcfsIncentive<K> extends TFT<K> {

	public FcfsIncentive(int timeoutForOldAmountMillis) {
		super(timeoutForOldAmountMillis);
	}
	
	@Override
	public float getAmountForTrade(K key) {
		
		float amount = super.getAmountForTrade(key);
		
		if (amount==0) {
			amount = -Clock.getMainClock().getTimeInMillis(false);
			super.setAmount(key, amount);
		}

		return amount;

	}
	
	@Override
	public void increaseDownload(K key, float downloadSize) {

		float amount = super.getAmountForTrade(key);
		
		if (amount==0) {
			amount = -Clock.getMainClock().getTimeInMillis(false);
			super.setAmount(key, amount);
		}
	}
	
}