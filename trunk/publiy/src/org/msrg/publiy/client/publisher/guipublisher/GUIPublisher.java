package org.msrg.publiy.client.publisher.guipublisher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.client.subscriber.guisubscriber.GUISubscriberFrame;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;


public class GUIPublisher extends SimpleFilePublisher implements ISimpleGUIPublisher {
	
	private final GUIPublisherFrame _guiPublisherFrame;
	private final GUISubscriberFrame _guiSubscriberFrame;
	
	public GUIPublisher(InetSocketAddress localAddress, 
			BrokerOpState opState, 
			InetSocketAddress joinPointAddress, 
			PubForwardingStrategy brokerForwardingStrategy, 
			Properties props)
			throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy, props);
		
		_guiSubscriberFrame = new GUISubscriberFrame(getBrokerID(), _defaultSubscriptionListener, _brokerShadow.getBrokerIdentityManager(), _arguments);
		_guiSubscriberFrame.initGui();
		
		_guiPublisherFrame = new GUIPublisherFrame(this, getBrokerID(), _arguments);
		_guiPublisherFrame.initGUI();
	}
	
	@Override
	protected void prepareReadPublication(Publication pub) {
		super.prepareReadPublication(pub);
	}

	public TMulticast_Publish publish(Publication publication, ITMConfirmationListener tmConfirmationListener) {
		TMulticast_Publish tmp = super.publish(publication, tmConfirmationListener);
		return tmp;
	}
	
	@Override
	public synchronized void brokerOpStateChanged(IBroker broker) {
		super.brokerOpStateChanged(broker);
		
		if(getBrokerOpState() == BrokerOpState.BRKR_PUBSUB_PS)
			_guiPublisherFrame.enableButton(true);
	}
	
	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_GUI_PUBLISHER;
	}
	
	@Override
	public boolean pause() {
		boolean ret = super.pause();
		_guiPublisherFrame.refreshPauseButton();
		return ret;
	}

	@Override
	public boolean play() {
		boolean ret = super.play();
		_guiPublisherFrame.refreshPauseButton();
		return ret;
	}

	@Override
	public boolean setSpeedLevel(int interval) {
		if(!super.setSpeedLevel(interval))
			return false;
		
		_guiPublisherFrame.setSpeed(interval);
		return true;
	}
	
	@Override
	public int getDelay() {
		return super.getDelay();
	}
	
	@Override
	public boolean isPaused() {
		return _paused;
	}
}
