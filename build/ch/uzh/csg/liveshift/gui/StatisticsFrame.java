package net.liveshift.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import net.liveshift.configuration.Configuration;
import net.liveshift.core.Channel;
import net.liveshift.core.Stats;
import net.liveshift.core.PeerId;
import net.liveshift.incentive.IncentiveMechanism;
import net.liveshift.signaling.messaging.AbstractMessage;
import net.liveshift.signaling.messaging.BlockReplyMessage;
import net.liveshift.signaling.messaging.BlockRequestMessage;
import net.liveshift.signaling.messaging.SegmentBlock;
import net.liveshift.storage.Segment;
import net.liveshift.storage.SegmentIdentifier;
import net.liveshift.upload.Subscriber;

public class StatisticsFrame extends JFrame implements Stats {

	private static final long serialVersionUID = -7216028720767314074L;

	private JPanel	contentPane;

	private JTable outgoingTable;
	private JTable incomingTable;
	private javax.swing.JTextField pshTextField;
	private javax.swing.JScrollPane queueScrollPane;
	private javax.swing.JTable queueTable;
	private javax.swing.JScrollPane reputationScrollPane;
	private javax.swing.JTable reputationTable;	

	private byte substreams;
	private JFreeChart localStorageChart;
	private NumberAxis xAxis;    
	final public static int WIDTH=60;
	final DefaultXYZDataset dataset=new DefaultXYZDataset();
	private double[][] data;
	private XYPlot plot;
	private javax.swing.JPanel blockPanel;
	private Map<SegmentIdentifier, byte[]> blockMaps;
	private long playTime;
	private long segmentNumber;
	private int currentWindow;
	private Map<PeerId, Float> oldPeerReputations;
	private Panel panel;
	private JScrollPane incomingScrollPane;
	private JScrollPane outgoingScrollPane;
	
	private JTable logTable;
	private JTable exceptionLogTable;
	private JComboBox cbLogCauserFilter;
	
	/**
	 * Create the frame.
	 */
	public StatisticsFrame(final LiveShiftGUI liveShiftGUI) {

		this.setTitle("Statistics");
		setBounds(100, 100, 1024, 768);
		this.setLocationRelativeTo(null);
		this.setIconImages(Design.getAppIcons());

		initialization();
		Channel channel = liveShiftGUI.getLiveShiftApplication().getPlayingChannel();
		if (channel != null) {
			reset(channel.getNumSubstreams());
		}
		
		liveShiftGUI.getLiveShiftApplication().addStatListener(this);
	}

	private void initialization(){

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(1, 1, 1, 1));
		setContentPane(contentPane);

		initStatComponents(substreams);
		contentPane.setLayout(new BorderLayout(0, 0));
		
//		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
//		contentPane.add(tabbedPane, BorderLayout.CENTER);		

		panel = new Panel();
		//contentPane.add(panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{400, 400, 200};
		gbl_panel.rowHeights = new int[]{24, 250, 24, 250};
		//gbl_panel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.5};
		//												gbl_panel.columnWidths = new int[]{31, 62, 433, 2, 8, 58, 0};
		//												gbl_panel.rowHeights = new int[]{418, 418, 270, 0};
		//												gbl_panel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		//												gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);		

		pshTextField = new JTextField();
//		GridBagConstraints gbc_pshTextField = new GridBagConstraints();
//		gbc_pshTextField.anchor = GridBagConstraints.EAST;
//		gbc_pshTextField.insets = new Insets(0, 0, 5, 5);
//		gbc_pshTextField.gridwidth = 3;
//		gbc_pshTextField.gridx = 0;
//		gbc_pshTextField.gridy = 0;
//		panel.add(pshTextField, gbc_pshTextField);

