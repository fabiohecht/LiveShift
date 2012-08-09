package net.liveshift.download;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.core.Channel;
import net.liveshift.core.ChannelSet;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.core.PeerId;
import net.liveshift.core.Stats;
import net.liveshift.p2p.DHTInterface;
import net.liveshift.p2p.PeerFailureNotifier;
import net.liveshift.p2p.P2PAddress;
import net.liveshift.signaling.VideoSignaling;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentBlockMap;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.storage.SegmentStorage;
import net.liveshift.time.Clock;
import net.liveshift.util.ExecutorPool;
import net.liveshift.util.MovingAverage;
import net.liveshift.video.VideoPlayer;

/**
 * Manages reception of a video stream. 
 * 
 * @author draft, Fabio Victora Hecht
 * @author Kevin Leopold
 * 
 */

public class Tuner implements IncomingBlockRateHandler, PeerFailureNotifier {
	final private static Logger logger = LoggerFactory.getLogger(Tuner.class);
	
	private final LiveShiftApplication liveShiftApplication;
	private final SegmentStorage segmentStorage;
	private final VideoSignaling videoSignaling;
	private final ChannelSet channelSet = new ChannelSet();
	
	private Channel	channel;
	private final NeighborList neighborList;
	private final Map<SegmentIdentifier, BlockScheduler> blockSchedulers = new ConcurrentHashMap<SegmentIdentifier, BlockScheduler>();  //TODO move it to TunedSegment
	private VideoPlayer videoPlayer;
	
	private BlockRequestQueue blockRequestQueue = new BlockRequestQueue();
	
	final private MovingAverage globalIncomingBlockRate = new MovingAverage(Clock.getMainClock(), 10, SegmentBlock.getBlocksPerSecond()*1000);
	
	public Tuner(final LiveShiftApplication liveShiftApplication)
	{
		if (logger.isDebugEnabled()) logger.debug("in constructor");
		
		this.liveShiftApplication = liveShiftApplication;
		this.segmentStorage = liveShiftApplication.getSegmentStorage();
		this.videoSignaling = liveShiftApplication.getVideoSignaling();
        this.neighborList = new NeighborList(this, liveShiftApplication.getConfiguration());
	}

	/**
	 * Switches to a channel/stream. This does query the DHT for peers that might have
	 * the data. Peers are queried on the DHT periodically.
	 * 
	 * @param channel The channel to watch
	 * @param timeshiftMS If timeshift is null, gets live video, otherwise starts
	 *        playing VoD
	 * @param ownChannel
	 */
	synchronized public void switchChannel(final Channel channel, final long timeshiftMS)
	{
		if (logger.isDebugEnabled()) logger.debug("switchChannel("+channel.toString()+","+timeshiftMS+")");
		
		//checks new interest
		Set<SegmentIdentifier> segmentIdentifiersToTune = new HashSet<SegmentIdentifier>(channel.getNumSubstreams());
		boolean interestChanged = false;

		for (byte substream = 0; substream < channel.getNumSubstreams(); substream++) {
			SegmentIdentifier segmentIdentifierToTune = new SegmentIdentifier(channel, substream, SegmentIdentifier.getSegmentNumber(timeshiftMS));
			segmentIdentifiersToTune.add(segmentIdentifierToTune);
			
			BlockScheduler blockScheduler = this.getBlockScheduler(segmentIdentifierToTune); 
			if (null==blockScheduler)
				interestChanged = true;
		}
		
		//cleans up
		this.disconnect(interestChanged);

		if (interestChanged)
			this.setChannel(channel);
		
		this.liveShiftApplication.setPlayTimeMs(timeshiftMS);  //this is just for the BlockScheduler to know where to start before the player is started (we might gain some milliseconds)
		
		//stats
		Stats stats = this.liveShiftApplication.getStats();
		if (stats!=null)
			stats.reset(channel.getNumSubstreams());
		
		
		this.setVideoPlayer(new VideoPlayer(this.segmentStorage, channel, timeshiftMS, this.liveShiftApplication));
		
		//schedules initial segments
		if (!channel.isOwnChannel()) {
			
			if (interestChanged)
				this.blockRequestQueue.initialize();
			
			for (final SegmentIdentifier segmentIdentifierToTune : segmentIdentifiersToTune) {
				Runnable runner = new Runnable() {
					@Override
					public void run() {
						try {
							createBlockScheduler(segmentIdentifierToTune, timeshiftMS);
						} catch (Exception e) {
							// just so it doesn't die silently if an unhandled exception happened
							logger.error("error running createBlockScheduler("+segmentIdentifierToTune+","+ timeshiftMS+"): "+e.getMessage() );

							e.printStackTrace();
						}

					}
				};
				ExecutorPool.getGeneralExecutorService().submit(runner);
			}
		}
		
		logger.info("switch channel:" + channel.getName() +"; timeshift:"+timeshiftMS);
	}

