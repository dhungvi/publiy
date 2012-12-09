package org.msrg.publiy.broker.info;

import java.net.InetSocketAddress;

import org.msrg.publiy.node.NodeTypes;


public class JoinInfo extends IBrokerInfo {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 2875275262983546381L;
	private final InetSocketAddress _nodeAddress1;
	private final NodeTypes _nodeTypes1;
	private final InetSocketAddress _nodeAddress2;
	private final NodeTypes _nodeTypes2;
	
	public JoinInfo(InetSocketAddress nodeAddress1, NodeTypes nodeTypes1, 
			InetSocketAddress nodeAddress2, NodeTypes nodeTypes2) {
		super(BrokerInfoTypes.BROKER_INFO_JOINS);
		_nodeTypes1 = nodeTypes1;
		_nodeAddress1 = nodeAddress1;
		_nodeAddress2 = nodeAddress2;
		_nodeTypes2 = nodeTypes2;
	}
	
	public InetSocketAddress getNodeAddress1(){
		return _nodeAddress1;
	}
	
	public InetSocketAddress getNodeAddress2(){
		return _nodeAddress2;
	}
	
	@Override
	protected String toStringPrivately() {
		return _nodeAddress1 + "[" + _nodeTypes1.getCodedChar() + "]-" 
				+ _nodeAddress2 + "[" + _nodeTypes2.getCodedChar() + "]";
	}
}
