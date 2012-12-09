package org.msrg.publiy.tests.commons;

import java.util.BitSet;

import org.msrg.publiy.pubsub.core.overlaymanager.NodeCache;

public class NodeCacheBitSet {

	public static String toStringNodeCache(NodeCache node, NodeCache[] nodes){
		BitSet closersBV = node.getMaskedClosers();
		BitSet farthersBV = node.getFarthers();
		
		String str = "#" + node.getIndex() + ": " + 
						node.getAddress().getPort() +
						" _Closer: " + toStringBV(closersBV, nodes) +
						" _Farther: " + toStringBV(farthersBV, nodes);
		
		return str;
	}

	public static String toStringBV(BitSet bv, NodeCache[] nodes){
		String str = "";
		for ( int i=0 ; i<nodes.length ; i++ )
			if ( bv.get(i) ) 
				str += nodes[i].getAddress().getPort() + ", ";
		
		return str;
	}
}