	public void disconnect(boolean interestChanged) {
		if (logger.isDebugEnabled()) logger.debug("disconnect("+interestChanged+")");
		
		if (interestChanged) {
			
			//remove all block schedulers
			this.cleanUpDeadBlockSchedulers(true);				
			
			//changes neighboring interest
			this.neighborList.reset();
			
			//clears queue
			this.blockRequestQueue.reset();
			
			//tells VectorHaveCollectors that probably new blocks aren't coming
			this.videoSignaling.getUploadSlotManager().notifyInterestChanged();
			
			this.channel = null;
		}			
	
		//kills videoPlayer
		if (this.getVideoPlayer()!=null) {
			this.getVideoPlayer().shutdown();
			this.setVideoPlayer(null);
		}
		
	}
	
	/**
	 * 
	 * @param segmentIdentifier
	 * @param timeshift: 0 here means the start of a segment
	 */
	private void createBlockScheduler(final SegmentIdentifier segmentIdentifier, final long timeshiftMS) {
		
		if (logger.isDebugEnabled()) logger.debug("createBlockScheduler("+segmentIdentifier.toString()+", "+timeshiftMS+")");
		
		//first looks in local storage
		//if it's FULLY available locally, no need to look for neighbors for this segment!
		//but only if it's fully available, that means, all the blocks
		
		Segment localSegment;
		if (timeshiftMS==0)
			localSegment = this.segmentStorage.getOrCreateSegment(segmentIdentifier, 0);
		else
			localSegment = this.segmentStorage.getOrCreateSegment(segmentIdentifier, SegmentBlock.getBlockNumber(timeshiftMS));
		
		//stats
		Stats stats = this.liveShiftApplication.getStats();
		if (stats!=null)
	        stats.loadSegment(localSegment);
		
		if (!localSegment.getSegmentIdentifier().getChannel().isOwnChannel()) {
			boolean findNeighbors;
			
			SegmentBlockMap segmentBlockMap = localSegment.getSegmentBlockMap();
			
			findNeighbors = !segmentBlockMap.isFull(SegmentBlock.getBlockNumber(timeshiftMS));
			
			//creates a block scheduler for this segment description
			BlockScheduler blockScheduler = this.getBlockScheduler(segmentIdentifier);
			if (null==blockScheduler) {
				//must create it even when !findNeighbors, since we don't know yet about the next segment
				blockScheduler = new BlockScheduler(localSegment, this.neighborList, this.liveShiftApplication);
				this.blockSchedulers.put(segmentIdentifier, blockScheduler);
			}
			
			if (findNeighbors) {
				
				//needs to schedule the first block(s) first, otherwise it loses a second
				int waitLimit = 15;   //*50 below = timeMillis
				while (!blockScheduler.firstBlockScheduled && waitLimit-->0) {
					try {
						logger.debug("waiting for first block to be scheduled");
						Thread.sleep(50);
					} catch (InterruptedException e) {
						logger.warn("interrupted ?!");
					}
				}
				
				//finds neighbors
				this.neighborList.addSegment(localSegment);
			
				//cleans up dead block schedulers
				this.cleanUpDeadBlockSchedulers(false);
				
				//cleans up old blocks that were skipped
				this.getBlockRequestQueue().cleanup(this.liveShiftApplication.getPlayTimeMillis());
				
			}
			else
				if (logger.isDebugEnabled()) logger.debug("!findNeighbors");
		}
		else
			if (logger.isDebugEnabled()) logger.debug("Channel().isOwnChannel()");
	}
	
	
	/**
	 * gets the next segment to the given one
	 */