		JLabel lblIncomingLabel = new JLabel("Incoming Block Requests");
		GridBagConstraints gbc_lblIncomingLabel = new GridBagConstraints();
		gbc_lblIncomingLabel.fill = GridBagConstraints.BOTH;
		gbc_lblIncomingLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblIncomingLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblIncomingLabel.gridx = 0;
		gbc_lblIncomingLabel.gridy = 0;
		panel.add(lblIncomingLabel, gbc_lblIncomingLabel);
		
		incomingScrollPane = new JScrollPane();
		GridBagConstraints gbc_incomingScrollPane = new GridBagConstraints();
		gbc_incomingScrollPane.fill = GridBagConstraints.BOTH;
		gbc_incomingScrollPane.anchor = GridBagConstraints.NORTHWEST;
		gbc_incomingScrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_incomingScrollPane.gridx = 0;
		gbc_incomingScrollPane.gridy = 1;
		panel.add(incomingScrollPane, gbc_incomingScrollPane);

		incomingTable = new JTable();
		incomingScrollPane.setViewportView(incomingTable);
		incomingTable.setModel(new ConnectionTableModel(true));
		incomingTable.setName("incomingTable");

		JLabel lblOutgoingLabel = new JLabel("Outgoing Block Requests");
		GridBagConstraints gbc_lblOutgoingLabel = new GridBagConstraints();
		gbc_lblOutgoingLabel.fill = GridBagConstraints.BOTH;
		gbc_lblOutgoingLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblOutgoingLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblOutgoingLabel.gridx = 1;
		gbc_lblOutgoingLabel.gridy = 0;
		panel.add(lblOutgoingLabel, gbc_lblOutgoingLabel);
		
		outgoingScrollPane = new JScrollPane();
		GridBagConstraints gbc_outgoingScrollPane = new GridBagConstraints();
		gbc_outgoingScrollPane.fill = GridBagConstraints.BOTH;
		gbc_outgoingScrollPane.anchor = GridBagConstraints.NORTHWEST;
		gbc_outgoingScrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_outgoingScrollPane.gridx = 1;
		gbc_outgoingScrollPane.gridy = 1;
		panel.add(outgoingScrollPane, gbc_outgoingScrollPane);

		outgoingTable = new JTable();
		outgoingScrollPane.setViewportView(outgoingTable);
		outgoingTable.setModel(new ConnectionTableModel(false));
		outgoingTable.setName("outgoingTable");
		
		JLabel lblReputationLabel = new JLabel("Reputation");
		GridBagConstraints gbc_lblReputationLabel = new GridBagConstraints();
		gbc_lblReputationLabel.fill = GridBagConstraints.BOTH;
		gbc_lblReputationLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblReputationLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblReputationLabel.gridx = 2;
		gbc_lblReputationLabel.gridy = 0;
		panel.add(lblReputationLabel, gbc_lblReputationLabel);

		reputationScrollPane = new JScrollPane();
		GridBagConstraints gbc_reputationScrollPane = new GridBagConstraints();
		gbc_reputationScrollPane.fill = GridBagConstraints.BOTH;
		gbc_reputationScrollPane.anchor = GridBagConstraints.WEST;
		gbc_reputationScrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_reputationScrollPane.gridx = 2;
		gbc_reputationScrollPane.gridy = 1;
		panel.add(reputationScrollPane, gbc_reputationScrollPane);
		
		reputationTable = new JTable();
        reputationScrollPane.setViewportView(reputationTable);		
		reputationTable.setModel(new ReputationTableModel(null));
		reputationTable.setName("reputationTable");

		JLabel lblQueueLabel = new JLabel("Upload Slot Queue");
		GridBagConstraints gbc_lblQueueLabel = new GridBagConstraints();
		gbc_lblQueueLabel.fill = GridBagConstraints.BOTH;
		gbc_lblQueueLabel.anchor = GridBagConstraints.NORTHWEST;
		gbc_lblQueueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblQueueLabel.gridx = 2;
		gbc_lblQueueLabel.gridy = 2;
		panel.add(lblQueueLabel, gbc_lblQueueLabel);
		
