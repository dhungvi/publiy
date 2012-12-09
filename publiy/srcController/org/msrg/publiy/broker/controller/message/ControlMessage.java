package org.msrg.publiy.broker.controller.message;

import java.io.Serializable;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.IBroker;

import org.msrg.publiy.communication.socketbinding.Message;
import org.msrg.publiy.communication.socketbinding.MessageTypes;
import org.msrg.publiy.node.NodeInstantiationData;



public class ControlMessage<T extends IBroker> extends Message {
	
	/**
	 * Auto generated.
	 */
	private static final long serialVersionUID = -8218702851080212329L;
	private final ControlMessageTypes _controlType;
	private final NodeInstantiationData<T> _nodeInstantiationData;
	private final Serializable _attachment;
	private final String _arguments;
	
//	private ControlMessage(InetSocketAddress to, ControlMessageTypes controlType, NodeInstantiationData<T> nodeInstantiationData, String arguments){
//		this(to, controlType, nodeInstantiationData, null, arguments);
//	}
	
	public ControlMessage(InetSocketAddress to, ControlMessageTypes controlType, NodeInstantiationData<T> nodeInstantiationData, Serializable attachment, String arguments){
		super(MessageTypes.MESSAGE_TYPE_CONTROL, to);
		_controlType = controlType;
		_nodeInstantiationData = nodeInstantiationData;
		_attachment = attachment;
		_arguments = arguments;
	}
	
	public Serializable getAttachment() {
		return _attachment;
	}

	public ControlMessageTypes getControlType(){
		return _controlType;
	}
	
	public NodeInstantiationData<T> getNodeInstantiationData(){
		return _nodeInstantiationData;
	}
	
	public String getArguments(){
		return _arguments;
	}
	
	@Override
	public String toString() {
		return "" + _controlType + "[" + getFrom() + " -> " + getTo() + "]:: " + _nodeInstantiationData + "\t::" + _arguments;
	}
}
