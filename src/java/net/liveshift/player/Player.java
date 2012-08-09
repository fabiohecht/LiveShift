package net.liveshift.player;
import java.awt.Canvas;

import net.liveshift.configuration.Configuration.StreamingProtocol;


/**
 * 
 * @author Fabio Victora Hecht
 */
public interface Player
{
	public void play();

	public void pausePlayer();

	public void stopPlayer();

	public void initializeLocalStream(String localIpAddress, int playerPort, StreamingProtocol protocol);

	public void setCanvas(Canvas jvcanvas);

	public void shutdown();

	public float getVolumePercent();
	
	public void setVolumePercent(float volumePercent);
	
	public void setVolumeUp(int amount);
	
	public void setVolumeDown(int amount);

}
