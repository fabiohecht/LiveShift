package net.liveshift.player;

import java.awt.Canvas;
import java.util.Set;

import net.liveshift.configuration.Configuration.StreamingProtocol;
import net.liveshift.core.PlayerInitializationException;
import net.liveshift.util.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.discovery.NativeDiscoveryStrategy;


public class VLCJPlayer implements Player
{
	final private static Logger logger = LoggerFactory.getLogger(VLCJPlayer.class);

	private static final float MAX_VOLUME = 200;

	private final Object lock = new Object();
	private MediaPlayerFactory mpf;
	private EmbeddedMediaPlayer mediaPlayer;
	private Canvas videoCanvas;
	
	public VLCJPlayer(final Set<String> pathToVlcLibs) throws PlayerInitializationException {
		try {
			NativeDiscovery nativeDiscovery = new NativeDiscovery();
	
			if (!nativeDiscovery.discover()) {
				for (String path : pathToVlcLibs) {
					NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), path);
				}
			    Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
			}
			
			this.mpf = new MediaPlayerFactory();
		}
		catch (RuntimeException e) {
			//error initializing player
			String message = "Error initializing VLCJ Player, tried libs from: "+Utils.toString(pathToVlcLibs);
			logger.error(message);
			throw new PlayerInitializationException(e);
		}
	}

	@Override
	public void setCanvas(Canvas jvcanvas)
	{
		synchronized (lock)
		{
				this.videoCanvas = jvcanvas;
				if (mediaPlayer != null) {
					mediaPlayer.setVideoSurface(mpf.newVideoSurface(videoCanvas));
				}

				logger.debug("vlcj player - canvas set");
		}
	}
	
	@Override
	public void initializeLocalStream(final String localIpAddress, final int playerPort, StreamingProtocol protocol)
	{
		synchronized (lock)
		{	
			
			if (videoCanvas==null) {
				logger.error("You need to setCanvas before initializeLocalStream");
				System.exit(1);
			}
			
			this.mediaPlayer = mpf.newEmbeddedMediaPlayer();
			mediaPlayer.setVideoSurface(mpf.newVideoSurface(videoCanvas));

			logger.debug("created mpf v." + this.mpf.version());

			logger.debug("vlcj player - prepare - " + protocol.name().toLowerCase() + "://@:" + playerPort);
			
			this.mediaPlayer.prepareMedia(protocol.name().toLowerCase() +"://@:" + playerPort);
		}
		
	}
	
	@Override
	public void play()
	{
		synchronized (lock)
		{
			if (mediaPlayer != null){
				mediaPlayer.play();
				
				logger.debug("vlcj player - play");
			}
			else
				throw new RuntimeException("media player is not initialazed");
		}
	}
	@Override
	public void shutdown()
	{
		synchronized (lock)
		{
			if (this.mediaPlayer != null)
			{
				this.mediaPlayer.stop();
				this.mediaPlayer.release();
			}
		}
	}

	@Override
	public void pausePlayer()
	{
		synchronized (lock)
		{
			if (mediaPlayer != null){
				mediaPlayer.pause();

				logger.debug("vlcj player - pause");
			}
		}
	}


	@Override
	public void stopPlayer()
	{
		synchronized (lock)
		{
			if (mediaPlayer != null){
				mediaPlayer.stop();

				logger.debug("vlcj player - stop");
			}
		}
	}
	
	@Override
	public float getVolumePercent() {
		if (logger.isDebugEnabled()) {
			logger.debug("returning volume "+this.mediaPlayer.getVolume());
		}
		return this.mediaPlayer.getVolume()*100/MAX_VOLUME;
	}
	
	@Override
	public void setVolumeUp(int amountPercent){
		int oldvolume = mediaPlayer.getVolume();
		mediaPlayer.setVolume((int) (oldvolume + amountPercent*MAX_VOLUME/100));
	}
	
	@Override
	public void setVolumeDown(int amountPercent){
		int oldvolume = mediaPlayer.getVolume();
		mediaPlayer.setVolume((int) (oldvolume - amountPercent*MAX_VOLUME/100));
	}

	@Override
	public void setVolumePercent(float volumePercent) {
		mediaPlayer.setVolume((int) (volumePercent*MAX_VOLUME/100));
	}
	
}
