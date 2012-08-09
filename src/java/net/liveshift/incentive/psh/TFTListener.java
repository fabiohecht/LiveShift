package net.liveshift.incentive.psh;

public interface TFTListener<K>
{
	public abstract void notifyChange(K key, float amount);

}
