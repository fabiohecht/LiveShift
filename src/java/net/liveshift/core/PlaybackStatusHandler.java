package net.liveshift.core;

import net.liveshift.core.LiveShiftApplication.PlaybackStatus;

public interface PlaybackStatusHandler {

	public abstract PlaybackStatus getPlaybackStatus();
	public abstract void setPlaybackStatus(PlaybackStatus playbackStatus);
	public abstract void notifyVolumeChanged();
	
}