	public void scheduleNextSegment(final SegmentIdentifier segmentIndentifier)
	{
		if (logger.isDebugEnabled()) logger.debug("will schedule a new Segment BlockScheduler (after "+segmentIndentifier.toString()+")");
		
		//adds new segments to the scheduler
		
		Runnable newBlockSchedulerRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					if (logger.isDebugEnabled()) logger.debug("scheduling a new Segment BlockScheduler (after "+segmentIndentifier.toString()+")");
					
					SegmentIdentifier segmentIdentifier = new SegmentIdentifier(segmentIndentifier.getChannel(),segmentIndentifier.getSubstream(),segmentIndentifier.getSegmentNumber()+1);
					createBlockScheduler(segmentIdentifier, 0);

				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					e.printStackTrace();
				}

			}
		};
		//it delays the start because there was a bug that if the video started after the threshold for
		//requesting the next segment, it would request all upload slots with no particular order, 
		//and many times it would get only the latter one granted
		ExecutorPool.getScheduledExecutorService().schedule(newBlockSchedulerRunnable, 750, TimeUnit.MILLISECONDS);
	}
	

	private void setChannel(Channel channel)
	{
		this.channel = channel;
	}

	public Channel getChannel() {
		return channel;
	}
	
	public BlockScheduler getBlockScheduler(final SegmentIdentifier segmentIdentifier) {
		if (this.blockSchedulers==null || segmentIdentifier==null)
			return null;
		else {
			BlockScheduler blockScheduler = this.blockSchedulers.get(segmentIdentifier);
			if (blockScheduler==null)
				return null;
			else 
				return blockScheduler.isRunning()?blockScheduler:null;
		}
	}
	
	private void cleanUpDeadBlockSchedulers(final boolean killEmAll) {
	
		if (logger.isDebugEnabled()) logger.debug("cleanUpDeadBlockSchedulers("+ killEmAll+")");
		
			
		if (this.blockSchedulers!=null) {
			Iterator<BlockScheduler> iter = this.blockSchedulers.values().iterator();
			BlockScheduler blockScheduler;
			while (iter.hasNext()) {
				blockScheduler = iter.next();
				
				if (killEmAll && blockScheduler!=null) {
					blockScheduler.shutdown();
					iter.remove();
				}
				else if (blockScheduler==null || !blockScheduler.isRunning())	
					iter.remove();
			}
		}
		if (logger.isDebugEnabled()) logger.debug("done cleanUpDeadBlockSchedulers");

	}

	private void setVideoPlayer(VideoPlayer videoPlayer) {
		this.videoPlayer = videoPlayer;
	}

	public VideoPlayer getVideoPlayer() {
		return videoPlayer;
	}

	public NeighborList getNeighborList() {
		return this.neighborList;
	}
	
	BlockRequestQueue getBlockRequestQueue() {
		return blockRequestQueue;
	}

	public SegmentBlockMap getSegmentBlockMap(final SegmentIdentifier segmentIdentifier) {
		Segment segment = this.segmentStorage.getSegment(segmentIdentifier);
		if (segment!=null)
			return segment.getSegmentBlockMap();
		else
			return new SegmentBlockMap();
	}
	
	public VideoSignaling getVideoSignaling() {
		return this.videoSignaling;
	}

	public boolean putSegmentBlock(final PeerId sender, final SegmentBlock segmentBlock) {
		
		this.blockRequestQueue.remove(new BlockRequest(segmentBlock.getSegmentIdentifier(), segmentBlock.getBlockNumber()));

		this.neighborList.blockDownloaded(sender, segmentBlock.getSegmentIdentifier(), segmentBlock.getBlockNumber());
		
		//stats
		Stats stats = this.liveShiftApplication.getStats();
		if (stats!=null)
			stats.blockStatChange(segmentBlock.getSegmentIdentifier(), segmentBlock.getBlockNumber(), 1);
		
		if (this.segmentStorage.putSegmentBlock(segmentBlock)) {
			this.incrementIncomingBlockRate();
			
			return true;
		}
		return false;
	}

	public void disconnectPeer(PeerId peerId, SegmentIdentifier segmentIdentifier) {
		//kills downloads
		this.neighborList.removeFromAll(peerId, segmentIdentifier);
	}

	@Override
	public void signalFailure(P2PAddress p2pAddress) {
		this.neighborList.signalFailure(p2pAddress);
	}

	/**
	 * Processes a Have message (update to a block map of a neighbor)
	 * 
	 * @param peerId
	 * @param segmentIdentifier
	 * @param blockNumber
	 * @param doHave 
	 * @param rate expected rate at which new blocks are expected to arrive from blockNumber. -1=don't change the last prediction, 0=clear last prediction, >0=update the last prediction 
	 */
	public void updateNeighborBlockMap(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final int blockNumber, final boolean doHave, final float rate) {

		if (logger.isDebugEnabled()) logger.debug("updateNeighborBlockMap("+peerId+","+segmentIdentifier+","+blockNumber+","+doHave+","+rate+")");

		//updates block map in NeighborList
		this.neighborList.updateBlockMap(peerId, segmentIdentifier, blockNumber, doHave, rate);
		
		this.maybeScheduleBlock(peerId, segmentIdentifier, blockNumber, doHave);
	}
	
	public void maybeScheduleBlock(final PeerId peerId, final SegmentIdentifier segmentIdentifier, final int blockNumber, final boolean doHave) {

		//maybe (cancel fetching the block and) schedule the new block
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				try {
					
					if (!doHave) {
						//TODO maybe in the future... not clear advantage yet seen
						
						//cancels it because it may be in the queue on the neighbor's side, and the neighbor will send us a Have when he has the block anyway
						//neighborList.cancelBlockRequest(segmentIdentifier, blockNumber);
					}
					
					BlockScheduler blockScheduler = blockSchedulers.get(segmentIdentifier);
					
					if (blockScheduler==null || !blockScheduler.isRunning()) {
						logger.warn("blockScheduler expired");
						
						return;
					}
					
					blockScheduler.scheduleNow();

					//maybe became interested because of this block
					neighborList.checkInterestAndSendInterested(peerId, segmentIdentifier);
					
					//signals block queue, since it could be already scheduled but waiting for a peer to announce having this block
					blockRequestQueue.notifyLock();
					
				} catch (Exception e) {
					// just so it doesn't die silently if an unhandled exception happened
					logger.error("error running blockScheduler.scheduleNow: "+e.getMessage() );
					e.printStackTrace();
				}
			}
		};
		
		ExecutorPool.getGeneralExecutorService().submit(runner);
	}
	
	long getPlayTimeMs() {
		return this.liveShiftApplication.getPlayTimeMillis();
	}

	public SegmentStorage getSegmentStorage() {
		return this.segmentStorage;
	}

	public LiveShiftApplication getApplication() {
		return this.liveShiftApplication;
	}

	public Channel getChannelById(int channelId)
	{
		synchronized (this.channelSet) {
			return this.channelSet.getById(channelId);
		}
	}
	
	public ChannelSet getChannelSet()
	{
		return this.channelSet;
	}

	public Map<Integer, Channel> getChannelIdMap() {
		return this.channelSet.getChannelIdMap();
	}
	
	public Collection<Channel> getChannelsByTags(String[] strings) {
		return this.channelSet.getByTags(strings);
	}
	
	public void addChannel(Channel channel) {
		synchronized (this.channelSet) {
			this.channelSet.add(channel);
		}
	}

	void checkAndRemoveOldTunedSegment(final SegmentIdentifier segmentIdentifier) {
		this.neighborList.checkAndRemoveOldTunedSegment(segmentIdentifier);
	}

	public DHTInterface getDht() {
		return this.liveShiftApplication.getDht();
	}
	
	public boolean isPlaybackFailed() {
		if (this.videoPlayer==null)
			return false;
		else
			return this.videoPlayer.isPlaybackFailed();
	}

	public void rewind(int timeMillis) {
		//TODO schedule new blocks that might have been skipped? probably
		
		this.videoPlayer.relativeShift(-timeMillis);
		this.videoPlayer.tryAndPlay();
	}

	public void fastForward(int timeMillis) {
		//TODO skip some blocks? probably unnecessary
		
		this.videoPlayer.relativeShift(timeMillis);
		this.videoPlayer.tryAndPlay();
	}

	public long getPlayTime() {
		if (this.videoPlayer==null) {
			return 0;
		}
		else {
			return this.videoPlayer.getPlayTime();
		}
	}

	@Override
	public void incrementIncomingBlockRate() {
		this.globalIncomingBlockRate.inputValue(1);
	}

	@Override
	public float getIncomingBlockRate() {
		return this.globalIncomingBlockRate.getAverage();
	}

	public void clearChannelSet() {
		this.channelSet.clear();
	}
}