package net.liveshift.core;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import net.liveshift.time.Clock;


/** 
 * @author Fabio Victora Hecht, draft
*/

public class Channel implements Serializable, Comparable<Channel> {
	
	private static final long	serialVersionUID	= -160570166976268673L;
	final private String name;
	final private byte numSubstreams;
	final private long startupTimeMillis;
	final private int uniqueId;  //todo: make sure the uniqueId is unique
	//TODO different for each channel final private long segmentSizeMillis = 1000;
	//TODO different for each channel final private long blockSizeMillis = 1000;
//	final private float typicalBlockSizeBytes;

	private String description;
	private PeerId source;
	transient private boolean ownChannel = false;
	
	private Set<String> tags; //TODO: set and search for tags
	
	public Channel(final String name, final byte numSubstreams, final int uniqueId)
	{
		this.name = name;
		this.numSubstreams = numSubstreams;
		this.startupTimeMillis = Clock.getMainClock().getTimeInMillis(false);
		this.uniqueId = uniqueId;
		this.tags = new HashSet<String>();
	}
	
	public String getName()
	{
		return name;
	}

	public byte getNumSubstreams()
	{
		return numSubstreams;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return "ch:"+this.name;
	}

	public void setOwnChannel()
	{
		this.ownChannel = true;
	}
	public boolean isOwnChannel()
	{
		return this.ownChannel;
	}

	public PeerId getSource()
	{
		return this.source;
	}
	
	public int getID()
	{
		return this.uniqueId;  //TODO should be unique
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode() ^ numSubstreams;
	}
	
	@Override
	public boolean equals(Object channel0) {
		if (channel0 == null || !(channel0 instanceof Channel))
			return false;
		Channel c = (Channel) channel0;
		return c.name.equals(name) && c.numSubstreams == numSubstreams;
	}

	public long getStartupTimeMillis() {
		return this.startupTimeMillis;
	}

	@Override
	public int compareTo(Channel o) {
		return this.uniqueId - o.uniqueId;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}
}
