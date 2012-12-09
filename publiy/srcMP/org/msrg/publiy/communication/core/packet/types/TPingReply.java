package org.msrg.publiy.communication.core.packet.types;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.utils.ListElement;
import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

public class TPingReply extends TPing implements ListElement {

	/////////////////////////////////////////////////////////
	//						LISTELEMENT					   //
	/////////////////////////////////////////////////////////
	private final long _elementTime;
	private final byte _cpu;
	private final int _inputPubRate;
	private final int _outputPubRate;
	private final long _arrivedTime;
	
	public TPingReply(TPing ping, double cpu, double inputPubRate, double outputPubRate, InetSocketAddress pingSender) {
		this(ping._time, cpu, inputPubRate, outputPubRate, pingSender);
	}
	
	public TPingReply(long pingTime, double cpu, double inputPubRate, double outputPubRate, InetSocketAddress pingSender) {
		super(pingSender, "TPingReply", pingTime);
		_cpu = (byte)(cpu * 100);
		_inputPubRate = (int) inputPubRate;
		_outputPubRate = (int) outputPubRate;
		_arrivedTime = -1;
		_elementTime = SystemTime.currentTimeMillis();
	}

	private TPingReply(InetSocketAddress inetSAddr, byte cpu, double inputPubRate, double outputPubRate, String str, long time){
		super(inetSAddr, str, time);
		_cpu = cpu;
		_inputPubRate = (int) inputPubRate;
		_outputPubRate = (int) outputPubRate;
		_arrivedTime = SystemTime.currentTimeMillis();
		_elementTime = SystemTime.currentTimeMillis();
	}
	
	@Override
	public PacketableTypes getObjectType() {
		return PacketableTypes.TPINGREPLY;
	}
	
	@Override
	public void putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer buffer){
		super.putObjectInBuffer(localSequencer, buffer);
		int cpuI = super.getContentSize();
		buffer.put(cpuI, _cpu);
		int inputRateI = cpuI + 1;
		buffer.putInt(inputRateI, _inputPubRate);
		int outputRateI = inputRateI + 4;
		buffer.putInt(outputRateI, _outputPubRate);
	}
	
	public static IPacketable getObjectFromBuffer(ByteBuffer bdy) {
		TPing ping = (TPing) TPing.getObjectFromBuffer(bdy);
		int cpuI = ping.getContentSize();
		byte cpu = bdy.get(cpuI);
		int inputRateI = cpuI + 1;
		int inputPubRate = bdy.getInt(inputRateI);
		int outputRateI = inputRateI + 4;
		int outputPubRate = bdy.getInt(outputRateI);

		return new TPingReply(ping.getInetSocketAddress(), cpu, inputPubRate, outputPubRate, ping.getString(), ping.getTime());
	}
	
	public int getContentSize(){
		return super.getContentSize() + 1 + 4 + 4;
	}
	
	public double getCPULoad(){
		return ((double)_cpu / 100);
	}
	
	public double getInputPublicationRate() {
		return _inputPubRate;
	}
	
	public double getOutputPublicationRate() {
		return _outputPubRate;
	}
	
	public long getRTT(){
		if (_arrivedTime < 0)
			return -1;
		else
			return _arrivedTime - _time;
	}
	
	@Override
	public double getValue(int i) {
		switch(i){
		case 0:
			return getRTT();
			
		case 1:
			return getCPULoad();
			
		case 2:
			return getInputPublicationRate();
			
		case 3:
			return getOutputPublicationRate();
			
		default:
			throw new UnsupportedOperationException("i: " + i);	
		}
	}

	@Override
	public long getElementTime() {
		return _elementTime;
	}

	@Override
	public int getValuesCount() {
		return 4;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TPingReply tPingReply = (TPingReply) obj;
		if(tPingReply._cpu != this._cpu)
			return false;
		
		if(tPingReply._arrivedTime != this._arrivedTime)
			return false;
		
		if(tPingReply._elementTime != this._elementTime)
			return false;
		
		if(tPingReply._inputPubRate != this._inputPubRate)
			return false;
		
		if(tPingReply._outputPubRate != this._outputPubRate)
			return false;
		
		return true;
	}

	public static void main(String[] argv) {
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 5000));
		TPing tp = new TPing(Broker.b1Addresses[0]);
		double cpu = 0.4;
		double inputRate = 100.5;
		double outputRate = 400.8;
		TPingReply tpr = new TPingReply(tp, cpu, inputRate, outputRate, Broker.b1Addresses[1]);
		
		IRawPacket raw = PacketFactory.wrapObject(localSequencer, tpr);
		IPacketable obj = PacketFactory.unwrapObject(null, raw);
		System.out.println(obj); //OK
		
//		System.out.println(((TPingReply)obj).getValue(0) + ", " + ((TPingReply)obj).getValue(1));
//		System.out.println(((TPingReply)obj).getRTT() + ", " + ((TPingReply)obj).getCPULoad());
		
		if(!tpr.equals(obj))
			throw new IllegalStateException();
		else
			System.out.println("Correct!");
	}
}
