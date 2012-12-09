package org.msrg.publiy.client.subscriber.guisubscriber;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.client.subscriber.SimpleFileSubscriber;
import org.msrg.publiy.node.NodeTypes;



public class GUISubscriber extends SimpleFileSubscriber {
	
	private final GUISubscriberFrame _guiSubscriberFrame;
	
	public GUISubscriber(InetSocketAddress localAddress, BrokerOpState opState,
			InetSocketAddress joinPointAddress, PubForwardingStrategy brokerForwardingStrategy,
			String filename, int delay, Properties arguments)
			throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy, filename, delay, arguments);
		
		_guiSubscriberFrame = new GUISubscriberFrame(getBrokerID(), _defaultSubscriptionListener, _brokerShadow.getBrokerIdentityManager(), _arguments);
		_guiSubscriberFrame.initGui();
	}
	
	public GUISubscriber(InetSocketAddress localAddress, BrokerOpState opState,
			InetSocketAddress joinPointAddress, PubForwardingStrategy brokerForwardingStrategy, 
			Properties props)
			throws IOException {
		super(localAddress, opState, joinPointAddress, brokerForwardingStrategy, props);
		
		_guiSubscriberFrame = new GUISubscriberFrame(getBrokerID(), _defaultSubscriptionListener, _brokerShadow.getBrokerIdentityManager(), _arguments);
		_guiSubscriberFrame.initGui();
	}
	
	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_GUI_SUBSCRIBER;
	}
}
