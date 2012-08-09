package net.liveshift.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.liveshift.core.Channel;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.upload.UploadSlotManager;
import net.liveshift.video.PacketData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author Fabio Victora Hecht
 */
public class SegmentAssembler {

	final private static Logger logger = LoggerFactory.getLogger(SegmentAssembler.class);

	private static final long	TIME_TO_PRECREATE_SEGMENT_MS = 5000;

	//must be set; if changed, new object must be created
	private boolean isInitialized = false;
	private Channel channel;
	private SegmentStorage segmentStorage;  //where segments are stored once they are complete
	
	//internal storage (buffers)
	private long startTimeMS;  //start time of the assembler
	private Map<Byte,List<PacketData>> currentBlocksData;  //is built with packets received, forming blocks (map has substream, list has packets)
	private long[] currentBlocksDataStartTimeMS;
	private Segment[] currentSegments;  //current segment being filled (only dimension is substream)
	
	//to send HAVE messages
	UploadSlotManager uploadSlotManager;
	
	//just for debug info
	private long newestTime;

	private boolean[] nextSegmentCreated; //one per substream

	public SegmentAssembler() {

		if (logger.isDebugEnabled()) logger.debug("in constructor()");
		
	}
	public void initialize (Channel channel, SegmentStorage segmentStorage, UploadSlotManager uploadSlotManager) {
		//define what those packets are about and where they should go
		//without calling this, the class will just thrash the packets
		
		if (logger.isDebugEnabled()) logger.debug("in initialize("+channel.toString()+","+segmentStorage.toString()+")");
		
		//clears buffers
		if (this.isInitialized)
			this.currentBlocksData = null;
		
		this.channel = channel;
		this.segmentStorage = segmentStorage;
		
		this.currentBlocksData = new TreeMap<Byte,List<PacketData>>();
		this.currentBlocksDataStartTimeMS = new long[this.channel.getNumSubstreams()];
		this.currentSegments = new Segment[this.channel.getNumSubstreams()];
		this.startTimeMS = Long.MIN_VALUE;
		this.nextSegmentCreated = new boolean[this.channel.getNumSubstreams()];

		this.uploadSlotManager = uploadSlotManager;
		
		this.isInitialized = true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see csg.liveshift.dht.SegmentStorage#putPacket(byte[])
	 * 
	 * receives one packet with video
	 * has an internal buffer
	 * outputs blocks and segments to the SegmentStorage
	 * 
	 */	
	public boolean putPacket(PacketData packetData) {

		if (logger.isDebugEnabled()) logger.debug("in putPacket("+packetData.toString()+")");
		
		if (!this.isInitialized) {
			SegmentAssembler.logger.error("SegmentAssembler needs to be initialized first.");
			return false;
		}
		
		//checks whether it must start a new block for this packet
		byte substream = packetData.getSubstream();
		long packetTime = packetData.getTimeMS();
		
		List<PacketData> currentBlockData;
		
		if (logger.isDebugEnabled()) logger.debug("substream="+packetData.getSubstream()+", packetTime="+packetTime+", seq="+packetData.getSequence());
		
		currentBlockData = this.currentBlocksData.get(substream);
		
		if (currentBlockData==null) {
			//the birth of a new block
			this.currentBlocksDataStartTimeMS[substream] = SegmentBlock.getStartTimeMillis(packetTime);
			currentBlockData = new ArrayList<PacketData>();
			this.currentBlocksData.put(substream,currentBlockData);
			
			if (this.startTimeMS == Long.MIN_VALUE)
				this.startTimeMS = this.currentBlocksDataStartTimeMS[substream];
			
			if (logger.isDebugEnabled()) logger.debug("created new block: startTime="+this.startTimeMS+", substream="+substream);
		}
		
		if (this.currentBlocksDataStartTimeMS[substream] < SegmentBlock.getStartTimeMillis(packetTime)) {
			//if this packet belongs to another block
			
			//outputs block to the segment
			this.putBlock(substream);
			
			//creates a new one
			this.currentBlocksDataStartTimeMS[substream] = SegmentBlock.getStartTimeMillis(packetTime);
			currentBlockData =new ArrayList<PacketData>();
			this.currentBlocksData.put(substream,currentBlockData);
			
		}

		//for the GUI
		this.newestTime = packetData.getTimeMS();

		//adds packet to the block
		return currentBlockData.add(packetData);
	}
	
	private boolean putBlock(byte substream) {

		if (logger.isDebugEnabled()) logger.debug("in putBlock(ss:"+substream+")");
		
		//checks whether it must start a new segment for this block
		long blockTime = this.currentBlocksDataStartTimeMS[substream];
		Segment thisSegment = this.currentSegments[substream];

		if (thisSegment==null) {
			//first block of first segment
			
			//new segment (starting from this block)
			thisSegment = this.currentSegments[substream] = this.getOrCreateSegment(substream, blockTime, true);
		}
		else {
			
			//checks if there would have been empty blocks before this one (could happen
			this.checkFormerBlocks(thisSegment, blockTime);
			
			//subsequent segments - a segment exists
			if (thisSegment.getSegmentIdentifier().getSegmentNumber() !=  SegmentIdentifier.getSegmentNumber(blockTime)) {
				//first block of a new segment
				
				//calculates score
				//if (this.scoreHandler!=null)
				//	this.scoreHandler.calculateScore(thisSegment);
				
				//new segment (starting from block 0)
				Segment oldSegment = thisSegment;
				thisSegment = this.currentSegments[substream] = this.getOrCreateSegment(substream, blockTime, false);

				//checks if there would have been empty blocks before this one in the new segment and possibly all segments in between
				for (long segmentNumber = oldSegment.getSegmentIdentifier().getSegmentNumber()+1; segmentNumber < thisSegment.getSegmentIdentifier().getSegmentNumber(); segmentNumber++)
					this.checkFormerBlocks(this.getOrCreateSegment(substream, segmentNumber), blockTime); //the ones in between (only happens if streaming was stopped for a full segment)
				this.checkFormerBlocks(thisSegment, blockTime); //the new one

				this.nextSegmentCreated[substream] = false;
			}
			else if (!this.nextSegmentCreated[substream] && thisSegment.getSegmentIdentifier().getEndTimeMS() - blockTime < TIME_TO_PRECREATE_SEGMENT_MS) {
				
				if (logger.isDebugEnabled()) logger.debug("precreating next segment (after "+thisSegment+")");
				
				//creates the next segment a bit ahead of time (so other peers get ready quicker to make the transition)
				this.getOrCreateSegment(substream, thisSegment.getSegmentIdentifier().getEndTimeMS()+1, false);
				
				this.nextSegmentCreated[substream] = true;
			}
			
		}
		
		//creates a block with the data
		List<PacketData> list = this.currentBlocksData.get(substream);
		PacketData[] packetDataArray = list.toArray(new PacketData[list.size()]);
		SegmentBlock currentBlock = new SegmentBlock(thisSegment.getSegmentIdentifier(), blockTime, packetDataArray);
		
		return this.storeBlock(currentBlock);
	}
	
	private boolean storeBlock(final SegmentBlock currentBlock) {
		//adds block to the segment
		boolean success = this.segmentStorage.putSegmentBlock(currentBlock);
		
		if (success) {
			
			//I had to put this in a new thread because it was taking a long time (probably because of the synchronized) and slowing down the whole segment assembler, basically bringing down streaming completely
			Thread haveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
	
						//sends HAVE messages
						uploadSlotManager.sendHave(currentBlock.getSegmentIdentifier(), currentBlock.getBlockNumber());
						
					} catch (Exception e) {
						// just so it doesn't die silently if an unhandled exception happened
						e.printStackTrace();
					}

				}
			});
			haveThread.start();
			
			/*
			 * removed because the stats window currently displays what is being played, not encoding, it would be mixing up 2 things, and we don't have play position
			 * 
			//stats
			try {
				Application.getApplication().getStatListener().blockStatChange(currentBlock.getSegmentIdentifier(), currentBlock.getBlockNumber(), 1);
			}
			catch (Exception e) {
				logger.error("Error updating stats: "+e.getMessage());
			}
			*/
		}
		
		return success;
	}
	
	/*
	 * checks if former blocks are present
	 * 
	 * @param thisSegment 
	 * @param blockTime 
	 */
	private void checkFormerBlocks(Segment thisSegment, long blockTimeMS) {

		//makes sure there are no missing blocks before this
		int firstBlockInThisSegment, lastBlockInThisSegment;

		if (SegmentIdentifier.getSegmentNumber(this.startTimeMS)==thisSegment.getSegmentIdentifier().getSegmentNumber())
			firstBlockInThisSegment = SegmentBlock.getBlockNumber(this.startTimeMS);
		else
			firstBlockInThisSegment = 0;
		
		if (thisSegment.getSegmentIdentifier().getSegmentNumber() ==  SegmentIdentifier.getSegmentNumber(blockTimeMS))
			lastBlockInThisSegment = SegmentBlock.getBlockNumber(blockTimeMS) - 1;
		else
			lastBlockInThisSegment = thisSegment.getSegmentBlockMap().getLength() - 1;
		
		//checks all substreams of past blocks that are absent
		for (int block = lastBlockInThisSegment; block>=firstBlockInThisSegment; block--) {
			
		
			if (thisSegment.getSegmentBlockMap().get(block))
				break;
			else {
				//creates a block with empty data
				SegmentBlock currentBlock = new SegmentBlock(thisSegment.getSegmentIdentifier(), block);
				if (logger.isDebugEnabled()) logger.debug("had to create an empty b#"+block+" in segment si="+thisSegment.getSegmentIdentifier());
						
				//adds empty block to the segment
				this.storeBlock(currentBlock);

			}
		}

	}
	
	private Segment getOrCreateSegment(byte substream, long timestamp, boolean isFirstSegment) {
		
		SegmentIdentifier segmentIdentifier = new SegmentIdentifier(this.channel,substream,SegmentIdentifier.getSegmentNumber(timestamp));
		Segment output = this.segmentStorage.getOrCreateSegment(segmentIdentifier, isFirstSegment?timestamp:0);
		
		return output;
	}

	private Segment getOrCreateSegment(byte substream, long segmentNumber) {

		SegmentIdentifier segmentIdentifier = new SegmentIdentifier(this.channel,substream,segmentNumber);
		Segment output = this.segmentStorage.getOrCreateSegment(segmentIdentifier, 0);
		
		return output;
	}
	
	//debug functions
	public long getCurrentSegmentNumber() {
		if (this.isInitialized && this.currentSegments.length>0 && this.currentSegments[0]!=null)
			return this.currentSegments[0].getSegmentIdentifier().getSegmentNumber();
		else
			return 0L;
	}
	public long getNewestTime() {
		return this.newestTime;
	}

}
