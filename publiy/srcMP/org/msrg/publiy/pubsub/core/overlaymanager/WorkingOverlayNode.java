package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;


import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.overlaymanager.OverlayNode;

public class WorkingOverlayNode extends OverlayNode {

	protected WorkingOverlayNode(LocalSequencer localSequencer, InetSocketAddress address, NodeTypes nodeType,
			WorkingOverlayNode closerNode) {
		super(localSequencer, address, nodeType, closerNode);
	}

}
