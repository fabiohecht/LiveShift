package net.liveshift.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import javax.swing.JList;

import net.liveshift.core.Channel;

public class ChannelsPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2059124764034073440L;
	
	final private LiveShiftGUI liveShiftGUI;

	private String currentSearch = "";
	private boolean searchFieldIsLabel= true;
	private JList channelList;
	
    @Override  
    public void paintComponent(Graphics g){
    	super.paintComponent(g);

    	g.setColor(Color.WHITE);
        g.fillRoundRect(0, 10, this.getWidth()-5, this.getHeight()-10, 10, 10);
    }
    
	public ChannelsPanel(final LiveShiftGUI liveShiftGUI) {
		
		this.liveShiftGUI = liveShiftGUI;
		
		//Rectangle r = LiveShiftGUI.getInstance().getAppBounds(LiveShiftGUI.SubAppPosition.RIGHT);
		//this.setSize(r.width, r.height);
		this.setOpaque(false);
		this.setBorder(new EmptyBorder(15,5,5,10));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.setMinimumSize(new Dimension(200, 30));
		this.setMaximumSize(new Dimension(200, 30));
		this.setPreferredSize(new Dimension(200, 30));

		
		JButton closeButton = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_CLOSE_CHANNEL_LIST), true);
		closeButton.setToolTipText(Design.TOOLTIP_CLOSE_CHANNEL_LIST);
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				
				liveShiftGUI.setChannelListVisible(false);
			}
		});
		
		JPanel searchPanel = new JPanel();
		searchPanel.setOpaque(false);
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));

		final JTextField searchField = new JTextField("search channels");
		searchField.setForeground(Design.TEXT_COLOR_DISABLED);
		searchField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				if (!searchFieldIsLabel && searchField.getText().equals("")) {
					searchField.setText("search channels");
					searchField.setForeground(Design.TEXT_COLOR_DISABLED);
					searchFieldIsLabel=true;
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				if (searchFieldIsLabel) {
					searchField.setText("");
					searchField.setForeground(Design.TEXT_COLOR_DARK);
					searchFieldIsLabel=false;
				}
			}
		});
		
		searchPanel.add(searchField);
		JButton searchButton = new LiveShiftButton(Design.getInstance().getIcon(Design.ICON_SEARCH), true);
		searchButton.setToolTipText(Design.TOOLTIP_SEARCH);
		searchButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (searchFieldIsLabel) {
					currentSearch = "";
				}
				else {
					currentSearch = searchField.getText();
				}
				updateChannelList();
			}
		});
		searchPanel.add(Box.createHorizontalStrut(2));
		searchPanel.add(searchButton);
		searchPanel.add(Box.createHorizontalStrut(2));
		searchPanel.add(closeButton);
		
		searchPanel.setMinimumSize(new Dimension(200, 25));
		searchPanel.setMaximumSize(new Dimension(200, 25));
		searchPanel.setPreferredSize(new Dimension(200, 25));
		
		this.add(searchPanel);
		this.add(Box.createVerticalStrut(5));
/*
		JPanel separator = new JPanel() {
			@Override
			public void paint(Graphics g) {
				g.setColor(Design.TEXT_COLOR_DARK);
				g.drawLine(0,0,200,0);
			}
		};
		separator.setSize(200, 1);
		separator.setMaximumSize(new Dimension(200, 1));
		this.add(separator);
*/
		ChannelsListModel listModel= new ChannelsListModel();
		
		channelList = new JList(listModel) {
		    // custom tooltip for each item
		    @Override
			public String getToolTipText(MouseEvent evt) {
		        int index = locationToIndex(evt.getPoint());
		        Channel channel = ((ChannelsListModel)getModel()).getChannelAt(index);
		        if (channel==null)
		        	return null;
		        else
		        	return channel.getName();
		    }
		};;
		JScrollPane channelsPanel = new JScrollPane(channelList);
		channelsPanel.setOpaque(false);
		channelsPanel.setBorder(BorderFactory.createEmptyBorder());
		channelsPanel.setViewportBorder(BorderFactory.createEmptyBorder());
		channelList.setBorder(BorderFactory.createEmptyBorder());
		this.add(channelsPanel, BorderLayout.CENTER);
		
		channelList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				//if inside list
		        JList list = (JList)e.getSource();
		        Rectangle r = list.getCellBounds(0, list.getLastVisibleIndex());
		        
		        if (r != null && r.contains(e.getPoint())) {
					//detects double-click
			        if (e.getClickCount() == 1) {
			        	liveShiftGUI.showPlayButtons();
			        }
			        else if (e.getClickCount() == 2) {
	
			        	Channel channel = getSelectedChannel();
			        	
			        	if (channel==null) {
			        		
			        	}
			        	else {
							//switches channel to the selected one (live)
			        		liveShiftGUI.switchChannel(channel, 0);
			        	}
			        }
		        }
			}
		});
	}
	
	public Channel getSelectedChannel() {
		JList list = this.channelList;
		int selectedIndex = list.getSelectedIndex();
		if (selectedIndex==-1) {
			return null;
		}
    	Object obj = ((ChannelsListModel)list.getModel()).getChannelAt(selectedIndex);
    	return (Channel)obj;
	}

	public void updateChannelList() {
		
		String[] tags = currentSearch.equals("")?new String[0]:currentSearch.split(" ");
		Collection<Channel> channels = liveShiftGUI.getChannelsByTags(tags);
		if (channels!=null) {
			((ChannelsListModel)this.channelList.getModel()).replace(channels);
		}
	}
	
}
