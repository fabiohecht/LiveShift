package net.liveshift.core;

import net.liveshift.configuration.PublishConfiguration;
import net.liveshift.core.LiveShiftApplication.PlaybackStatus;
import net.liveshift.core.LiveShiftApplication.PlayerStatus;

public interface UiListener {
	public void connectionStateChanged();
	public void publishStateChanged(PublishConfiguration publishTabBean);
	public void playingStateChanged(Channel channel, PlayerStatus playerStatus, PlaybackStatus playbackStatus, long playTime);
	public void volumeChanged(float volumePercent);
}
