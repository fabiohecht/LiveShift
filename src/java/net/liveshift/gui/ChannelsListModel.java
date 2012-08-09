package net.liveshift.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractListModel;

import net.liveshift.core.Channel;
import net.liveshift.util.Utils;


public class ChannelsListModel extends AbstractListModel {

	private static final int MAX_LENGTH = 20;
	
	private List<Channel> channels;
	
	@Override
	public int getSize() {
		return channels!=null?this.channels.size():0;
	}

	@Override
	public Object getElementAt(int index) {
		
		if (this.channels==null) {
			return null;
		}
		
		Channel channel = (Channel)this.channels.get(index);
		
		return Utils.shortenString(/*"#"+channel.getID()+" "+*/channel.getName(), MAX_LENGTH);
	}

	public Channel getChannelAt(int index) {
		if (index==-1)
			return null;
		else
			return channels!=null?((Channel)this.channels.get(index)):null;
	}

	public void replace(Collection<Channel> channels) {

		this.channels = new ArrayList<Channel>(channels);
		
		this.fireContentsChanged(this, 0, channels.size()-1);
	}
}
