package org.msrg.publiy.node;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.PubForwardingStrategy;

import org.msrg.publiy.utils.PropertyGrabber;


public class NodeInstantiationData<T extends IBroker> implements Serializable {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -3225449642704255305L;

	public transient static final Class<?>[] BROKER_INSTANTIATION_DATA_ARG_TYPES = new Class[]{InetSocketAddress.class, BrokerOpState.class, InetSocketAddress.class, PubForwardingStrategy.class, Properties.class};
	public transient static final int JOINPOINT_ADDRESS_INDEX = 2;
	
//	protected final byte[] _dummy = new byte[1000];
	protected final Class<T> _xClass;
	protected final InetSocketAddress _nodeAddress;
	protected final InetSocketAddress _nodeJoinPointAddress;
	protected final BrokerOpState _nodeInitialOpState;
	protected final Properties _arguments;
	protected final PubForwardingStrategy _brokerForwardingStrategy;
	
	public NodeInstantiationData(Class<T> xClass, InetSocketAddress brokerAddress, BrokerOpState initialBrokerState, InetSocketAddress brokerJoinPoinAddress, PubForwardingStrategy brokerForwardingStrategy, Properties arguments){
		_xClass = xClass;
		_nodeAddress = brokerAddress;
		_nodeJoinPointAddress = brokerJoinPoinAddress;
		_nodeInitialOpState = initialBrokerState;
		_brokerForwardingStrategy = brokerForwardingStrategy;
		if ( arguments == null )
			_arguments = new Properties();
		else
			_arguments = arguments;
		if ( _nodeJoinPointAddress != null && _arguments != null )
			try{_arguments.load(new StringReader(PropertyGrabber.PROPERTY_JOINPOINT_ADDRESS+"="+_nodeJoinPointAddress.toString().substring(1)));}catch(IOException iox){}
	}
	
	public final Class<T> getXClass(){
		return _xClass;
	}
	
	public Class<?>[] getInstantiationArgTypes(){
		return BROKER_INSTANTIATION_DATA_ARG_TYPES;
	}
	
	public Properties getProperties(){
		return _arguments;
	}
	
	public String getProperty(String propertyName){
		if ( _arguments == null )
			return null;
		else
			return _arguments.getProperty(propertyName);
	}
	
	public Object[] getInstantiationData(){
		return new Object[]{_nodeAddress, _nodeInitialOpState, _nodeJoinPointAddress, _brokerForwardingStrategy, _arguments};
	}
	
	public String toString(){
		return "NodeInstantiationData[" + _xClass + ", " + _nodeAddress + ", " + _nodeInitialOpState + ", " + _nodeJoinPointAddress + ", " + _brokerForwardingStrategy + ", " + _arguments + "]";
	}
	
	public boolean validate(InetSocketAddress brokerAddress, BrokerOpState opState){
		if ( !brokerAddress.equals(_nodeAddress) )
			return false;
		
		if ( opState != _nodeInitialOpState )
			return false;
		
		return true;
	}
}
