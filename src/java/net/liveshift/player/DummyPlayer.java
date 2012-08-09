package net.liveshift.player;

import java.awt.Canvas;

import net.liveshift.configuration.Configuration.StreamingProtocol;



public class DummyPlayer implements Player {

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pausePlayer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void play() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setCanvas(Canvas jvcanvas) {
		// TODO Auto-generated method stub

	}

	@Override
	public void stopPlayer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initializeLocalStream(String localIpAddress, int playerPort, StreamingProtocol protocol) {
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getVolumePercent() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setVolumeUp(int amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVolumeDown(int amount) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVolumePercent(float volumePercent) {
		// TODO Auto-generated method stub
		
	}

}
