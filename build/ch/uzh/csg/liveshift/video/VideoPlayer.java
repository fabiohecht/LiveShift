package net.liveshift.video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.configuration.Configuration.PlayerName;
import net.liveshift.core.Channel;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.core.LiveShiftApplication.PlaybackStatus;
import net.liveshift.player.DummyPlayer;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;
import net.liveshift.util.TriggableScheduledExecutorService;
import net.liveshift.video.playbackpolicies.*;
import net.liveshift.video.playbackpolicies.PlaybackPolicy.PlayingDecision;

/*
 * controls the playing position
 * pulls blocks from storage
 * multiplexes blocks (from different substreams)
 * sends blocks to the player at appropriate rate
 * stalls ot skips if required blocks are not present
 * 
 */
public class VideoPlayer {

	private static final int BURST_BLOCKS = (int) (2000/Configuration.SEGMENTBLOCK_SIZE_MS);
	private static final int FAILED_PLAYBACK_WINDOW_BLOCKS = 30;

	private final long startTimeMS;
	private final Clock playClock;  //keeps track of playing position
	private final Clock playClockPerfect;  //playing position in a perfect world
	private AtomicInteger skippedBlocks=new AtomicInteger();  //for the statistics -- number of blocks skipped
	private AtomicInteger playedBlocks=new AtomicInteger();  //for the statistics -- number of blocks played
	private AtomicInteger skippedOccasions=new AtomicInteger();  //for the statistics -- how many times n>0 blocks were skipped
	private PlayingDecision lastPlayingDecision;
	private long lastTimeStalled;
	private boolean firstPlay = true;

	final private LiveShiftApplication liveShiftApplication;
	private VideoPlayerSender videoPlayerSender;
	private Channel channel; //channel we are playing
	private SegmentStorage segmentStorage; //storage where blocks come from 
	final private Map<SegmentIdentifier, Segment> segments;  //segments being played (object fetches them by itself)
	private PlaybackPolicy playbackPolicy;  //stall/skip policy
	final private MovingAverage playbackAverage = new MovingAverage(Clock.getMainClock(), FAILED_PLAYBACK_WINDOW_BLOCKS, (int)Configuration.SEGMENTBLOCK_SIZE_MS);
	
	private long lastPlayedBlocksStartTimeMs;  //to avoid double-playing
	//private long minimumDelay;

	private boolean paused = false;
	private boolean stalled = false;
	
	private int bursted = 0;  //for VLC not to get skippy, we accummulate and burst X blocks together, then normalize
	private Collection<PacketData> burstBuffer;
		
	private TriggableScheduledExecutorService playExecutorService;
	
	final private static Logger logger = LoggerFactory.getLogger(VideoPlayer.class);

	//schedules block playback
	final private Callable<Boolean> scheduledTask = new Callable<Boolean>() {
		
		@Override
		public Boolean call() {
			try {
				return play();
			}
			catch (Exception e)
			{
				//just so it doesn't die silently if an unhandled exception happened
				e.printStackTrace();
				
				return false;
			}
		}
	};
	
	//reports stats
	final private Runnable delayReporter = new Runnable() {
		
		@Override
		public void run() {
			try {
				
				isPlaybackFailed();
				
				long perfectPlayTime = playClockPerfect.getTimeInMillis(false);
				long playTime = playClock.getTimeInMillis(false);
				
				logger.info(Clock.getMainClock().getTimeInMillis(false)+" DELAY AT "+(perfectPlayTime-startTimeMS)+" IS "+(perfectPlayTime-playTime)+" "+lastPlayingDecision+/*" MIN "+(minimumDelay)+ */" SKIPPED " +(skippedBlocks.floatValue()/(playedBlocks.get()+skippedBlocks.get()==0?1:playedBlocks.get()+skippedBlocks.get())) + " "+ skippedBlocks.get()+"/"+(playedBlocks.get()+skippedBlocks.get())+" win="+playbackAverage.getSum()+"/"+playbackAverage.getValidBoxCount());
			} catch (Exception e) {
				// just so it doesn't die silently if an unhandled exception happened
				e.printStackTrace();
			}
		}
	};
	final private ScheduledFuture<?>	infoExecutorService;
	private int numNeighborsHaveBlock;

