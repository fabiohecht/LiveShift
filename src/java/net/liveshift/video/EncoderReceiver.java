package net.liveshift.video;

import net.liveshift.core.Channel;

public interface EncoderReceiver  {
	public void startEncoding();
	public void kill();
	public Channel getChannel();
}