		queueScrollPane = new JScrollPane();
		GridBagConstraints gbc_queueScrollPane = new GridBagConstraints();
		gbc_queueScrollPane.fill = GridBagConstraints.BOTH;
		gbc_queueScrollPane.anchor = GridBagConstraints.WEST;
		gbc_queueScrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_queueScrollPane.gridx = 2;
		gbc_queueScrollPane.gridy = 3;
		panel.add(queueScrollPane, gbc_queueScrollPane);
		
		queueTable = new JTable();
		queueScrollPane.setViewportView(queueTable);
		queueTable.setModel(new QueueTableModel());
		queueTable.setName("queueTable");

		//		contentPane.add(outgoingTable);


		blockPanel = new JPanel();
		GridBagConstraints gbc_blockPanel = new GridBagConstraints();
		gbc_blockPanel.fill = GridBagConstraints.BOTH;
		gbc_blockPanel.anchor = GridBagConstraints.NORTHWEST;
		gbc_blockPanel.insets = new Insets(0, 0, 5, 5);
		gbc_blockPanel.gridwidth = 2;
		gbc_blockPanel.gridx = 0;
		gbc_blockPanel.gridy = 3;
		panel.add(blockPanel, gbc_blockPanel);

		blockPanel.setName("blockPanel"); // NOI18N

