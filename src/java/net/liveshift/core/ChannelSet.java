package net.liveshift.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChannelSet {

	Map<String, Set<Channel>> tags = new HashMap<String, Set<Channel>>();
	Map<Integer,Channel> ids = new HashMap<Integer, Channel>();
	
	public void add(Channel channel) {
		this.ids.put(channel.getID(), channel);
		
		for (String tag : channel.getTags()) {
			this.putTag(tag, channel);
		}
		
		//puts channel name and description as tags as well
		for (String tag : channel.getName().split(" ")) {
			this.putTag(tag, channel);
		}
		String description = channel.getDescription();
		if (description!=null) {		
			for (String tag : description.split(" ")) {
				this.putTag(tag, channel);
			}
		}
	}

	synchronized private void putTag(String tag, Channel channel) {
		Set<Channel> channelSet = this.tags.get(tag);
		if (channelSet == null) {
			channelSet = new HashSet<Channel>();
			this.tags.put(tag.toLowerCase(), channelSet);
		}
		channelSet.add(channel);
	}

	public Channel getById(int channelId) {
		return this.ids.get(channelId);
	}

	/**
	 * returns all channels that match any tags
	 * 
	 * @param strings
	 * @return
	 */
	synchronized public Collection<Channel> getByTags(String[] strings) {
		
		if (strings==null || strings.length==0) {
			//returns all channels (likely empty search criteria)
			return this.ids.values();
		}
		
		Set<Channel> out = new HashSet<Channel>();
		for (String tag : strings) {
			Set<Channel> channel = this.tags.get(tag.toLowerCase());
			if (channel!=null) {
				out.addAll(channel);
			}
		}
		return out;
	}

	/**
	 * returns all channels that match any tags
	 * 
	 * @param strings
	 * @return
	 */
	synchronized public Set<Channel> getByTags(Set<String> strings) {
		Set<Channel> out = new HashSet<Channel>();
		for (String tag : strings) {
			Set<Channel> channel = this.tags.get(tag);
			if (channel!=null) {
				out.addAll(channel);
			}
		}
		return out;
	}
	public Map<Integer, Channel> getChannelIdMap() {
		return this.ids;
	}
	
	synchronized public void clear() {
		this.ids.clear();
		this.tags.clear();
	}

}