	public VideoPlayer(SegmentStorage segmentStorage, Channel channel, long startTimeMS, LiveShiftApplication liveShiftApplication) {
		
		this.liveShiftApplication = liveShiftApplication;
		this.segmentStorage = segmentStorage;
		this.channel = channel;
		
		this.playClockPerfect = new Clock();
		this.playClockPerfect.setTime(startTimeMS, false);
		
		this.playClock = new Clock();
		this.playClock.setTime(startTimeMS, false);

		this.startTimeMS = startTimeMS;

		//sets playback policy
		int initialBufferSizeSeconds = this.liveShiftApplication.getConfiguration().getPlaybackBuffering();
		float playbackPolicyParameter = this.liveShiftApplication.getConfiguration().getPlaybackPolicyParameter();
//		float playbackPolicyParameter2 = this.liveShiftApplication.getConfiguration().getPlaybackPolicyParameter2();
		switch (this.liveShiftApplication.getConfiguration().getPlaybackPolicy()) {
			case SMART_RETRY:
				playbackPolicy = new SmartRetryPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds, (int) playbackPolicyParameter); 
				break;
			case RATIO:
				playbackPolicy = new RatioPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds, (int) playbackPolicyParameter); 
				break;
			case RDT:
				RdtPlaybackPolicy rtdPlaybackPolicy = new RdtPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds, (int) playbackPolicyParameter);
				rtdPlaybackPolicy.registerIncomingBlockRateHandler(this.liveShiftApplication.getTuner());
				playbackPolicy = rtdPlaybackPolicy;
				break;
			case RETRY:
				playbackPolicy = new RetryPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds, (int) playbackPolicyParameter); 
				break;
			case SKIPSTALL:
				playbackPolicy = new SkipThresholdPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds, playbackPolicyParameter); 
				break;
			case CATCHUP:
				playbackPolicy = new CatchUpPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds); 
				break;
			default:
				playbackPolicy = new RatioPlaybackPolicy(channel.getNumSubstreams(), initialBufferSizeSeconds, (int) playbackPolicyParameter); 
		}
		
		//creates objects
		this.videoPlayerSender =  new VideoPlayerSender(liveShiftApplication.getConfiguration().getLocalIpAddress(), liveShiftApplication.getConfiguration().getPlayerPort());
		this.segments = new HashMap<SegmentIdentifier,Segment>();
		
		this.playExecutorService = new TriggableScheduledExecutorService(this.scheduledTask, Configuration.SEGMENTBLOCK_SIZE_MS-25, 20, Configuration.SEGMENTBLOCK_SIZE_MS-25, Clock.getMainClock(),"VideoPlayer");
		this.infoExecutorService = ExecutorPool.getScheduledExecutorService().scheduleWithFixedDelay(delayReporter, Configuration.SEGMENTBLOCK_SIZE_MS, Configuration.SEGMENTBLOCK_SIZE_MS, TimeUnit.MILLISECONDS);
		
	}

	public synchronized void shutdown() {
		if (logger.isDebugEnabled()) logger.debug("TIME"+System.currentTimeMillis()+" SHUTDOWN");
		if (this.playExecutorService != null)
			this.playExecutorService.shutdown();
		if (this.infoExecutorService != null)
			this.infoExecutorService.cancel(false);
	}
	
	/**
	 * plays one block of video, according to this.playClock
	 * plays by sending video data to the VideoPlayerSender
	 * 
	 * @return whether playing was successful or not (stalled)
	 */
	protected synchronized boolean play() {
		boolean playedOK;

		//gets current play time
		long playTimeMs = this.playClock.getTimeInMillis(false);
		long perfectPlayTimeMs = this.playClockPerfect.getTimeInMillis(false);

		if (logger.isDebugEnabled()) logger.debug("play() time: "+playTimeMs+" diff:"+(perfectPlayTimeMs-playTimeMs)+ " ratio:"+(perfectPlayTimeMs-this.startTimeMS)/(float)(playTimeMs-this.startTimeMS));
		
		if (!this.isStalled() && this.lastPlayedBlocksStartTimeMs == SegmentBlock.getStartTimeMillis(playTimeMs)) {
			if (logger.isDebugEnabled()) logger.debug("already played this, skipping (returning false)");
			return false;
		}
		else
			this.lastPlayedBlocksStartTimeMs = SegmentBlock.getStartTimeMillis(playTimeMs);
		
		//stats
		this.liveShiftApplication.getStats().setPlayPosition(playTimeMs);

		this.liveShiftApplication.setPlayTimeMs(playTimeMs);
		
		if (this.isPaused())
			return false;
		
		//gets blocks for current time
		this.getSegments(playTimeMs, perfectPlayTimeMs);
		
		//merges packets from all substreams
		this.numNeighborsHaveBlock=-1;
		Collection<PacketData> videoData = this.getMergedVideo(playTimeMs, perfectPlayTimeMs);
		
		//evaluates missing data and acts accordingly
		if (this.playbackPolicy instanceof SmartRetryPlaybackPolicy) {
			((SmartRetryPlaybackPolicy) this.playbackPolicy).setNumNeighborsHaveBlock(numNeighborsHaveBlock);
		}
		PlayingDecision playingDecision = this.playbackPolicy.getPlayDecision();

		if (playingDecision==PlayingDecision.SKIP && this.lastPlayingDecision != playingDecision && this.playedBlocks.get()>0) {
			this.skippedOccasions.incrementAndGet();
		}

		this.lastPlayingDecision = playingDecision;
		
		if (playingDecision==PlayingDecision.STALL) {

			if (logger.isInfoEnabled()) logger.info("playback policy: STALL ("+(this.playbackPolicy.shareNextBlock()*100)+"% blocks are here) at segment "+channel.getName()+" "+SegmentIdentifier.getSegmentNumber(playTimeMs)+" b#"+SegmentBlock.getBlockNumber(playTimeMs));

			if (!this.isStalled())
				this.setStalled(true);
			
			this.playClock.setTime(playTimeMs, false);
		}
		else if (playingDecision==PlayingDecision.SKIP) {

			int blocksToSkip=this.playbackPolicy.getNumBlocksToSkip();
			
			if (logger.isInfoEnabled()) logger.info("playback policy: SKIP "+blocksToSkip+" blocks ("+(this.playbackPolicy.shareNextBlock()*100)+"% blocks are here) at segment "+SegmentIdentifier.getSegmentNumber(playTimeMs)+" b#"+SegmentBlock.getBlockNumber(playTimeMs));
			this.skippedBlocks.addAndGet(blocksToSkip);
			this.playClock.delay(-Configuration.SEGMENTBLOCK_SIZE_MS*blocksToSkip, true);

			return play();
		}
		else if (this.isStalled()) {
			
			if (logger.isDebugEnabled()) logger.debug("video has (finally) arrived! unstalling.");
			//unstalls
			this.setStalled(false);
		}
		
		if (!this.isStalled()) {

			this.playedBlocks.incrementAndGet();
			this.playbackAverage.inputValue(1);

			if (logger.isInfoEnabled()) logger.info("playback policy: PLAY ("+(this.playbackPolicy.shareNextBlock()*100)+"% blocks are here) LAG "+
					((float)(playClockPerfect.getTimeInMillis(false)-playClock.getTimeInMillis(false))/(playClockPerfect.getTimeInMillis(false)-this.startTimeMS))+" "+
					(playClockPerfect.getTimeInMillis(false)-playClock.getTimeInMillis(false))+"/"+(playClockPerfect.getTimeInMillis(false)-this.startTimeMS)+
					" CNT "+(((float)(playedBlocks.get()-skippedBlocks.get())))/(playedBlocks.get()==0?1:playedBlocks.get())+
					" P "+playedBlocks.get()+
					" SB "+skippedBlocks.get()+
					" SO "+skippedOccasions.get()+
					" at segment "+SegmentIdentifier.getSegmentNumber(playTimeMs)+" b#"+SegmentBlock.getBlockNumber(playTimeMs));
			
			//for VLC not to get skippy in the beginning, we burst 2 blocks in the first sending, then normalize
			if (!liveShiftApplication.getConfiguration().getPlayerName().equals(PlayerName.Dummy) && BURST_BLOCKS > this.bursted) {
				if (logger.isDebugEnabled()) logger.debug("keeping "+this.bursted+"/"+BURST_BLOCKS+" video blocks for bursting"); 
				
				this.burstBuffer=videoData;
				this.bursted++;
				return true;
			}
			else if (this.burstBuffer!=null) {
				if (logger.isDebugEnabled()) logger.debug("joining for bursting");
				
				this.burstBuffer.addAll(videoData);
				videoData = this.burstBuffer;
				this.burstBuffer=null;
			}
			
			this.showInfoPlaying(videoData.size());
			
			//sends to player
			long timeToSleepBetweenPackets = (long) (((float)Configuration.SEGMENTBLOCK_SIZE_MS / (float)videoData.size()) * .9F);  //10% margin in case sendDataToPlayer takes significant time
			Iterator<PacketData> iterPacket = videoData.iterator();
			while (iterPacket.hasNext())
				try {
					this.videoPlayerSender.sendDataToPlayer(iterPacket.next().getVideoData());
					
					Thread.sleep(timeToSleepBetweenPackets);
					
				} catch (IOException e) {
					logger.error("Exception "+e+" when sending data to videoPlayer.");
					e.printStackTrace();
				}
				catch (NullPointerException e) {
					logger.error("NullPointerException when sending data to videoPlayer. Maybe the DatagramSocket has been closed.");
				} catch (InterruptedException e) {
					logger.debug("interrupted while sending packets to player");
					return false;
				}
			
			playedOK = true;
		}
		else {
			this.showInfoStalled();
			
			playedOK = false;
		}
		
		return playedOK;
	}
	
	private void showInfoStalled() {

		//shows info in GUI/logger
		this.liveShiftApplication.setPlaybackStatus(PlaybackStatus.STALLED);
		//this.liveShiftApplication.getStatusPanelBean().setQuality(0);
		
		logger.info("stalled, play delay is "+(Clock.getMainClock().getTimeInMillis(false)-this.playClock.getTimeInMillis(false))+" ms");

	}

	private void showInfoPlaying(int numPackets) {

		//shows info in GUI/logger
		this.liveShiftApplication.setPlaybackStatus(PlaybackStatus.PLAYING);
		
		if (this.firstPlay) {
			//VLCJ needs some time after playback to return the right volume level (some time > 800ms)
			ExecutorPool.getScheduledExecutorService().schedule(new Runnable() {
				
				@Override
				public void run() {
					liveShiftApplication.notifyVolumeChanged();
				}
			}, 1000, TimeUnit.MILLISECONDS);
			this.firstPlay = false;
		}
		
		//this.liveShiftApplication.getStatusPanelBean().setQuality((int) (this.segmentBlockMonitor.shareNextBlock()*100));
		if (logger.isDebugEnabled()) logger.debug("sending video data to player ("+numPackets+" packets)");
		
	}