		javax.swing.GroupLayout gl_blockPanel = new javax.swing.GroupLayout(blockPanel);
		blockPanel.setLayout(gl_blockPanel);
		gl_blockPanel.setHorizontalGroup(
				gl_blockPanel.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGap(0, 882, Short.MAX_VALUE)
		);
		gl_blockPanel.setVerticalGroup(
				gl_blockPanel.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGap(0, 191, Short.MAX_VALUE)
		);
		ChartPanel chartPanel=new ChartPanel(localStorageChart);
		chartPanel.setMinimumDrawHeight(300);
		chartPanel.setMinimumDrawWidth(600);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		blockPanel.setLayout(new GridLayout());
		blockPanel.add(chartPanel);		
		
//		tabbedPane.addTab("Overview", panel);
		contentPane.add(panel, BorderLayout.CENTER);
		

		
		
//		
//		
//		pnlLog = new JPanel();
//		GridBagLayout gbl_pnlLog = new GridBagLayout();
//		gbl_pnlLog.columnWidths = new int[]{500, 100};
//		gbl_pnlLog.rowHeights = new int[]{24, 24, 250};
//		gbl_pnlLog.columnWeights = new double[]{0.5, 0.0};
//		gbl_pnlLog.rowWeights = new double[]{0.0, 0.0, 0.5};
//		pnlLog.setLayout(gbl_pnlLog);
//		
//
//		
//		JLabel lblLogLabel = new JLabel("Application Log (last 1000 entries)");
//		GridBagConstraints gbc_lblLogLabel = new GridBagConstraints();
//		gbc_lblLogLabel.fill = GridBagConstraints.BOTH;
//		gbc_lblLogLabel.anchor = GridBagConstraints.NORTHWEST;
//		gbc_lblLogLabel.insets = new Insets(0, 0, 5, 5);
//		gbc_lblLogLabel.gridx = 0;
//		gbc_lblLogLabel.gridy = 0;
//		pnlLog.add(lblLogLabel, gbc_lblLogLabel);
//		
//		cbLogCauserFilter = new JComboBox();
//		GridBagConstraints gbc_cbLogCauserFilter = new GridBagConstraints();
//		gbc_cbLogCauserFilter.fill = GridBagConstraints.BOTH;
//		gbc_cbLogCauserFilter.anchor = GridBagConstraints.NORTHWEST;
//		gbc_cbLogCauserFilter.insets = new Insets(0, 0, 5, 5);
//		gbc_cbLogCauserFilter.gridx = 0;
//		gbc_cbLogCauserFilter.gridy = 1;
//		pnlLog.add(cbLogCauserFilter, gbc_cbLogCauserFilter);
//		cbLogCauserFilter.addItem("*");
//		cbLogCauserFilter.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {
//					public boolean include(Entry entry) {
//						if(cbLogCauserFilter.getSelectedItem().toString() == "*")
//							return true;
//						return entry.getValue(1) == cbLogCauserFilter.getSelectedItem().toString();
//					}
//				};
//
//				TableRowSorter<LogTableModel> sorter = new TableRowSorter<LogTableModel>((LogTableModel)logTable.getModel());
//				sorter.setRowFilter(filter);
//				logTable.setRowSorter(sorter);
//			}
//		});
//		
//		JButton btnClearLog = new JButton("Clear");
//		GridBagConstraints gbc_btnClearLog = new GridBagConstraints();
//		gbc_btnClearLog.fill = GridBagConstraints.BOTH;
//		gbc_btnClearLog.anchor = GridBagConstraints.NORTHWEST;
//		gbc_btnClearLog.insets = new Insets(0, 0, 5, 5);
//		gbc_btnClearLog.gridx = 1;
//		gbc_btnClearLog.gridy = 1;
//		pnlLog.add(btnClearLog, gbc_btnClearLog);
//		btnClearLog.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				logTable.setModel(new LogTableModel());
//				
//				while(cbLogCauserFilter.getItemCount() > 0){
//					cbLogCauserFilter.removeItemAt(0);
//				}
//				cbLogCauserFilter.addItem("*");
//			}
//		});
//		
//		logScrollPane = new JScrollPane();
//		GridBagConstraints gbc_logScrollPane = new GridBagConstraints();
//		gbc_logScrollPane.anchor = GridBagConstraints.NORTHWEST;
//		gbc_logScrollPane.fill = GridBagConstraints.BOTH;
//		gbc_logScrollPane.gridx = 0;
//		gbc_logScrollPane.gridy = 2;
//		gbc_logScrollPane.gridwidth = 2;
//		pnlLog.add(logScrollPane, gbc_logScrollPane);
//		
//		logTable = new JTable();
//		logScrollPane.setViewportView(logTable);
//		logTable.setModel(new LogTableModel());
//		logTable.setName("logTable");
//
//		tabbedPane.addTab("Log", pnlLog);
//		
//		pnlUsageData = new JPanel();
//		GridBagLayout gbl_pnlUsageData = new GridBagLayout();
//		gbl_pnlUsageData.columnWidths = new int[]{400, 400, 200};
//		gbl_pnlUsageData.rowHeights = new int[]{44, 250, 250, 50};
//		//gbl_pnlUsageData.columnWeights = new double[]{0.0, Double.MIN_VALUE};
//		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.5};
//		pnlUsageData.setLayout(gbl_pnlUsageData);	
//		
//		JPanel pnlSummary = new JPanel();
//		pnlSummary.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Summary", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
//		JLabel lblConnectionUsageLabel = new JLabel("Connection: "); 
//		lblConnectionUsage = new JLabel();
//		pnlSummary.add(lblConnectionUsageLabel);
//		pnlSummary.add(lblConnectionUsage);
//		JLabel lblPublishUsageLabel = new JLabel("Publish: ");
//		lblPublishUsage = new JLabel();
//		pnlSummary.add(lblPublishUsageLabel);
//		pnlSummary.add(lblPublishUsage);
//		JLabel lblChannelUsageLabel = new JLabel("Channel: ");
//		lblChannelUsage = new JLabel();
//		pnlSummary.add(lblChannelUsageLabel);
//		pnlSummary.add(lblChannelUsage);
//		
//		GridBagConstraints gbc_pnlSummary = new GridBagConstraints();
//		gbc_pnlSummary.anchor = GridBagConstraints.NORTHWEST;
//		gbc_pnlSummary.gridwidth = 2;
//		gbc_pnlSummary.gridx = 0;
//		gbc_pnlSummary.gridy = 0;
//		pnlUsageData.add(pnlSummary, gbc_pnlSummary);
//		
//		
//		datasetChannels = new DefaultPieDataset();		
//		plotChannels = new PiePlot();
//		plotChannels.setDataset(datasetChannels);
//		chartChannels = new JFreeChart("Channel Usage", plotChannels);
//		chartPanelChannel = new ChartPanel(chartChannels);
//		GridBagConstraints gbc_chartPanelChannel = new GridBagConstraints();
//		gbc_chartPanelChannel.fill = GridBagConstraints.BOTH;
//		gbc_chartPanelChannel.gridy = 1;
//		gbc_chartPanelChannel.gridx = 0;
//		gbc_chartPanelChannel.anchor = GridBagConstraints.NORTHWEST;
//		pnlUsageData.add(chartPanelChannel, gbc_chartPanelChannel);
//		
//		datasetPlaymode = new DefaultPieDataset();		
//		plotPlaymode = new PiePlot();
//		plotPlaymode.setDataset(datasetPlaymode);
//		chartPlaymode = new JFreeChart("Timeshift / Live Stream Usage", plotPlaymode);
//		chartPanelPlaymode = new ChartPanel(chartPlaymode);
//		GridBagConstraints gbc_chartPanelPlaymode = new GridBagConstraints();
//		gbc_chartPanelPlaymode.fill = GridBagConstraints.BOTH;
//		gbc_chartPanelPlaymode.gridy = 1;
//		gbc_chartPanelPlaymode.gridx = 1;
//		gbc_chartPanelPlaymode.anchor = GridBagConstraints.NORTHWEST;
//		pnlUsageData.add(chartPanelPlaymode, gbc_chartPanelPlaymode);
//		
//	
//
//		
//		
//		
//		
//		tabbedPane.addTab("Usage Data", pnlUsageData);
//		
		JButton btnClose = new JButton("Close");
		GridBagConstraints gbc_btnClose = new GridBagConstraints();
		gbc_btnClose.anchor = GridBagConstraints.WEST;
		gbc_btnClose.gridwidth = 3;
		gbc_btnClose.gridx = 0;
		gbc_btnClose.gridy = 3;
		contentPane.add(btnClose, BorderLayout.SOUTH);
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				close();
			}
		});
