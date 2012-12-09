package org.msrg.publiy.broker.core.nodes;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.msrg.publiy.utils.PropertyGrabber;

public class OverlayNodeId implements Comparable<OverlayNodeId>, Serializable {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = -3045888497912064515L;
	protected String _identifier;
	protected InetSocketAddress _adderss;
	protected transient Properties _nodeProperties;
	
	public InetSocketAddress getNodeAddress() {
		return _adderss;
	}
	
	protected OverlayNodeId(String id) {
		if ( id == null )
			throw new IllegalArgumentException("Identifier cannot be null");
		_identifier = id;
		_nodeProperties = new Properties();
	}
	
	public String getNodeIdentifier(){
		return _identifier;
	}

	private boolean equalsStr(String strId){
		return _identifier.toString().equalsIgnoreCase(strId);
	}
	
	public boolean equals(Object obj){
		if ( obj == null )
			return false;
		
		if ( obj.getClass() == String.class )
			return equalsStr((String)obj);
			
		if ( obj.getClass() != this.getClass() )
			return false;
		
		OverlayNodeId node = (OverlayNodeId) obj;
		return _identifier.equals(node._identifier);
	}
	
	@Override
	public String toString(){
		return _identifier.toString();
	}
	
	public String getFileName(){
		return _identifier.toString() + PropertyGrabber.PROPERTY_FILE_EXTENSION;
	}
	
	public Properties getProperties(){
		if ( _nodeProperties == null )
			return null;
		
		String nodenameInProsp = _nodeProperties.getProperty(PropertyGrabber.PROPERTY_NODE_NAME);
		if ( !_identifier.equals(nodenameInProsp) )
			throw new IllegalStateException("NodeID '" + this + "' does not match nodename in properties: '" + nodenameInProsp + "'.");
		return _nodeProperties;
	}

	protected void changeJoinPointName(String oldJoinPointName, String newJoinPointName){
		String oldValue = _nodeProperties.getProperty(PropertyGrabber.PROPERTY_JOINPOINT_NAME);
		if ( oldValue == null )
			return;
		
		oldValue.trim();
		if ( oldValue.equalsIgnoreCase("") || oldJoinPointName.equalsIgnoreCase("null") )
			return;
		
		if ( oldValue.equalsIgnoreCase(newJoinPointName) )
			_nodeProperties.setProperty(PropertyGrabber.PROPERTY_JOINPOINT_NAME, newJoinPointName);
	}

	@Override
	public int compareTo(OverlayNodeId o) {
		return _identifier.compareTo(o._identifier);
	}
}
