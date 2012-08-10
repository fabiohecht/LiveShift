package net.liveshift.gui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import net.liveshift.core.PeerId;


public class ReputationTableModel implements TableModel
{
	final private static String[] names = new String[] { "Peer", "Reputation" };
	final private List<TableModelListener> listeners = new ArrayList<TableModelListener>();
	final private Map<PeerId, Float>	peerReputations;

	public ReputationTableModel(Map<PeerId, Float> peerReputations)
	{
		this.peerReputations = peerReputations;
	}

	public void notifyChange()
	{
		for (TableModelListener l : listeners)
			l.tableChanged(new TableModelEvent(this));
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		synchronized (this)
		{
			List<Entry<PeerId, Float>> cache = new ArrayList<Entry<PeerId,Float>>(peerReputations.entrySet());
			
			Entry<PeerId, Float> entry = cache.get(rowIndex);
			if (entry == null)
				return "n/a";
			
			switch (columnIndex)
			{
				case 0:
					PeerId peerId = entry.getKey();
					if (peerId == null)
						return entry.getKey();
					else
						return peerId.getName();
				case 1:
					return entry.getValue();
				default:
					return "n/a";
			}
			
		}
	}

	@Override
	public int getRowCount()
	{
		synchronized (this)
		{
			if (peerReputations == null)
				return 0;
			else
				return peerReputations.size();
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return false;
	}

	@Override
	public int getColumnCount()
	{
		return names.length;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return names[columnIndex];
	}

	@Override
	public void addTableModelListener(TableModelListener l)
	{
		synchronized (this)
		{
			listeners.add(l);
		}
	}

	@Override
	public void removeTableModelListener(TableModelListener l)
	{
		synchronized (this)
		{
			listeners.remove(l);
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
	}
}
