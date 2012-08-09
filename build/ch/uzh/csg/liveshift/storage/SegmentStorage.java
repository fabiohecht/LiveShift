package net.liveshift.storage;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.LiveShiftApplication;
import net.liveshift.download.Tuner;
import net.liveshift.download.Tuner;
import net.liveshift.p2p.DHTInterface;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.util.ExecutorPool;

/**
 *
 * @author Fabio Victora Hecht
 */
public class SegmentStorage {

	final private static Logger logger = LoggerFactory.getLogger(SegmentStorage.class);
	//final private static Logger logger = LiveShiftApplication.getLogger(SegmentStorage.class);

	// configuration parameters
	private static final int MAX_BLOCKS_ON_DISK = 120*(60*1000)/(int)Configuration.SEGMENTBLOCK_SIZE_MS;  //120 minutes if 1 substream TODO how to take into account that different channels have different number of substreams?
	private static final int CACHE_SIZE_BLOCKS = SegmentBlock.getBlocksPerSegment()*5;
	private static final int INDEX_SAVE_FREQUENCY = 30000;
	private final String storageDirectory;
	
	private Tuner tuner;
	
	// internal storage
	protected ConcurrentSkipListMap<SegmentIdentifier, Segment> index;  //disk storage index
	private final SoftReference<SegmentBlock>[] cache;
	private AtomicInteger blocksOnDisk = new AtomicInteger(0);

	//interfaces
	private final DHTInterface dht;
	
	private Runnable indexSaver = new Runnable() {
		
		@Override
		public void run() {
			try {

				if (blocksOnDisk.get() > MAX_BLOCKS_ON_DISK) {
					if (logger.isDebugEnabled()) logger.debug("too many blocks on disk ("+blocksOnDisk+"/"+MAX_BLOCKS_ON_DISK+"), will delete 10% of blocks stored");
					deleteBlocks((int)(blocksOnDisk.get()*.1));
				}
				
				saveIndex();
			}
			catch (Exception e) {
				logger.error("error in indexSaver: "+e.getMessage());
				e.printStackTrace();
			}
		}
	};
	private final ScheduledFuture<?> scheduleIndexSaver;

