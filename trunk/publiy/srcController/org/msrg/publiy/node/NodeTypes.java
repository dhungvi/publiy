package org.msrg.publiy.node;

import java.io.Serializable;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.client.multipath.publisher.FilePublisher_MP;
import org.msrg.publiy.client.multipath.subscriber.FileSubscriber_MP;
import org.msrg.publiy.client.networkcoding.publisher.FilePublisher_NC;
import org.msrg.publiy.client.networkcoding.subscriber.FileSubscriber_NC;
import org.msrg.publiy.client.publisher.SimpleFilePublisher;
import org.msrg.publiy.client.publisher.guipublisher.GUIPublisher;
import org.msrg.publiy.client.subscriber.SimpleFileSubscriber;
import org.msrg.publiy.client.subscriber.guisubscriber.GUISubscriber;


class NodeTypesIndexCounterClass {
	static byte _index = 0;
}

public enum NodeTypes implements Serializable {

	NODE_BROKER 		(Broker.class, 					false, 	false, 	false,	false, 	NodeTypesIndexCounterClass._index++,	'B'),
	NODE_SUBSCRIBER 	(SimpleFileSubscriber.class,	false, 	true, 	false,	true,	NodeTypesIndexCounterClass._index++,	'P'),
	NODE_PUBLISHER 		(SimpleFilePublisher.class, 	false, 	false, 	true,	true,	NodeTypesIndexCounterClass._index++,	'S'),

	NODE_GUI_SUBSCRIBER (GUISubscriber.class, 			true, 	true, 	false,	true,	NodeTypesIndexCounterClass._index++,	'Q'),
	NODE_GUI_PUBLISHER 	(GUIPublisher.class, 			true, 	false, 	true,	true,	NodeTypesIndexCounterClass._index++,	'W'),
	
	NODE_MP_SUBSCRIBER 	(FileSubscriber_MP.class,		false, 	true, 	false,	true,	NodeTypesIndexCounterClass._index++,	'R'),
	NODE_MP_PUBLISHER 	(FilePublisher_MP.class, 		false, 	false, 	true,	true,	NodeTypesIndexCounterClass._index++,	'L'),
	
	NODE_NC_SUBSCRIBER 	(FileSubscriber_NC.class,		false, 	true, 	false,	true,	NodeTypesIndexCounterClass._index++,	'N'),
	NODE_NC_PUBLISHER 	(FilePublisher_NC.class, 		false, 	false, 	true,	true,	NodeTypesIndexCounterClass._index++,	'C');
	
	private Class<?> _xClass;
	private boolean _isSubscriber;
	private boolean _isPublisher;
	private boolean _mustBeLocalHost;
	
	private final boolean _isClient;
	private final char _codedChar;
	private final byte _codedByte;
	
	public boolean isSubscriber() {
		return _isSubscriber;
	}
	
	public boolean isPublisher() {
		return _isPublisher;
	}
	
	public static NodeTypes getNodeType(String str) {
		if(str == null)
			return null;
		
		str = str.trim();
		NodeTypes[] allTypes = values();
		for(int i=0 ; i<allTypes.length ; i++)
			if(str.equalsIgnoreCase(allTypes[i].toString()))
				return allTypes[i];
		
		String errorStr = "";
		for(int i=0 ; i<allTypes.length ; i++)
			errorStr += allTypes[i].toString() + "=" + (str.equalsIgnoreCase(allTypes[i].toString())) + ((i==allTypes.length-1)?"":",");
		throw new UnsupportedOperationException("WTF! " + str + " {" + errorStr + "}");
	}
	
	public boolean mustBeLocalHost() {
		return _mustBeLocalHost;
	}
	
	public static void main(String[] argv) {
		NodeTypes nodeType = getNodeType("NODE_GUI_PUBLISHER");
		System.out.println(nodeType);
	}
	
	NodeTypes(Class<?> xClass, boolean mustBeLocalHost, boolean isSubscriber, boolean isPublisher,
								boolean isClient, byte codedByte, char codedChar) {
		_xClass = xClass;
		_mustBeLocalHost = mustBeLocalHost;
		_isSubscriber = isSubscriber;
		_isPublisher = isPublisher;
		
		_isClient = isClient;
		_codedChar = codedChar;
		_codedByte = codedByte;
	}
	
	public char getCodedChar() {
		return _codedChar;
	}
	
	public byte getCodedByte() {
		return _codedByte;
	}
	
	public boolean needsFiles() {
		if(this == NODE_BROKER)
			return false;
		
		return true;
	}
	
	public static NodeTypes getNodeType(byte codedByte) {
		switch(codedByte) {
		case 0:
			return NODE_BROKER;
			
		case 1:
			return NODE_PUBLISHER;
			
		case 2:
			return NODE_SUBSCRIBER;
			
		default:
			throw new IllegalArgumentException("Requested coded char: " + codedByte + "[" + (byte)codedByte + "]");
		}
	}

	public static NodeTypes getNodeType(char codedChar) {
		switch(codedChar) {
		case 'B':
			return NODE_BROKER;
			
		case 'P':
			return NODE_PUBLISHER;
			
		case 'S':
			return NODE_SUBSCRIBER;
			
		default:
			throw new IllegalArgumentException("Requested coded char: " + codedChar + "[" + (byte)codedChar + "]");
		}
	}
	
	@Deprecated
	public static String[] getNodeTypesString() {
		NodeTypes[] allTypes = values();
		String[] allTypesStr = new String[allTypes.length];
		for(int i=0 ; i<allTypesStr.length ; i++)
			allTypesStr[i] = allTypes[i].toString();
		
		return allTypesStr;
	}
	
	public boolean isClient() {
		return _isClient;
	}
	
	public Class<?> getXClass() {
		return _xClass;
	}
}
