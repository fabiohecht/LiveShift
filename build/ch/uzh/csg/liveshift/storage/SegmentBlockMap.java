package net.liveshift.storage;

import java.io.Serializable;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.liveshift.signaling.messaging.SegmentBlock;

/**
 * Holds block availability information for a certain segment
 * Information is stored using a BitSet
 * 
 * 
 * @author fabio, draft
 *
 */

public class SegmentBlockMap extends BitSet implements Serializable {

	private static final long serialVersionUID = 3545950751149349081L;

	final private static Logger logger = LoggerFactory.getLogger(SegmentBlockMap.class);

	final private static int length = (SegmentBlock.getBlocksPerSegment());  //number of blocks per segment
	transient private int startBlockNumber = 0;  //starting block (useful in the first requested segment, since the request may not match exactly the start of a segment

	public SegmentBlockMap() {
		super(length);
	}
	public SegmentBlockMap(int startBlockNumber) {
		this();
		
		this.setStartBlockNumber(startBlockNumber);
	}

	// Returns a BitSet containing the values in bytes.
	// The byte-ordering of bytes must be big-endian which means the most
	// significant bit is in element 0.
	public SegmentBlockMap(byte[] byteArray)
	{
		for (int i = 0; i < byteArray.length * 8; i++)
		{
			if ((byteArray[byteArray.length - i / 8 - 1] & (1 << (i % 8))) > 0)
			{
				this.set(i);
			}
		}
	}

	// Returns a byte array of at least length 1.
	// The most significant bit in the result is guaranteed not to be a 1
	// (since BitSet does not support sign extension).
	// The byte-ordering of the result is big-endian which means the most
	// significant bit is in element 0.
	// The bit at index 0 of the bit set is assumed to be the least significant
	// bit.
	public byte[] toByteArray()
	{
		byte[] bytes = new byte[(int)Math.ceil(SegmentBlockMap.length / 8F)];
		for (int i = 0; i < SegmentBlockMap.length; i++)
		{
			if (this.get(i))
			{
				bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
			}
		}
		return bytes;
	}
	
	/**
	 * the opposite of isEmpty
	 * returns whether there is no unset bit (taking the range after the given fromBlockNumber in consideration)
	 * 
	 * @param fromBlockNumber 
	 */
	public boolean isFull(int fromBlockNumber) {
		float howFull = this.howFull(fromBlockNumber);
		if (logger.isDebugEnabled()) logger.debug("howfull="+howFull);
		return howFull==1F;
	}
	
	/**
	 * returns share of occupied blocks (set bits) 0..1
	 * from this.start until the end
	 * @param fromBlockNumber 
	 */
	private float howFull(int fromBlockNumber) {
		
		if (this.isEmpty())
			return 0F;
		
		int startBlock = Math.max(fromBlockNumber, this.startBlockNumber);
		
		int clearBitAfterStart = this.nextClearBit(startBlock);
		
		if (clearBitAfterStart==-1)
			return 1F;
		else
			return (float)(clearBitAfterStart-startBlock)/(SegmentBlockMap.length-startBlock);
	}
	
	/**
	 * returns -1 if not found
	 */
	@Override
	public int nextSetBit(int fromIndex) {
		
		int nextClear = super.nextSetBit(Math.max(fromIndex, this.getStartBlockNumber()));
		
		if (nextClear >= SegmentBlockMap.length)
			return -1;  //no next set: they are all full fromIndex
		else
			return nextClear;
		
	}
	
	/**
	 * returns -1 if not found
	 */
	@Override
	public int nextClearBit(int fromIndex) {

		int nextClear = super.nextClearBit(Math.max(fromIndex, this.getStartBlockNumber()));
		
		if (nextClear >= SegmentBlockMap.length)
			return -1;  //no next clear: they are all full fromIndex
		else
			return nextClear;
		
	}
	

	/**
	 * returns the numbers of the next "howMany" blocks that at least one neighbor has
	 * @param fromBlock initial block to select 
	 */
	public BitSet getNextClearBits(int fromBlock, int howMany, int limitBlock) {
		
		if (logger.isDebugEnabled()) logger.debug("in getNextClearBits("+fromBlock+","+howMany+","+limitBlock+")");
		
		BitSet nextBlocks = new BitSet(howMany);
		int found = 0, nextBlock = fromBlock-1;

		while (found++ < howMany) {
			
			nextBlock = this.nextClearBit(nextBlock+1);
			
			if (nextBlock==-1 || nextBlock > limitBlock)  //not found || it's the same
				break;
			
			nextBlocks.set(nextBlock);
		}
		if (logger.isDebugEnabled()) logger.debug(" getNextClearBits returning ("+nextBlocks.toString()+")");
		
		return nextBlocks;
	}
	public void setStartBlockNumber(int startBlockNumber) {
		this.startBlockNumber = startBlockNumber;
	}
	public int getStartBlockNumber() {
		return startBlockNumber;
	}
	
	/**
	 * returns the last set bit, counting from the given bit.
	 * if they are all set, returns the last one.
	 * if the bit at the given blockNumber is not set, returns the last set bit before the given bit.
	 *  
	 * @param blockNumber the reference bit
	 */
	public int getLastSetBit(int blockNumber) {
		if (!super.get(blockNumber))
			return this.getPreviousSetBit(blockNumber);
		else
			return super.nextClearBit(blockNumber)-1;
	}

	public int getLastSetBit() {
		return getPreviousSetBit(length);
	}

	/**
	 * returns the actual length of this SegmentBlockMap, in blocks (bits)
	 * @return the actual length of this SegmentBlockMap, in blocks (bits)
	 */
	public int getLength() {
		return length;
	}
	
	@Override
	public String toString() {
		String out = "";
		int unset = 0, set;
		
		while (true) {
			
			set = super.nextSetBit(unset);
			if (set == -1)
				break;
			
			unset = super.nextClearBit(set);
			
			if (out.length()>0)
				out += ",";
			
			if (unset == -1) {
				int lastSet = this.getLastSetBit();
				if (lastSet==set)
					out += set;
				else
					out += set+"-"+lastSet;
				break;
			}
			if (set == unset-1)
				out += set;
			else if (set < unset-1)
				out += set + "-" + (unset-1);
			
		}
		
		return "[" + out + "] st:"+startBlockNumber;
	}
	
    /**
     * Returns the index of the nearest bit that is set to <code>true</code>
     * that occurs on or before the specified starting index. If no such
     * bit exists then -1 is returned.
     * 
     * [Java 7 will support this natively, so we can get rid of this here]
     * 
     * @param fromIndex the index to start checking from (inclusive).
     * @return the index of the previous set bit.
     * @throws IndexOutOfBoundsException if the specified index is negative.
     */
    public int getPreviousSetBit(int fromIndex) {
		if (fromIndex >= 0) {
		    for (int i = fromIndex; i>=0; i--)
		    	if (super.get(i))
		    		return i;
		    return -1;
		}
		else
			throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
		
    }
/*
    @Override
    public boolean equals(Object obj) {
		if (!(obj instanceof SegmentBlockMap))
		    return false;
		if (this == obj)
		    return true;

		SegmentBlockMap set = (SegmentBlockMap) obj;

		if (wordsInUse != set.wordsInUse)
	            return false;

		// Check words in use by both BitSets
		for (int i = 0; i < wordsInUse; i++)
		    if (words[i] != set.words[i])
			return false;

		return true;
    }*/
}