	public SegmentStorage(DHTInterface dht, String storageDirectory) {
		
		if (logger.isDebugEnabled()) logger.debug("in constructor");
		
		this.dht = dht;
		this.storageDirectory = storageDirectory;

		// gets index from disk
		try {
			this.loadIndex();
		} catch (IOException e) {
			logger.error("i/o error reading index - new one created");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		this.index = new ConcurrentSkipListMap<SegmentIdentifier,Segment>();
		this.cache = new SoftReference[CACHE_SIZE_BLOCKS];
		
		this.scheduleIndexSaver = ExecutorPool.getScheduledExecutorService().scheduleAtFixedRate(indexSaver, INDEX_SAVE_FREQUENCY, INDEX_SAVE_FREQUENCY, TimeUnit.MILLISECONDS);

	}

	//removes a number of segments enough to remove at least the number of blocks provided
	protected void deleteBlocks(int numberOfBlocksToRemove) {
		
		if (logger.isDebugEnabled()) logger.debug("about to try and delete "+numberOfBlocksToRemove+" blocks");

		int blocksRemoved=0;
		
		long playbackPosition = this.tuner.getVideoPlayer().getLastPlayedBlocksStartTimeMs();
		Set<Segment> segmentsToDelete = new HashSet<Segment>(); 
		
		//first, deletes blocks from oldest segments before playback position

		Iterator<Entry<SegmentIdentifier, Segment>> iter = this.index.entrySet().iterator();
		
		while (iter.hasNext() && blocksRemoved < numberOfBlocksToRemove) {
			
			Entry<SegmentIdentifier, Segment> mapEntry = iter.next();
			Segment segment = mapEntry.getValue();
			
			if (segment.getSegmentIdentifier().getEndTimeMS() < playbackPosition) {
				int blocksPresent = segment.getSegmentBlockMap().cardinality();
				blocksRemoved += blocksPresent;
			}
		}
		blocksOnDisk.addAndGet(-blocksRemoved);
		
		//then, (if necessary, hope not), deletes blocks from newest segments after playback position (but newest = rarest)
		iter = this.index.descendingMap().entrySet().iterator();
		
		while (iter.hasNext() && blocksRemoved < numberOfBlocksToRemove) {
			
			Entry<SegmentIdentifier, Segment> mapEntry = iter.next();
			Segment segment = mapEntry.getValue();
			
			if (segment.getSegmentIdentifier().getEndTimeMS() > playbackPosition) {
				int blocksPresent = segment.getSegmentBlockMap().cardinality();
				blocksRemoved += blocksPresent;
			}
		}
		blocksOnDisk.addAndGet(-blocksRemoved);

		//really deletes them

		for (Segment segment : segmentsToDelete) {
			if (logger.isDebugEnabled()) logger.debug("removing segment: "+segment);
			
			this.deleteSegmentFromDisk(segment);
			iter.remove();
			/*
			try {
				this.tuner.getDht().unPublishSegment(segment.getSegmentIdentifier());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
		
		if (logger.isDebugEnabled()) logger.debug("deleted "+blocksRemoved+" blocks");

	}

	public void shutdown() {
		if (logger.isDebugEnabled()) logger.debug("in shutdown()");
		
		// saves index to disk
		this.scheduleIndexSaver.cancel(false);
		this.saveIndex();
	}

	private void saveIndex() {

		if (logger.isDebugEnabled()) logger.debug("saving index()");
		
		try {
			FileOutputStream fos = new FileOutputStream(storageDirectory + "/index");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			synchronized (index) {
				oos.writeObject(index);
			}
			
			oos.close();
			fos.close();
		}
		catch (Exception e) {
			logger.error("ERROR at saveIndex: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public synchronized Segment getSegment(final SegmentIdentifier segmentIdentifier) {

		if (logger.isDebugEnabled()) logger.debug("in getSegment("+segmentIdentifier.toString()+")");
		
		// checks index
		Segment segment = this.index.get(segmentIdentifier);
		
		return segment;
	}

	public Segment getOrCreateSegment(SegmentIdentifier segmentIdentifier, long startBlockTime) {
		
		if (startBlockTime > 0 && !segmentIdentifier.timeBelongsHere(startBlockTime)) {
			logger.error("given startBlockTime ("+startBlockTime+") does not belong in given segmentIdentifier ("+segmentIdentifier+")");
			LiveShiftApplication.quit("given startBlockTime ("+startBlockTime+") does not belong in given segmentIdentifier ("+segmentIdentifier+")");
		}
		
		return this.getOrCreateSegment(segmentIdentifier, SegmentBlock.getBlockNumber(startBlockTime));
	}
	public synchronized Segment getOrCreateSegment(SegmentIdentifier segmentIdentifier, int startBlockNumber) {
		
		if (logger.isDebugEnabled()) logger.debug("in getOrCreateSegment("+segmentIdentifier.toString()+", startBlock#"+startBlockNumber+")");
		
		// checks index
		Segment segment = this.index.get(segmentIdentifier);
		
		if (segment==null) {
			//if not here, gives a new one
			segment = new Segment(segmentIdentifier, startBlockNumber);
			if (logger.isDebugEnabled()) logger.debug("new segment created");
			
			this.putSegment(segment);
		}
		else if (startBlockNumber != segment.getSegmentBlockMap().getStartBlockNumber()) {
			segment.getSegmentBlockMap().setStartBlockNumber(startBlockNumber);
			if (logger.isDebugEnabled()) logger.debug("existing segment taken, startBlockNumber set to "+startBlockNumber);
			
			this.putSegment(segment);
		}
		else {
			if (logger.isDebugEnabled()) logger.debug("existing segment taken, startBlockNumber matches");
		}
		
		return segment;
	}

	/**
	 * 
	 * @throws IOException 
	 * @see csg.liveshift.dht.SegmentStorage#putSegment(csg.liveshift.Segment)
	 * 
	 * puts a new segment in the disk storage
	 * 
	 */
	
	private void putSegment(Segment segment) {
		
		if (logger.isDebugEnabled()) logger.debug("in putSegment("+segment.toString()+")");

		// adds it to memory buffer
		this.index.put(segment.getSegmentIdentifier(), segment);
		
		//announces availability on DHT
		this.announceOnDht(segment.getSegmentIdentifier());

	}

	
	public synchronized void announceAllOnDht() {
		this.announceOnDht(this.index);
	}
	private void announceOnDht(Map<SegmentIdentifier, Segment> map) {
		
		if (!this.dht.isConnected())
			return;

		synchronized (this.index) {
			for (SegmentIdentifier segmentIdentifier : this.index.keySet())
				this.announceOnDht(segmentIdentifier);
		}
		
	}
	private void announceOnDht(SegmentIdentifier segmentIdentifier) {
		
		if (logger.isDebugEnabled()) logger.debug("announcing si on DHT ("+segmentIdentifier+")");
		
		if (!this.dht.isConnected())
			return;
		
		try {
			this.dht.publishSegment(segmentIdentifier);
		} catch (IOException e) {
			logger.error("error annnouncing segment on DHT: "+e.getMessage());
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadIndex() throws IOException, ClassNotFoundException {
		
		if (logger.isDebugEnabled()) logger.debug("in loadIndex");
		
		String fileName = this.storageDirectory + "/index";
		File file = new File(fileName);
		boolean exists = file.exists();
		
		if (!exists) {
			

			boolean success;
			
			//creates directory if needed
			this.mkdirs(this.storageDirectory);
			
			//creates it
			success = file.createNewFile();
			
			if (logger.isDebugEnabled()) logger.debug("new index created");
			
			if (!success) {
				SegmentStorage.logger.error("ERROR creating new index file");
			}
			
			this.index = new ConcurrentSkipListMap<SegmentIdentifier, Segment>();
			
			return;
		}
		
		ObjectInputStream ois;

		try {
			FileInputStream fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);

			this.index = (ConcurrentSkipListMap<SegmentIdentifier, Segment>) ois.readObject();
			
			if (logger.isDebugEnabled()) logger.debug("existing index loaded");
			
			ois.close();
			fis.close();
		}
		catch (EOFException e) {
			logger.error("error loading index: "+e.getMessage());

			this.index = new ConcurrentSkipListMap<SegmentIdentifier, Segment>();
		}
		catch (ClassCastException e) {
			logger.error("error loading index, the type is incompatible: "+e.getMessage());
			
			this.index = new ConcurrentSkipListMap<SegmentIdentifier, Segment>();
		}
		
		//announces availability on DHT
		this.announceOnDht(this.index);
		
	}
	
	private boolean mkdirs(String dirname) {
		
		boolean success = false;
		
		try {
			// Create multiple directories
			success = (new File(dirname)).mkdirs();
			if (success)
				if (logger.isDebugEnabled()) logger.debug("Directory (" + dirname + ") created");
		}
		catch (Exception e) {//Catch exception if any
			SegmentStorage.logger.error("ERROR creating directory (" + dirname + "): " + e.getMessage());
			return false;
		}
		
		return success;
	}

	private int getIndexCount() {
		return this.index.size();
	}

	private synchronized void deleteSegmentFromDisk(Segment segment) {
		
		String filename = segment.getSegmentIdentifier().getStringIdentifier();
		
		if (logger.isDebugEnabled()) logger.debug("in deleteSegmentFromDisk("+filename+")");
		
		//deletes all blocks
		SegmentBlockMap segmentBlockMap = segment.getSegmentBlockMap();
		int index = -1;
		while (-1 != (index = segmentBlockMap.nextSetBit(index+1))) {
			try {
				this.deleteBlock(segment.getSegmentIdentifier(), index);
				segmentBlockMap.clear(index);
			}
			catch (IllegalArgumentException e) {
				logger.error("error deleting block ("+segment.getSegmentIdentifier()+","+ index+"): "+e.getMessage());
			}
		}
	}


	public synchronized boolean putSegmentBlock(final SegmentBlock segmentBlock) {
		
		logger.info("in putSegmentBlock("+segmentBlock.toString()+")");
		
		//sanity check
		if (segmentBlock.getBlockNumber() >= SegmentBlock.getBlocksPerSegment()) {
			logger.error("trying to putSegmentBlock with blockNuber too high ("+segmentBlock.toString()+")");
			return false;
		}
		
		SegmentIdentifier segmentIdentifier = segmentBlock.getSegmentIdentifier();
		Segment segment = this.getSegment(segmentIdentifier);
		
		if (segment==null) {
			logger.warn("received a block on an inexisting segment, I'm not storing this (could be a late blockReply after interest change)");
			return false;
		}
		
		//checks bitmap if already had this block
		synchronized (this) {
			boolean blockPresent;
			try {
				blockPresent = segment.getSegmentBlockMap().get(segmentBlock.getBlockNumber());
			}
			catch (IndexOutOfBoundsException iobe) {
				blockPresent = false;
			}
			
			if (blockPresent) {
				logger.info("duplicate block received ("+segmentBlock.toString()+")");  //for the statistics
				logger.warn(" block is already present, according to bitmap ("+segmentBlock.toString()+"), I'm not storing this");
				return false;
			}
			
			if (segment.getSegmentIdentifier().blockBelongsHere(segmentBlock)) {
				
				//adds block to cache
				this.putInCache(segmentBlock);
				
				//adds block to storage
				if (!this.writeBlock(segmentBlock)) {
					logger.error("error writing segmentBlock "+segmentBlock+" -- disk full? permissions?");
				}
				else {
					//updates bitmap
					segment.getSegmentBlockMap().set(segmentBlock.getBlockNumber());
				}
			}
			else {
				logger.error("look, I fetched the wrong segment! I'm confused -- your code is buggy (bn="+segmentBlock.getBlockNumber()+" || s.si="+segment.getSegmentIdentifier()+" || b.si="+segmentBlock.getSegmentIdentifier()+" || b.gt="+segmentBlock.getTimeMillis()+" || si.gsn(b.gt)="+SegmentIdentifier.getSegmentNumber(segmentBlock.getTimeMillis())+")");
				/*
				//debug
				if (segmentBlock.getSegmentIdentifier().getSubstream()==0) {
					if (logger.isDebugEnabled()) logger.debug("b sn="+segmentBlock.getSegmentIdentifier().getSegmentNumber()+" bn="+segmentBlock.getBlockNumber()+" t="+segmentBlock.getTime());
					if (logger.isDebugEnabled()) logger.debug("s sn="+segment.getSegmentIdentifier().getSegmentNumber()+" | SI sn(b.t)="+SegmentIdentifier.getSegmentNumber(segmentBlock.getTime()));
				}
				*/
				return false;
			}
	
			this.blocksOnDisk.incrementAndGet();
			
			return true;
		}
	}
	
	private boolean writeBlock(SegmentBlock segmentBlock)  {

		SegmentIdentifier segmentIdentifier = segmentBlock.getSegmentIdentifier();
		
		File file = segmentIdentifier.getFile(storageDirectory, segmentBlock.getBlockNumber());
		
		if (logger.isDebugEnabled()) logger.debug("in writeBlock("+file.getPath()+")");
		
		//creates directory if necessary
		this.mkdirs(file.getParent());
		
		try {
			FileOutputStream fos = new FileOutputStream(file.getPath());
			ObjectOutputStream oos = new ObjectOutputStream(fos);
	
			oos.writeObject(segmentBlock);
	
			oos.close();
			fos.close();
		}
		catch (Exception e) {
			logger.error("ERROR writing block file " + file.getPath() + ": " + e.getMessage());
			//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
			return false;
		}
		
		return true;
	}

	public synchronized SegmentBlock getSegmentBlock(SegmentIdentifier segmentIdentifier, int blockNumber) {

		if (logger.isDebugEnabled()) logger.debug("in getSegmentBlock("+segmentIdentifier+","+blockNumber+")");
		
		// sanity check
		if (blockNumber >= SegmentBlock.getBlocksPerSegment()) {
			logger.error("trying to getSegmentBlock with blockNuber too high ("
					+ blockNumber + ")");
			return null;
		}
		
		//first checks index
		Segment segment = this.getSegment(segmentIdentifier);
		
		if (segment==null || !segment.getSegmentBlockMap().get(blockNumber)) {
			if (logger.isDebugEnabled()) logger.debug("segment block not found on index ("+segmentIdentifier+","+blockNumber+")");
			return null;
		}
		
		//if it's present in the index, we should have the file
		//tries to get from cache
		SegmentBlock segmentBlock=this.getFromCache(segmentIdentifier, blockNumber);
		if (segmentBlock!=null) {
			if (logger.isDebugEnabled()) logger.debug("hit cache");
			return segmentBlock;
		}
		else
			if (logger.isDebugEnabled()) logger.debug("missed cache");

		
		//gets from disk
		File file = segmentIdentifier.getFile(this.storageDirectory, blockNumber);
		
		if (logger.isDebugEnabled()) logger.debug("in readBlock("+file.getPath()+")");

		FileInputStream fis;
		try {
			fis = new FileInputStream(file.getPath());
			ObjectInputStream ois = new ObjectInputStream(fis);
	
			segmentBlock = (SegmentBlock) ois.readObject();
	
			ois.close();
			fis.close();
			
		} catch (Exception e) {
			logger.error("ERROR reading block file " + file.getPath() + ": " + e.getMessage());
			//LiveShiftApplication.getLogs().newExceptionLogEntry(new LogEntry(e));
			return null;
		}

		return segmentBlock;

	}

	public synchronized void deleteBlock(SegmentIdentifier segmentIdentifier, int blockNumber) {
		
		File file = segmentIdentifier.getFile(this.storageDirectory, blockNumber);
		
		if (logger.isDebugEnabled()) logger.debug("in deleteBlock("+file.getPath()+")");
		
		// A File object to represent the filename

		// Make sure the file or directory exists and isn't write protected
		if (!file.exists())
			throw new IllegalArgumentException("Delete: no such file or directory: " + file.getPath());

		if (!file.canWrite())
			throw new IllegalArgumentException("Delete: write protected: " + file.getPath());

		// If it is a directory, make sure it is empty
		if (file.isDirectory()) {
			String[] files = file.list();
			if (files.length > 0)
				throw new IllegalArgumentException("Delete: directory not empty: " + file.getPath());
		}
		
		//FIXME delete the directory when it becomes empty

		// Attempt to delete it
		boolean success = file.delete();

		if (!success)
			throw new IllegalArgumentException("Delete: deletion failed");
		
		//TODO delete from index also

	}
	
	public synchronized int getOccupation() {
		return this.index.size();
	}

	private SegmentBlock getFromCache(final SegmentIdentifier segmentIdentifier, final int blockNumber) {
		synchronized (this.cache) {
			SegmentBlock segmentBlock;
			SoftReference<SegmentBlock> softSegmentBlock = this.cache[this.getCacheIndex(segmentIdentifier.getSegmentNumber(), blockNumber)];
			
			if (softSegmentBlock!=null)
				if (null!=(segmentBlock=softSegmentBlock.get())
						&& segmentBlock.getBlockNumber()==blockNumber 
						&& segmentBlock.getSegmentIdentifier().equals(segmentIdentifier))
					return segmentBlock;
			return null;
		}
	}
	private void putInCache(final SegmentBlock segmentBlock) {
		synchronized (this.cache) {
			this.cache[this.getCacheIndex(segmentBlock.getSegmentIdentifier().getSegmentNumber(), segmentBlock.getBlockNumber())] = new SoftReference<SegmentBlock>(segmentBlock);
		}
	}
	private int getCacheIndex(final long segmentNumber, final int blockNumber) {
		return Math.abs((int)segmentNumber*SegmentBlock.getBlocksPerSegment() + blockNumber)%CACHE_SIZE_BLOCKS;
	}

	public void registerTuner(Tuner tuner) {
		this.tuner = tuner;
	}

	public int getCapacity() {
		return MAX_BLOCKS_ON_DISK;
	}

	public boolean hasEmptyBlockMap(SegmentIdentifier segmentIdentifier) {
		Segment segment = this.getSegment(segmentIdentifier);
		if (segment==null)
			return true;
		return segment.getSegmentBlockMap().isEmpty();
	}
}
