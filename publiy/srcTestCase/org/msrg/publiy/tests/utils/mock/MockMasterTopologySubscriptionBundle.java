package org.msrg.publiy.tests.utils.mock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Subscription;
import org.msrg.publiy.utils.FileExtensionFilter;

public class MockMasterTopologySubscriptionBundle {
	
	protected final int _delta = 3;
	private final String _setupDirname;
	static private Pattern topFilenamePattern = Pattern.compile("(.*)_(.*)\\.top");
	static private Pattern subscriptionLinePattern = Pattern.compile("Sub\\(\\[(.*)\\]\\) _ /([^ ]*).*");
	private MockMasterTopologySubscriptionNode[] _masterNodes;
	protected final InetSocketAddress[] _allAddresses;
	
	public MockMasterTopologySubscriptionBundle(LocalSequencer localSequencer, String setupDirname) throws IOException {
		_setupDirname = setupDirname;
		
		File dir = new File(_setupDirname);
		if (!dir.isDirectory())
			throw new IllegalArgumentException("Directory does not exist: " + _setupDirname);
		
		FilenameFilter filter = new FileExtensionFilter("", "top");
		File[] topFiles = dir.listFiles(filter);
		_masterNodes = new MockMasterTopologySubscriptionNode[topFiles.length];
		_allAddresses = new InetSocketAddress[topFiles.length];
		for (int i=0 ; i<topFiles.length ; i++) {
			String topname = topFiles[i].getName();
			Matcher matcher = topFilenamePattern.matcher(topname);
			if (!matcher.matches() || matcher.groupCount() != 2)
				throw new IllegalStateException("Filename does not match: " + topname);
			
			String addressStr = matcher.group(1);
			String portStr = matcher.group(2);
			InetSocketAddress nodeAddress = new InetSocketAddress(addressStr, new Integer(portStr));
			_allAddresses[i] = nodeAddress;
			
			BufferedReader topReader = new BufferedReader(new FileReader(topFiles[i]));
			List<TRecovery_Join> trjs = new LinkedList<TRecovery_Join>();
			while(true) {
				String line = topReader.readLine();
				if (line==null) break;

				trjs.add(TRecovery_Join.getTRecoveryObject(localSequencer, line));
			}
			
			String subsFilename = _setupDirname + "/" + addressStr + "_" + portStr + ".subs";
			File subsFile = new File(subsFilename);
			BufferedReader subReader = new BufferedReader(new FileReader(subsFile));
			List<TRecovery_Subscription> trss = new LinkedList<TRecovery_Subscription>();
			while(true) {
				String line = subReader.readLine();
				if (line==null) break;
				trss.add(getTRecovery_Subscription(localSequencer, line, true));
			}
			IBrokerShadow brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, nodeAddress).setDelta(_delta);
			_masterNodes[i] = new MockMasterTopologySubscriptionNode(brokerShadow,
					trjs.toArray(new TRecovery_Join[0]),
					trss.toArray(new TRecovery_Subscription[0]));
		}
	}
	
	static TRecovery_Subscription getTRecovery_Subscription(LocalSequencer localSequencer, String line, boolean realFrom) {
		Matcher matcher = subscriptionLinePattern.matcher(line);
		if (!matcher.matches() | matcher.groupCount()!=2)
			throw new IllegalStateException("Subscription line does not match pattern: " + line);
		String predicates = matcher.group(1);
		String fromStr = matcher.group(2);
		InetSocketAddress from = new InetSocketAddress(fromStr.split(":")[0], new Integer(fromStr.split(":")[1]));
		
		Subscription subscription = Subscription.decodeRegex(predicates);
		TRecovery_Subscription trs = new TRecovery_Subscription(subscription, localSequencer.getNext(), from);
		return trs;
	}
	
	void write(Writer ioWriter) throws IOException {
		write(ioWriter, ',');
	}
	
	void write(Writer ioWriter, char delim) throws IOException {
		for (MockMasterTopologySubscriptionNode node : _masterNodes) {
			ioWriter.write(node.toString() + delim);
		}
	}
	
	public static void main(String[] argv) throws IOException {
		String dirname = "Auxiliary/Topologies/ML";
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 1000));
		MockMasterTopologySubscriptionBundle mockT = new MockMasterTopologySubscriptionBundle(localSequencer, dirname);
		StringWriter strWriter = new StringWriter();
		mockT.write(strWriter, '\n');
		System.out.println(strWriter.toString());
	}

	public MockMasterTopologySubscriptionNode[] getMasterNodes() {
		return _masterNodes;
	}

	public InetSocketAddress[] getNodeAddresses() {
		return _allAddresses;
	}
}

