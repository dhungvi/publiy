package org.msrg.publiy.tests.utils.mock;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.sessions.ISessionManager;

import org.msrg.publiy.pubsub.core.multipath.WorkingManagersBundle;

public class MockWorkingTopologySubscriptionBundle {

	final MockMasterTopologySubscriptionBundle _mockBundle;

	final Map<InetSocketAddress, MockWorkingTopologySubscriptionNode> _workingNodes = new HashMap<InetSocketAddress, MockWorkingTopologySubscriptionNode>();

	MockWorkingTopologySubscriptionBundle(Map<InetSocketAddress, IBrokerShadow> brokerShadows, String dirname) throws IOException {
		this(brokerShadows, new MockMasterTopologySubscriptionBundle(LocalSequencer.init(null, Sequence.getRandomAddress()), dirname));
	}
	
	public MockWorkingTopologySubscriptionBundle(Map<InetSocketAddress, IBrokerShadow> brokerShadows, MockMasterTopologySubscriptionBundle mockBundle) {
		this(brokerShadows, mockBundle, getSoftAddressesFor(), getCandidateAddressesFor());
	}
	
	public MockWorkingTopologySubscriptionBundle(
			Map<InetSocketAddress, IBrokerShadow> brokerShadows,
			MockMasterTopologySubscriptionBundle mockBundle,
			Map<InetSocketAddress, InetSocketAddress[]> softAddressesMap,
			Map<InetSocketAddress, InetSocketAddress[]> canidateAddressesMap) {
		_mockBundle = mockBundle;
		
		MockMasterTopologySubscriptionNode[] masterNodes = _mockBundle.getMasterNodes();
		for (int i=0 ; i<masterNodes.length ; i++) {
			InetSocketAddress nodeAddress = masterNodes[i]._nodeAddress;
			LocalSequencer localSequencer = LocalSequencer.init(null, nodeAddress);

			_workingNodes.put(nodeAddress,
					new MockWorkingTopologySubscriptionNode(
							masterNodes[i],
							softAddressesMap.get(masterNodes[i]),
							canidateAddressesMap.get(masterNodes[i])));
		}
	}
	
	public WorkingManagersBundle getWorkingManagersBundle(InetSocketAddress nodeAddress) {
		return _workingNodes.get(nodeAddress)._workingBundle;
	}
	
	private static Map<InetSocketAddress, InetSocketAddress[]> getSoftAddressesFor() {
		return new HashMap<InetSocketAddress, InetSocketAddress[]>();
	}
	
	private static Map<InetSocketAddress, InetSocketAddress[]> getCandidateAddressesFor() {
		return new HashMap<InetSocketAddress, InetSocketAddress[]>();	
	}
}
