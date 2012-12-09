package org.msrg.publiy.pubsub.core.overlaymanager;

public enum OverlayNodeType {
	
	BROKER 			(false, 		(byte)0,		'B'),
	PUBLISHER		(true,			(byte)1,		'P'),
	SUBSCRIBER		(true,			(byte)2,		'S');

	private final boolean _isClient;
	private final char _codedChar;
	private final byte _codedByte;
	
	OverlayNodeType(boolean isClient, byte codedByte, char codedChar){
		_isClient = isClient;
		_codedChar = codedChar;
		_codedByte = codedByte;
	}
	
	public char getCodedChar(){
		return _codedChar;
	}
	
	public byte getCodedByte(){
		return _codedByte;
	}
	
	public static OverlayNodeType getNodeType(byte codedChar){
		switch(codedChar){
		case 0:
			return BROKER;
			
		case 1:
			return PUBLISHER;
			
		case 2:
			return SUBSCRIBER;
			
		default:
			throw new IllegalArgumentException("Requested coded char: " + codedChar + "[" + (byte)codedChar + "]");
		}
	}

	public static OverlayNodeType getNodeType(char codedChar){
		switch(codedChar){
		case 'B':
			return BROKER;
			
		case 'P':
			return PUBLISHER;
			
		case 'S':
			return SUBSCRIBER;
			
		default:
			throw new IllegalArgumentException("Requested coded char: " + codedChar + "[" + (byte)codedChar + "]");
		}
	}
	
	public boolean isClinet(){
		return _isClient;
	}
}
