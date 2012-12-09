package org.msrg.publiy.client.subscriber.guisubscriber;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.msrg.publiy.broker.BrokerIdentityManager;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.client.subscriber.DefaultSubscriptionListener;

public class GUISubscriberFrame extends JFrame {
	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -2464288118132585723L;
	private Map<String, ReceivedPublicationsPanel> _panels =
			new HashMap<String, ReceivedPublicationsPanel>();
	private JScrollPane _scrollPane;
	private JPanel _viewPortPanel;
	private JLabel _headingLabel;

	private static int MIN_GUI_UPDATE_INTERVAL = 100;
	private Timer _timer = new Timer("GUI_Subscriber" + "-THREAD");
	private final Properties _arguments;
	private final DefaultSubscriptionListener _defaultSubscriptionListener;
	private final String _id;
	private final BrokerIdentityManager _idMan;
	
	public GUISubscriberFrame(String id, DefaultSubscriptionListener defaultSubscriptionListener, BrokerIdentityManager idMan, Properties arguments) {
		super(id);
		setLayout(new BorderLayout());
	
		_id = id;
		_idMan = idMan;
		_arguments = arguments;
		_defaultSubscriptionListener = defaultSubscriptionListener;
		_headingLabel = new JLabel("Heading");
		setHeading("");
		add(_headingLabel, BorderLayout.NORTH);
		
		_scrollPane = new JScrollPane();
		_viewPortPanel = new JPanel();
		_scrollPane.setViewportView(_viewPortPanel);
		_viewPortPanel.setLayout(new GridLayout(0, 1));
		add(_scrollPane, BorderLayout.CENTER);
	}
	
	private void setHeading(String subsStr) {
		String headingStr = "Node '" + _id + "' subscribing to: " + subsStr;
		_headingLabel.setText(headingStr);
		doLayout();
	}
	
	protected Point getPreferredLocation() {
		int x = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_X, 0);
		int y = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_Y, 0);
		return new Point(x, y);
	}

	protected Dimension getFramePreferredSize() {
		int width = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_WIDTH, 600);
		int height = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_HEIGHT, 150);
		return new Dimension(width, height);
	}

	public void initGui() {
		pack();
		setSize(getFramePreferredSize());
		setAlwaysOnTop(true);
		setVisible(true);
		setLocation(getPreferredLocation());

		_timer.schedule(new GUISubscriberUpdateTask(), 0, MIN_GUI_UPDATE_INTERVAL);
	}

	class GUISubscriberUpdateTask extends TimerTask{
		@Override
		public void run() {
			updateGUI();
		}
	}
	
	private String formatSubs() {
		String subsStr = "";
//		Subscription[] subs = getConfirmedSubscriptions();
//		if(subs.length == 0)
//			return "None";
//		
//		for(int i=0 ; i<subs.length ; i++)
//			if(i== subs.length -1)
//				subsStr = subs[i].toString();
//			else
//				subsStr = subs[i].toString() + "  -  ";

		return subsStr;
	}
	
	private ReceivedPublicationsPanel getPublisherPanel(String publisherId) {
		ReceivedPublicationsPanel publisherPanel = _panels.get(publisherId);
		if(publisherPanel!=null)
			return publisherPanel;
		
		publisherPanel = new ReceivedPublicationsPanel(publisherId);
		_panels.put(publisherId, publisherPanel);
		_viewPortPanel.add(publisherPanel);
		_viewPortPanel.doLayout();
//		pack();
		return publisherPanel;
	}
	
	private synchronized void updateGUI() {
//		PublicationInfo lastPublicationInfo = _defaultSubscriptionListener.getLastReceivedPublications();
//		if(lastPublicationInfo == null)
//			return;
//		Publication lastPublication = lastPublicationInfo.getPublication();
//		if(lastPublication == null)
//			return;
//		
//		List<Integer> countList = lastPublication.getValue("count");
//		Integer pubCount = (countList == null || countList.size() == 0) ? -1 : countList.get(0);

		Map<String, Publication> lastPublicationsDelivered = _defaultSubscriptionListener.getLastPublicationDeliveredPerPublisher();
		Map<String, Integer> allDeliveryCounters = _defaultSubscriptionListener.getDeliveredPublicationCounterPerPublisher(); //(countList==null || countList.isEmpty() ? -1 : countList.get(0));
		Map<String, Long> allDeliveryTimes = _defaultSubscriptionListener.getLastPublicationDeliveryTimesPerPublisher();
		
		synchronized (allDeliveryCounters) {
			for(Entry<String, Integer> counter : allDeliveryCounters.entrySet()) {
				String nodeAddr = counter.getKey();
				Publication lastPublication = lastPublicationsDelivered.get(nodeAddr);
				int pubCount = -2;
				if(lastPublication != null) {
					List<Integer> pubCountList = lastPublication.getValue(SimpleFilePublisher.getGeneralCounterAttributeName());
					if(pubCountList == null)
						pubCount = -1;
					else
						pubCount = pubCountList.get(0);
				}
						
				OverlayNodeId nodeId = _idMan.getBrokerId(nodeAddr);
				String nodeIdStr = "" +  nodeId;
				Integer deliveredCount = counter.getValue();
				int count = deliveredCount == null ? -10 : deliveredCount;
				ReceivedPublicationsPanel publisherPanel = getPublisherPanel(nodeIdStr + "[" + nodeAddr + "]");
				publisherPanel.updateLabel(pubCount, count, allDeliveryTimes.get(nodeAddr));
			}
		}
		
		String subsStr = formatSubs();
		setHeading(subsStr);
		pack();
	}
}
