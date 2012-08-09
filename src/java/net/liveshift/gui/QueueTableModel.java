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

import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.upload.Subscriber;


/**
 * 
 * @author draft
 */
public class QueueTableModel implements TableModel
{
	private List<Subscriber> queueAndUploadSlots = null;
	final private List<TableModelListener> listeners = new ArrayList<TableModelListener>();
	private IncentiveMechanism	incentiveMechanism;

	public void updateSnapshot(List<Subscriber> queueAndUploadSlots, IncentiveMechanism incentiveMechanism)
	{
		synchronized (this)
		{
			this.incentiveMechanism = incentiveMechanism;
			this.queueAndUploadSlots = queueAndUploadSlots;
			notifyChange();

		}
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
			if (queueAndUploadSlots == null)
				return 0;
			else
				return queueAndUploadSlots.size();
		}
	}

	public int getColumnCount()
	{
		return 2;
	}

	public String getColumnName(int columnIndex)
	{
		if (columnIndex == 0)
			return "Peer";
		else
			return "Reputation";
	}

	public Class<?> getColumnClass(int columnIndex)
	{
		if (columnIndex == 0)
			return String.class;
		else
			return Integer.class;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	public Object getValueAt(int rowIndex, int columnIndex)
	{
		synchronized (this)
		{
			
			if (rowIndex >= queueAndUploadSlots.size())
				return "n/a";
			
			Subscriber subscriber = queueAndUploadSlots.get(rowIndex);
			if (subscriber==null)
				return "n/a";
			
			if (columnIndex == 0)
			{
				StringBuilder sb = new StringBuilder();
				
				if (subscriber.isGranted())
					sb.append("* ");
				else if (subscriber.isInterested())
					sb.append("+ ");
				
				sb.append(subscriber.getRequesterPeerId().getName()+" [");
				sb.append(subscriber.getSegmentIdentifier().getNiceStringIdentifier());
				return sb.append("]").toString();
			}
			else {
				return this.incentiveMechanism.getReputation(subscriber.getRequesterPeerId(), subscriber.getSegmentIdentifier().getStartTimeMS());
			}
		}
	}

	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
	}

	public void addTableModelListener(TableModelListener l)
	{
		listeners.add(l);
	}

	public void removeTableModelListener(TableModelListener l)
	{
		listeners.remove(l);
	}
}
