package org.msrg.publiy.client.networkcoding.subscriber;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.msrg.publiy.networkcodes.engine.CodingEngineLogger;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualContentLogger;


import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.client.multipath.subscriber.FileSubscriber_MP;

public class FileSubscriber_NC extends FileSubscriber_MP {

	public final InetSocketAddress _clientsJoiningBrokerAddress;
	
	public FileSubscriber_NC(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, Properties props)
			throws IOException {
		super(localAddress, opState, null, brokerForwardingStrategy, props);
		
		_clientsJoiningBrokerAddress = joinPointAddress;
		
		if(!_brokerShadow.isNC())
			throw new IllegalStateException();
		
		if(!_brokerShadow.isMP())
			throw new IllegalStateException();
		
		_contentLogger = new CasualContentLogger((BrokerShadow)_brokerShadow);
		_codingEngineLogger = new CodingEngineLogger((BrokerShadow)_brokerShadow);
	}

	public FileSubscriber_NC(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, String filename,
			int delay, Properties arguments) throws IOException {
		super(localAddress, opState, null, brokerForwardingStrategy,
				filename, delay, arguments);
		
		_clientsJoiningBrokerAddress = joinPointAddress;

		if(!_brokerShadow.isNC())
			throw new IllegalStateException();
		
		if(!_brokerShadow.isMP())
			throw new IllegalStateException();
		
		_contentLogger = new CasualContentLogger((BrokerShadow)_brokerShadow);
		_codingEngineLogger = new CodingEngineLogger((BrokerShadow)_brokerShadow);
	}
	
	@Override
	public void prepareToStart() {
		_casualLoggerEngine.registerCasualLogger(_contentLogger);
		_casualLoggerEngine.registerCasualLogger(_codingEngineLogger);
		super.prepareToStart();
	}

	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_NC_SUBSCRIBER;
	}
	
	@Override
	public InetSocketAddress getClientsJoiningBrokerAddress() {
		return _clientsJoiningBrokerAddress;
	}
}
