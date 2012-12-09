package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.pubsub.core.IOverlayManager;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.BrokerIdentityManager;

public class TBFTMessageManipulator_FilterPublication
	extends AbstractThreeCriteriaTBFTMessageManipulator {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -475020894063533602L;
	
	public static final String CMD = "drop";
	
	public TBFTMessageManipulator_FilterPublication(
			int dropCount, Set<InetSocketAddress> affectRemotes,
			Set<InetSocketAddress> affectSources,
			Set<InetSocketAddress> affectSubscribers) {
		super(TCommandTypes.CMND_BFT_PUBLICATION_MANIPULATE_DROP, CMD,
				dropCount, affectRemotes, affectSources, affectSubscribers);
	}
	
	public static TBFTMessageManipulator_FilterPublication decode(
			String str, BrokerIdentityManager idMan) {
		if(str == null)
			return null;
		
		String[] parts = str.split(" ");
		if(parts.length == 0)
			return null;
		
		String cmd = parts[0];
		if(!cmd.equals(getStaticCommand()))
			throw new IllegalArgumentException("str");
		
		Set<InetSocketAddress> affectRemotes =
				new HashSet<InetSocketAddress>();
		Set<InetSocketAddress>  affectSources =
				new HashSet<InetSocketAddress>();
		Set<InetSocketAddress>  affectSubscribers =
				new HashSet<InetSocketAddress>();
		
		int count = Integer.valueOf(parts[1]);
		Set<InetSocketAddress> setToConsider = null;
		for(int i=2 ; i<parts.length ; i++) {
			if(parts[i].equals("to")) {
				setToConsider = affectRemotes;
				i++;
			}
			
			if(parts[i].equals("from")) {
				setToConsider = affectSources;
				i++;
			}
			
			if(parts[i].equals("matching")) {
				setToConsider = affectSubscribers;
			}
			
			String nodeId = parts[i];
			InetSocketAddress nodeAddress = idMan.getNodeAddress(nodeId);
			setToConsider.add(nodeAddress);
		}
		
		return new TBFTMessageManipulator_FilterPublication(
				count, affectRemotes, affectSources, affectSubscribers);
	}

	public static Object getStaticCommand() {
		return CMD;
	}

	@Override
	public IRawPacket manipulatePrivately(IRawPacket checkedOutRaw,
			ISession session, IOverlayManager overlayManager) {
		return null;
	}
}