/**
 * gets the segment descriptions for the given time from the storage
 * they will be then ready to be played (sent to player)
 * 
 * @param timeMS
 * @return
 */
	private void getSegments(final long timeMS, final long perfectPlayTime) {

		if (logger.isDebugEnabled()) logger.debug("in getSegments("+timeMS+","+perfectPlayTime+")");
		
		synchronized(this.segments) {
			
			//verifies and deletes the ones we don't need anymore
			SegmentIdentifier segmentIdentifier;
			Iterator<SegmentIdentifier> iter = this.segments.keySet().iterator();
			
			while (iter.hasNext()) {
				segmentIdentifier= iter.next();

				if (!segmentIdentifier.getChannel().equals(this.channel)
					|| segmentIdentifier.getSegmentNumber() < SegmentIdentifier.getSegmentNumber(timeMS)
					|| segmentIdentifier.getSegmentNumber() > SegmentIdentifier.getSegmentNumber(perfectPlayTime))
					
					iter.remove();  //unschedules it
			}
			
			//potentially gets new ones
			if (logger.isDebugEnabled()) logger.debug("loading new segments to scheduler (keeping old ones)");
			
			for (long segmentNumber = SegmentIdentifier.getSegmentNumber(timeMS); segmentNumber <= SegmentIdentifier.getSegmentNumber(perfectPlayTime ); segmentNumber++) {
				for (byte substream = 0; substream < channel.getNumSubstreams(); substream++) {
					segmentIdentifier = new SegmentIdentifier(this.channel, substream, segmentNumber);
					
					if (!this.segments.keySet().contains(segmentIdentifier)) {
						Segment segment = this.segmentStorage.getSegment(segmentIdentifier);
						if (segment!=null)
							this.segments.put(segmentIdentifier, segment);
						else
							if (logger.isDebugEnabled()) logger.debug("segment not found on disk ["+segmentIdentifier+"]");
					}
				}
			}
		}
	}
	
	private Collection<PacketData> getMergedVideo(final long timeMS, final long perfectPlayTimeMs) {
		
		int blockNumber = SegmentBlock.getBlockNumber(timeMS);
		
		if (logger.isDebugEnabled()) logger.debug("in getMergedVideo(time:"+timeMS+", b#"+blockNumber+", #segments:"+segments.size()+")");
		
		this.playbackPolicy.clearWindow(timeMS, perfectPlayTimeMs);
		
		synchronized(this.segments) {
	
			Map<Long,PacketData> videoData = new TreeMap<Long, PacketData>();  //to put them in sequential order
			
			Segment segment; Iterator<Segment> iterSegments = this.segments.values().iterator();
			
			Long firstSequence = Long.MIN_VALUE, sequence;
			
			while (iterSegments.hasNext()) {
	
				segment = iterSegments.next();
				                    
				if (logger.isDebugEnabled()) logger.debug(" getting segment data: "+segment.toString());
				
				SegmentIdentifier segmentIdentifier = segment.getSegmentIdentifier();
				
				//if right channel and time
				if (segmentIdentifier.getChannel().equals(this.channel)
						&& segmentIdentifier.getSegmentNumber()==SegmentIdentifier.getSegmentNumber(timeMS)) {
					
					SegmentBlock segmentBlock = this.segmentStorage.getSegmentBlock(segmentIdentifier, blockNumber);
					if (segmentBlock!=null) {
						for (PacketData packet : segmentBlock.getPackets()) {
							sequence = packet.getTimeMS() + packet.getSequence();
							
							//orders them and adds to map that will be returned
							if (firstSequence == Long.MIN_VALUE)
								firstSequence = sequence;
							
							videoData.put(sequence-firstSequence, packet);
							
							//if (logger.isDebugEnabled()) logger.debug("ss:"+ packet.getDescription() + " seq:"+(packet.getSequence()-this.firstSequence));
							
						}
					}
					else {
						//at least a block is missing
						if (logger.isDebugEnabled()) logger.debug(" missing block for playback (t: "+timeMS+" b#"+SegmentBlock.getBlockNumber(timeMS)+" in "+segment.toString()+")");
						
						//reports which neighbors have that block (just for building a new playback policy)
						if (logger.isInfoEnabled()) {
							this.numNeighborsHaveBlock = this.liveShiftApplication.getTuner().getNeighborList().howManyNeighborsHaveBlock(segmentIdentifier, blockNumber);
							int numUsGrantersHaveBlock = this.liveShiftApplication.getTuner().getNeighborList().howManyUsGrantersHaveBlock(segmentIdentifier, blockNumber);
							logger.info("si:"+segmentIdentifier+" b#"+blockNumber+" is held by "+numUsGrantersHaveBlock+" USG and "+numNeighborsHaveBlock+" neighbors");
						}
					}
				}
				
				//also, looks ahead to see how much we have coming up -- this info will be useful for playback policy
				this.playbackPolicy.addBlockMap(segment);
			}
			
			//this.minimumDelay = perfectPlayTimeMs - this.playbackPolicy.getLatestPossiblePlayableTimeMillis();
			
			return new ArrayList<PacketData>(videoData.values());
		}
	}
	
	public void setPaused(boolean paused) {
		
		if (logger.isDebugEnabled()) logger.debug("setPaused("+paused+")");
		
		this.paused = paused;
		this.playClock.setPaused(paused);
	}
	public boolean isPaused() {
		return paused;
	}

	/**
	 * tries to send a block immediately to the player
	 */
	public void tryAndPlay() {
		if (this.playExecutorService!=null)
			this.playExecutorService.scheduleNow();
	}
	
	private void setStalled(boolean stalled) {
		
		if (logger.isDebugEnabled()) logger.debug("setStalled("+stalled+")");
		
		this.stalled = stalled;
		this.lastTimeStalled = Clock.getMainClock().getTimeInMillis(false);

		this.playClock.setPaused(stalled);
		if (!stalled)
			this.bursted = 0;
	}
	
	private boolean isStalled() {
		return stalled;
	}
	
	public long getLastPlayedBlocksStartTimeMs() {
		return this.lastPlayedBlocksStartTimeMs;
	}
	
	public boolean isCurrentRateEnough() {
		return this.lastTimeStalled <= Clock.getMainClock().getTimeInMillis(false) - 1000L;
	}
	
	public boolean isPlaybackFailed() {
		
		final int DONT_FAIL_IF_PLAYED_LAST = 5;
		
		int[] boxes = playbackAverage.getRawValidBoxes(1);
		if (logger.isDebugEnabled()) {
			String boxesAsString = "";
			for (int box : boxes)
				boxesAsString += box+":";
			logger.debug("window is: "+boxesAsString);
		}
		
		if (boxes.length<FAILED_PLAYBACK_WINDOW_BLOCKS) {
			return false;
		}
		
		int grader = this.playbackPolicy.getBlocksBuffered();
		int lastones=0;
		for (int i=0; i<boxes.length; i++) {
			grader += boxes[i]==0?-1:boxes[i];
			if (grader>0) {
				return false;
			}
			if (i >= boxes.length-DONT_FAIL_IF_PLAYED_LAST && boxes[i]>0) {
				lastones += boxes[i];
			}
		}
		
		//if played the last 5 blocks, don't fail playback, because a window of 0..01..1 was returning false
		if (lastones>=DONT_FAIL_IF_PLAYED_LAST) {
			if (logger.isDebugEnabled()) logger.debug("played last "+DONT_FAIL_IF_PLAYED_LAST+", not failing");
			return false;
		}
		
		if (logger.isDebugEnabled()) logger.debug("will return true, window is: "+playbackAverage);

		return true;
	}

	public long getPlayTime() {
		if (playClock==null) {
			return 0;
		}
		return playClock.getTimeInMillis(false);
	}

	public synchronized void relativeShift(int timeMillis) {
		logger.debug("relativeShift("+timeMillis+")");
		this.playClock.delay(-timeMillis, true);
	}
}
