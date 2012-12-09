package org.msrg.publiy.pubsub.core.overlaymanager;

import java.net.InetSocketAddress;

public class Path<T extends INode> {

	protected final T [] _nodes;
	protected int _length = 0;
	
	private Path(T[] nodes){
		_nodes = nodes;
	}
	
	static Path<NodeCache> createNodesCachePath(int neighborhoodRadious){
		Path<NodeCache> path = new Path<NodeCache>(new NodeCache[neighborhoodRadious]);
		return path;
	}
	
	static Path<INode> createOverlayNodesPath(Path<NodeCache> cachePath, int neighborhoodRaious){
		Path<INode> path = createOverlayNodesPath(neighborhoodRaious);
		
		for ( int i=0 ; i<cachePath._length ; i++ )
			path.addNode(cachePath._nodes[i]._remoteNode);
		
		return path;
	}
	
	static Path<INode> createOverlayNodesPath(int neighborhoodRadious){
		Path<INode> path = new Path<INode>(new OverlayNode[neighborhoodRadious]);
		return path;
	}
	
	public int getLength(){
		return _length;
	}
	
	public boolean intersect(Path<T> p){
		for ( int i=0 ; i<_length ; i++ ){
			for ( int j=0 ; j<p._length ; j++ ){
				if ( this._nodes[i].equals(p._nodes[j]) )
					return true;
			}
		}
		
		return false;
	}
	
	public boolean passes(OverlayNode node){
		return passes(node._address);
	}
	
	public boolean passes(InetSocketAddress x){
		if ( x == null )
			return false;
		for ( int i=0  ; i<_length ; i++ )
			if ( _nodes[i].equals(x) )
				return true;
		return false;
	}
	
	public void addNodeToBegining(T node){
		if ( node == null )
			throw new NullPointerException();
		
		for ( int i=_length ; i>0 ; i-- )
			_nodes[i] = _nodes[i-1];
		
		_nodes[0] = node;
		_length++;
	}
	
	public void addNode(T node){
		if ( node == null )
			throw new NullPointerException();
		
		_nodes[_length] = node;
		_length ++;
	}
	
	public InetSocketAddress [] getAddresses(){
		InetSocketAddress [] addresses = new InetSocketAddress[_length];
		for ( int i=0 ; i<_length ; i++ )
			addresses[i] = _nodes[i].getAddress();
		return addresses;
	}
	
	public T getFirst(){
		return _nodes[0];
	}
	
	public T getLast(){
		if ( _length == 0 )
			return null;
		return _nodes[_length-1];
	}
	
	public T get(int i){
		return _nodes[i];
	}
	
	public InetSocketAddress[] getReverseAddresses(){
		InetSocketAddress[] reverseAddresses = new InetSocketAddress[_length];
		for ( int i=0 ; i<this._length ; i++ )
			reverseAddresses[i] = this._nodes[_length-i-1].getAddress();
		
		return reverseAddresses;
	}
	
	@Override
	public String toString(){
		String str = "P[";
		for ( int i=0 ; i<_length ; i++ )
			str += _nodes[i] + ",";
		return str + "]";
	}	
}