//
//
//
//		PropertyChangeListener pclUsageLogger = new PropertyChangeListener() {
//			@Override
//			public void propertyChange(PropertyChangeEvent evt) {
//				if(evt.getPropertyName() == "events"){
//					lblConnectionUsage.setText("Opened (" + UsageLogger.getInstance().getEventCount(UsageEventType.OPEN_CONNECTION) + "), " +
//							"Closed (" + UsageLogger.getInstance().getEventCount(UsageEventType.CLOSE_CONNECTION) + ")");
//					lblPublishUsage.setText("Published (" + UsageLogger.getInstance().getEventCount(UsageEventType.PUBLISH_CHANNEL) + "), " +
//							"Closed (" + UsageLogger.getInstance().getEventCount(UsageEventType.UNPUBLISH_CHANNEL) + ")");
////					lblChannelUsage.setText("Opened (" + UsageLogger.getInstance().getEventCount(UsageEventType.OPEN_CHANNEL) + "), " +
////							"Closed (" + UsageLogger.getInstance().getEventCount(UsageEventType.CLOSE_CHANNEL) + ")");
//					
//					Map<String, Integer> channelOccurences = new HashMap<String, Integer>();
//					for(UsageEvent ev : UsageLogger.getInstance().getEvents(UsageEventType.CHANGE_CHANNEL)){
//						if(channelOccurences.containsKey(((Channel)ev.getObject()).getName())){
//							int nr = channelOccurences.get(((Channel)ev.getObject()).getName());
//							nr++;
//							channelOccurences.put(((Channel)ev.getObject()).getName(), nr);
//						} else {
//							channelOccurences.put(((Channel)ev.getObject()).getName(), 1);
//						}
//					}
//					for(String ch : channelOccurences.keySet()){
//						DatasetGroup dsGroup = new DatasetGroup(ch);
//						datasetChannels.setGroup(dsGroup);
//						datasetChannels.setValue(ch, channelOccurences.get(ch));
//					}
//															
//					
//					int live = UsageLogger.getInstance().getEventCount(UsageEventType.VIEW_LIVESTREAM);
//					int ts = UsageLogger.getInstance().getEventCount(UsageEventType.VIEW_TIMESHIFT);
//					double liveRelative = 0;
//					double tsRelative = 0;
//					if(live + ts > 0){
//						double total = (double)live + (double)ts;
//						liveRelative = live / total * 100;
//						tsRelative = ts / total * 100;
//					}
//					DatasetGroup dsGroupLive = new DatasetGroup("Live Stream");
//					datasetPlaymode.setGroup(dsGroupLive);
//					datasetPlaymode.setValue(dsGroupLive.getID(), liveRelative);
//					DatasetGroup dsGroupTimeshift = new DatasetGroup("Timeshift");
//					datasetPlaymode.setGroup(dsGroupTimeshift);
//					datasetPlaymode.setValue(dsGroupTimeshift.getID(), tsRelative);
//
//					
////					for(final UsageEvent ev : UsageLogger.getInstance().getEvents(UsageEventType.QUALITY_CHANGE)){						
////						//seriesQuality.addOrUpdate(ev.getTimestamp().getTime(), Integer.parseInt(ev.getObject().toString()));
////						Minute minute = new Minute(ev.getTimestamp());	
////						TimeSeriesDataItem di = seriesQuality.getDataItem(minute);	
////						if(di != null)
////							seriesQuality.addOrUpdate(minute, (Double.parseDouble(di.getValue().toString()) * ev.getTimestamp().getSeconds() + Double.parseDouble(ev.getObject().toString())) / 60);
////						else
////							seriesQuality.addOrUpdate(minute, Double.parseDouble(ev.getObject().toString()));
////					}
//				}
//			}
//		};
//		UsageLogger.getInstance().addPropertyChangeListener(pclUsageLogger);
		
	}

	private void close(){
		this.setVisible(false);
	}

	private void setValue(int c, int r, double value)
	{
		if (data[0].length <= r * WIDTH + c) {
			//logger.warn("invalid parameter to setValue("+c+","+r+","+value+"), the problem is that"+ data[0].length+" <= "+r * WIDTH + c);
			return;
		}

		data[0][r * WIDTH + c] = c;
		data[1][r * WIDTH + c] = r;
		data[2][r * WIDTH + c] = value;
		dataset.seriesChanged(new SeriesChangeEvent(this));        
	}

	public void init(byte substreams)
	{
		this.data = new double[][] {new double[substreams * WIDTH], new double[substreams * WIDTH], new double[substreams * WIDTH]};
		this.substreams = substreams;
		this.clearStat(substreams);
		dataset.removeSeries("Blocks");
		dataset.addSeries("Blocks", data);
	}


	private void clearStat(byte substreams)
	{
		for (int c = 0; c < WIDTH; c++)
			for (int r = 0; r < substreams; r++)
				setValue(c, r, 0.0);
	}

	private void initStatComponents(byte substreams)
	{
		init(substreams);
		xAxis = new NumberAxis("Block");
		xAxis.setLowerMargin(0.0);
		xAxis.setUpperMargin(0.0);
		NumberAxis yAxis = new NumberAxis("Substream");
		yAxis.setAutoRangeIncludesZero(false);
		yAxis.setInverted(false);
		yAxis.setLowerMargin(0.0);
		yAxis.setUpperMargin(0.0);
		yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		XYBlockRenderer renderer = new XYBlockRenderer();
		LookupPaintScale paintScale = new LookupPaintScale(-0.5, 3.5,
				Color.black);
		paintScale.add(-0.5, Color.white);
		paintScale.add(0.5, Color.green);
		paintScale.add(1.5, Color.orange);
		paintScale.add(2.5, Color.red);
		renderer.setPaintScale(paintScale);
		plot = new XYPlot(dataset, xAxis, yAxis, renderer);
		plot.setDomainCrosshairVisible(true);
		plot.setDomainCrosshairStroke(new BasicStroke(2));
		plot.setDomainCrosshairLockedOnData(false);
		plot.setRangeCrosshairVisible(false);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setForegroundAlpha(0.66f);
		plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
		localStorageChart = new JFreeChart("Local Storage",plot);
		localStorageChart.removeLegend();
		localStorageChart.setBackgroundPaint(Color.white);
		SymbolAxis scaleAxis = new SymbolAxis(null, new String[] {"Absent", "Present",
				"Requested", "Canceled"});
		scaleAxis.setRange(-0.5, 3.5);
		scaleAxis.setPlot(new PiePlot());
		scaleAxis.setGridBandsVisible(false);
		PaintScaleLegend psl = new PaintScaleLegend(paintScale, scaleAxis);
		psl.setAxisOffset(5.0);
		psl.setPosition(RectangleEdge.RIGHT);
		psl.setMargin(new RectangleInsets(5, 5, 5, 5));
		localStorageChart.addSubtitle(psl);
	}

	private int getWindow(int block) {
		return (block / WIDTH);
	}
	private int getWindowPosition(int block) {
		return (block % WIDTH);
	}
	private int getWindowLowerBound(int block) {
		return (WIDTH*(block / WIDTH));
	}

	private void setBlock(SegmentIdentifier segmentIdentifier, int block, int status) {
		if (segmentIdentifier.getSegmentNumber() == SegmentIdentifier.getSegmentNumber(this.playTime) && this.getWindow(SegmentBlock.getBlockNumber(this.playTime))==this.getWindow(block))
			setValue(this.getWindowPosition(block), segmentIdentifier.getSubstream(), status);
	}

	@Override
	public void reset(byte substreams) {
		this.data = new double[][] { new double[substreams * WIDTH], new double[substreams * WIDTH], new double[substreams * WIDTH]};
		this.blockMaps = new HashMap<SegmentIdentifier, byte[]>(substreams*2);
		this.playTime = -1;
		this.currentWindow = -1;
		init(substreams);
	}

	@Override
	public void loadSegment(Segment segment) {

		byte[] statuses = new byte[(int) (Configuration.SEGMENT_SIZE_MS/Configuration.SEGMENTBLOCK_SIZE_MS)];
		int start=0;
		while((start=segment.getSegmentBlockMap().nextSetBit(start))>=0) {
			statuses[start] = 1;

			setBlock(segment.getSegmentIdentifier(), start++, 1);
		}

		this.blockMaps.put(segment.getSegmentIdentifier(), statuses);
	}


	@Override
	public void setPlayPosition(long timeMs) {

		if (this.playTime != timeMs) {
			this.playTime = timeMs;

			int block = SegmentBlock.getBlockNumber(timeMs);

			if (this.segmentNumber != SegmentIdentifier.getSegmentNumber(timeMs) || this.currentWindow != this.getWindow(block)) {

				this.segmentNumber = SegmentIdentifier.getSegmentNumber(timeMs);
				this.currentWindow = this.getWindow(block);

				this.xAxis.setTickUnit(new NumberTickUnit(2, new OffsetIntegerNumberFormat(this.getWindowLowerBound(block))));
				this.localStorageChart.setTitle("Local Storage for Segment "+this.segmentNumber);

				this.clearStat(this.substreams);

				for (Entry<SegmentIdentifier, byte[]> entry : this.blockMaps.entrySet()) 
					for (int i=0;i<entry.getValue().length;i++)
						if (entry.getValue()[i]>0)
							setBlock(entry.getKey(), i, entry.getValue()[i]);
			}

			plot.setDomainCrosshairValue(this.getWindowPosition(block));
		}

	}

	@Override
	public void blockStatChange(SegmentIdentifier segmentIdentifier, int block, int status) {

		byte[] statuses = this.blockMaps.get(segmentIdentifier);

		if (statuses!=null)
			statuses[block] = (byte)status;

		setBlock(segmentIdentifier, block, status);
	}

	@Override
	public void newIncomingRequest(BlockRequestMessage message) {
		((ConnectionTableModel)incomingTable.getModel()).addRequest(message.getSender(), this.getStringType(message), message.getSegmentIdentifier().getChannel(), message.getSegmentIdentifier().getSegmentNumber(), message.getSegmentIdentifier().getSubstream(), (message.getSender().hashCode() ^ message.getSegmentIdentifier().hashCode()<<8 ^ message.getBlockNumber()<<24), message.getBlockNumber());
	}


	@Override
	public void newOutgoingRequest(BlockRequestMessage message, PeerId peerId) {
		((ConnectionTableModel)outgoingTable.getModel()).addRequest(peerId, this.getStringType(message), message.getSegmentIdentifier().getChannel(), message.getSegmentIdentifier().getSegmentNumber(), message.getSegmentIdentifier().getSubstream(), (peerId.hashCode() ^ message.getSegmentIdentifier().hashCode()<<8 ^ message.getBlockNumber()<<24), message.getBlockNumber());
	}

	@Override
	public void updateIncomingRequest(BlockReplyMessage message, PeerId peerId) {

		if (message==null || message.getReplyCode()==null)
			return;

		((ConnectionTableModel)incomingTable.getModel()).updateRequest(message.getReplyCode().name(), peerId.hashCode() ^ message.getSegmentBlock().getSegmentIdentifier().hashCode()<<8 ^ message.getSegmentBlock().getBlockNumber()<<24);		
	}

	@Override
	public void updateOutgoingRequest(BlockReplyMessage message) {

		((ConnectionTableModel)outgoingTable.getModel()).updateRequest(message.getReplyCode().name(), message.getSender().hashCode() ^ message.getSegmentBlock().getSegmentIdentifier().hashCode()<<8 ^ message.getSegmentBlock().getBlockNumber()<<24);

	}

	private String getStringType(AbstractMessage message) {

		String stringType = message.getClass().getCanonicalName();

		return stringType;
	}

	@Override
	public void updateQueueSnapshot(List<Subscriber> queueAndUploadSlots, IncentiveMechanism incentiveMechanism)
	{
		((QueueTableModel) queueTable.getModel()).updateSnapshot(queueAndUploadSlots, incentiveMechanism);

	}
	@Override
	public void updateReputationTable(Map<PeerId, Float> peerReputations)
	{
		this.setReputationTable(peerReputations);
	}

	@Override
	public void addIncentiveMessage(String message) {
		pshTextField.setText(message);
	}

	public void setReputationTable(Map<PeerId, Float> peerReputations)
	{

		if (reputationTable.getModel() == null || oldPeerReputations==null || !oldPeerReputations.equals(peerReputations))
		{
			reputationTable.setModel(new ReputationTableModel(peerReputations));
			oldPeerReputations = peerReputations;
		}
		else
			((ReputationTableModel)reputationTable.getModel()).notifyChange();
	}


	private class OffsetIntegerNumberFormat extends NumberFormat {

		/**
		 * 
		 */
		private static final long serialVersionUID = -1859664308098152800L;
		private final int	offset;

		public OffsetIntegerNumberFormat(int offset) {
			this.offset = offset;
		}

		@Override
		public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {

			StringBuffer sb = new StringBuffer();
			sb.append(this.offset + (int)number);

			return sb;
		}

		@Override
		public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
			StringBuffer sb = new StringBuffer();
			sb.append(this.offset + (int)number);

			return sb;
		}

		@Override
		public Number parse(String source, ParsePosition parsePosition) {
			return null;
		}

	}
}

