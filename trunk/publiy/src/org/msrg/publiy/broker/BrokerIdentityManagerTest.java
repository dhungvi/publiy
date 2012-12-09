package org.msrg.publiy.broker;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class BrokerIdentityManagerTest extends TestCase {
	String[] ids = {"d1", "d2", "d3", "pd1", "pd2", "pd3", "pu1", "pu2", "pu3",
			"r1", "r2", "r3", "sd1", "sd2", "sd3", "su1", "su2", 
			"su3", "u1", "u2", "u3"};
	
	String[][] joinpoints = {
			{"pd1","d1"},
			{"pd2","d2"},
			{"pd3","d3"},
			{"pu1","u1"},
			{"pu2","u2"},
			{"pu3","u3"},
			{"sd1","d1"},
			{"sd2","d2"},
			{"sd3","d3"},
			{"su1","u1"},
			{"su2","u2"},
			{"su3","u3"}};

	protected String _idFileContent;
	protected BrokerIdentityManager _idManager;

	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		Map<String, String> joinpointsMap = new HashMap<String, String>();
		for(String[] joinpoint : joinpoints)
			joinpointsMap.put(joinpoint[0], joinpoint[1]);
		
		_idFileContent = "";
		for(int i=0 ; i<ids.length ; i++) {
			String jp = joinpointsMap.get(ids[i]);
			String jpStr = (jp==null?"":"\t"+jp);
			_idFileContent += (ids[i] + "\t" + new InetSocketAddress("127.0.0.1", 2000 + i) + jpStr + "\n");
		}
		
		_idManager = new BrokerIdentityManager(new InetSocketAddress("127.0.0.1", 2000), 1);
		_idManager.loadIdContent(_idFileContent.split("\n"));
	}
	
	public void testIdLoading() {
		System.out.println(_idManager.toString());
		for(int i=0 ; i<ids.length ; i++) {
			String addrStr = "127.0.0.1:" + (2000 + i);
			OverlayNodeId nodeId = _idManager.getBrokerId(addrStr);

			assertTrue(nodeId!=null);
			assertTrue(nodeId == _idManager.getBrokerId(nodeId.getNodeIdentifier()));
			
			addrStr = "/127.0.0.1:" + (2000 + i);
			nodeId = _idManager.getBrokerId(addrStr);

			assertTrue(nodeId!=null);
			assertTrue(nodeId == _idManager.getBrokerId(nodeId.getNodeIdentifier()));
		}
		
		for(String[] joinpoint : joinpoints) {
			InetSocketAddress jpAddress = _idManager.getJoinpointAddress(joinpoint[0]);
			assertNotNull(jpAddress);
			
			OverlayNodeId jpId = _idManager.getBrokerId(jpAddress.toString());
			assertNotNull(jpId);
			
			assertTrue(jpId.equals(joinpoint[1]));
		}
	}
}
