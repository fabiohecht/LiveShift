/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package net.liveshift.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.liveshift.core.Channel;
import net.liveshift.core.PeerId;


/**
 * 
 * @author draft
 */
public class ConnectionTableModel implements TableModel
{
	final private static String[] names = new String[] { "*", "Type",
			"Channel.Segment.Substream.Block", "Reply" };
	final private static int MAX_SIZE = 25;
	final private List<Row> cacheMap = new ArrayList<Row>(MAX_SIZE);
	final private List<TableModelListener> listeners = new ArrayList<TableModelListener>();

	public ConnectionTableModel(boolean incoming)
	{
		names[0] = incoming ? "Sender" : "Receiver";
	}

	public void addRequest(PeerId peerId, String type, Channel channel, long segment,
			int substream, int conversationID, int blockNr)
	{
		
		synchronized (this)
		{
			cacheMap.add(0, new Row(peerId, type, channel, segment, substream, conversationID, blockNr, ""));
			if (cacheMap.size() > MAX_SIZE)
			{
				cacheMap.remove(cacheMap.size() - 1);
				notifyChange(cacheMap.size() - 1);
			}
			notifyChange();
		}
	}

	public void updateRequest(String reply, int conversationID)
	{
		synchronized (this)
		{
			for (int i = 0; i < cacheMap.size(); i++)
			{
				Row row = cacheMap.get(i);
				if (row.getConversationID() == conversationID)
				{
					row.setReply(reply);
					notifyChange(i);
					return;
				}
			}
		}
	}

	private void notifyChange(int row)
	{
		for (TableModelListener l : listeners)
			l.tableChanged(new TableModelEvent(this, row));
	}

	private void notifyChange()
	{
		for (TableModelListener l : listeners)
			l.tableChanged(new TableModelEvent(this));
	}

	public int getRowCount()
	{
		synchronized (this)
		{
			return cacheMap.size();
		}
	}

	public int getColumnCount()
	{
		return names.length;
	}

	public String getColumnName(int columnIndex)
	{
		return names[columnIndex];
	}

	public Class<?> getColumnClass(int columnIndex)
	{
		return String.class;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		synchronized (this)
		{
			if (rowIndex >= cacheMap.size())
				return "n/a";
			Row row = cacheMap.get(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return row.getPeerId().getName();
				case 1:
					String[] splitType = row.getType().split("\\.");
					return splitType[splitType.length-1];
				case 2:
					StringBuilder sb = new StringBuilder(row.getChannel().getName()).append(".");
					sb.append(row.getSegment()).append(".");
					sb.append(row.getSubstream()).append(".");
					sb.append(row.getBlockNr());

					return sb;
				case 3:
					return row.reply;
				default:
					return "n/a";
			}
		}
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
	}

	public void addTableModelListener(TableModelListener l)
	{
		synchronized (this)
		{
			listeners.add(l);
		}
	}

	public void removeTableModelListener(TableModelListener l)
	{
		synchronized (this)
		{
			listeners.remove(l);
		}
	}
	private class Row
	{
		final private PeerId peerId;
		final private Channel channel;
		final private Long segment;
		final private int substream;
		final private int conversationID;
		final private int blockNr;
		private String type;
		private String reply;

		public Row(PeerId peerId, String type, Channel channel, Long segment, int substream,
				int conversationID, int blockNr, String reply)
		{
			this.peerId = peerId;
			this.type = type;
			this.channel = channel;
			this.segment = segment;
			this.substream = substream;
			this.conversationID = conversationID;
			this.blockNr = blockNr;
			this.setReply(reply);
		}

		public PeerId getPeerId()
		{
			return peerId;
		}

		public String getType()
		{
			return type;
		}

		public void setType(String messageType)
		{
			this.type = messageType;
		}

		public Channel getChannel()
		{
			return channel;
		}

		public Long getSegment()
		{
			return segment;
		}

		public int getSubstream()
		{
			return substream;
		}

		public int getConversationID()
		{
			return conversationID;
		}

		public int getBlockNr()
		{
			return blockNr;
		}

		public void setReply(String reply) {
			this.reply = reply;
		}

		public String getReply() {
			return reply;
		}
	}
}
