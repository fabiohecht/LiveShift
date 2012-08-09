package net.liveshift.incentive.psh;

import java.util.Random;

import net.liveshift.util.Utils;


public class RandomIncentive<K> extends TFT<K> {

	public RandomIncentive(int timeoutForOldAmountMillis) {
		super(timeoutForOldAmountMillis);
	}

	@Override
	public float getAmountForTrade(K key) {
		
		return Utils.getRandomFloat()*1000;
		/*
		float amount = super.getAmountForTrade(key);
		
		if (amount==0) {
			amount = new Random().nextDouble()*1000;
			this.increaseDownload(key, amount-500);
		}
		
		return super.getAmountForTrade(key);
		*/
	}
}
