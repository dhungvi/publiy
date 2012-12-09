package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.util.Set;

import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.INode;
import org.msrg.publiy.pubsub.core.overlaymanager.Path;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.core.sequence.Sequence;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

public abstract class AbstractThreeCriteriaTBFTMessageManipulator extends
		TBFTMessageManipulator {
	
	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -7152108462319828456L;
	protected final String _command;
	protected final Set<InetSocketAddress> _affectRemotes;
	protected final Set<InetSocketAddress> _affectSources;
	protected final Set<InetSocketAddress> _affectSubscribers;
	protected int _dropCount;
	
	public AbstractThreeCriteriaTBFTMessageManipulator(
			TCommandTypes type, String command,
			int dropCount, Set<InetSocketAddress> affectRemotes,
			Set<InetSocketAddress> affectSources,
			Set<InetSocketAddress> affectSubscribers) {
		super(type , "Manipulator");
		
		_command = command;
		
		_affectRemotes = affectRemotes;
		_affectSources = affectSources;
		_affectSubscribers = affectSubscribers;
		
		_dropCount = dropCount;
	}

	public abstract IRawPacket manipulatePrivately(IRawPacket checkedOutRaw,
			ISession session, IOverlayManager overlayManager);
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("[" + _dropCount + "]");
		sb.append(Writers.write(_affectRemotes));
		sb.append(Writers.write(_affectSources));
		sb.append(Writers.write(_affectSubscribers));
		
		return sb.toString();
	}
	
	@Override
	public IRawPacket manipulate(IRawPacket checkedOutRaw,
			ISession session, IOverlayManager overlayManager) {
		if(checkedOutRaw == null)
			return null;
		
		if(checkedOutRaw.getType() != PacketableTypes.TMULTICAST)
			return checkedOutRaw;
		
		if(TMulticast.getTMulticastType(checkedOutRaw) !=
				TMulticastTypes.T_MULTICAST_PUBLICATION_BFT)
			return checkedOutRaw;
		
		if(_dropCount == 0)
			return checkedOutRaw;
		
		if(_affectRemotes == null)
			return null;
		
		if(_affectSubscribers == null)
			return null;
		
		if(_affectSources == null)
			return null;
		
		InetSocketAddress remote = session.getRemoteAddress();
		if(_affectRemotes.contains(remote)) {
			_dropCount--;
			return manipulatePrivately(checkedOutRaw, session, overlayManager);
		}
		
		Sequence sourceSequence = checkedOutRaw.getSourceSequence();
		if(sourceSequence != null) {
			InetSocketAddress source = sourceSequence.getAddress();
			if(source != null) {
				if(_affectSources.contains(source)) {
					_dropCount--;
					return null;
				}
			}
		}
		
		for(InetSocketAddress subscriberRemote : _affectSubscribers) {
			Path<INode> pathFromSubscriber =
					overlayManager.getPathFrom(subscriberRemote);
			if(pathFromSubscriber != null) {
				if(pathFromSubscriber.passes(remote)) {
					_dropCount--;
					return null;
				}
			}
		}
		
		return checkedOutRaw;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!this.getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TBFTMessageManipulator_FilterPublication manipulatorObj =
				(TBFTMessageManipulator_FilterPublication) obj;
		if(_dropCount != manipulatorObj._dropCount)
			return false;
		
		if(!_affectRemotes.equals(manipulatorObj._affectRemotes))
			return false;
		
		if(!_affectSources.equals(manipulatorObj._affectSources))
			return false;
		
		if(!_affectSubscribers.equals(manipulatorObj._affectSubscribers))
			return false;
		
		return true;
	}

	public String getCommand() {
		return _command;
	}
	
	@Override
	public String toString(BrokerIdentityManager idMan) {
		StringBuilder sb = new StringBuilder();
		sb.append(getCommand());
		sb.append(" ");
		sb.append(_dropCount);
		
		if(_affectRemotes.size() != 0) {
			sb.append(" " + "to");
			for(InetSocketAddress remote : _affectRemotes) {
				String remoteStr = remote.toString();
				OverlayNodeId remoteNode = idMan.getBrokerId(remoteStr);
				String nodeId = remoteNode.getNodeIdentifier();
				sb.append(" " + nodeId);
			}
		}
		
		if(_affectSources.size() != 0) {
			sb.append(" " + "from");
			for(InetSocketAddress remote : _affectSources) {
				String nodeId = idMan.getBrokerId(
						remote.toString()).getNodeIdentifier();
				sb.append(" " + nodeId);
			}
		}
		
		if(_affectSubscribers.size() != 0) {
			sb.append(" " + "matching");
			for(InetSocketAddress remote : _affectSubscribers) {
				String nodeId = idMan.getBrokerId(
						remote.toString()).getNodeIdentifier();
				sb.append(" " + nodeId);
			}
		}
		
		return sb.toString();
	}
}
