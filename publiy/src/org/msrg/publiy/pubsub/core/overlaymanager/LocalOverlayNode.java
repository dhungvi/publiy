package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public class LocalOverlayNode extends OverlayNode implements ILocalOverlayNode {
	
	protected LocalOverlayNode(LocalSequencer localSequencer, InetSocketAddress address) {
		super(localSequencer, address, LocalSequencer.getNodeTypeGeneric());
		_neighbors[0] = this;
		_neighborCount = 1;
		
		//Should never be accessed.
		_overlayNodeState = null;
	}
	
	@Override
	protected void setOverlayNodeState(OverlayNodeState overlayNodeState){
		return;
	}

	@Override
	public OverlayNode getCloserNode() {
		return null;
	} 

	@Override
	public boolean isLocal(){
		return true;
	}
}
