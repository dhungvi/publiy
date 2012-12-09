package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TNetworkCoding_PListReply extends TNetworkCoding_CodedPieceIdReq {

	public boolean _launch;
	public final int _rows;
	public final int _cols;
	public final Set<InetSocketAddress> _remotes;
	public final byte _remotesSize;
	
	public TNetworkCoding_PListReply(
			boolean launch,
			InetSocketAddress sender,
			Sequence sequence,
			int rows, int cols,
			Set<InetSocketAddress> localClients,
			Set<InetSocketAddress> launchClients) {
		super(CodedPieceIdReqTypes.PLIST_REPLY, sender, sequence);

		_launch = launch;
		_rows = rows;
		_cols = cols;
		_remotes = new HashSet<InetSocketAddress>();
		
		if(localClients != null)
			_remotes.addAll(localClients);
		
		if(launchClients != null)
			_remotes.addAll(launchClients);
		
		int size = _remotes.size();
		if(size > Byte.MAX_VALUE)
			throw new IllegalArgumentException("Too large: " + size);
		
		_remotesSize = (byte) size;
	}

	public TNetworkCoding_PListReply(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
		
		_remotes = new HashSet<InetSocketAddress>();
		
		int offset = bbOffset + super.getContentSize();
		
		_launch = bb.get(offset)==0 ? false : true;
		offset += 1;
		
		_rows = bb.getInt(offset);
		offset += 4;
		
		_cols = bb.getInt(offset);
		offset += 4;
		
		_remotesSize = bb.get(offset);
		offset += 1;

		for(int i=0 ; i<_remotesSize ; i++) {
			InetSocketAddress remote = Sequence.readAddressFromByteBuffer(bb, offset);
			_remotes.add(remote);
			offset += Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE;
		}
	}
	
	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int offset = bbOffset + super.putObjectInBuffer(localSequencer, bb, bbOffset);
		
		bb.put(offset, _launch ? (byte)1 : (byte)0);
		offset += 1;
		
		bb.putInt(offset, _rows);
		offset += 4;
		
		bb.putInt(offset, _cols);
		offset += 4;
		
		bb.put(offset, _remotesSize);
		offset += 1;
		
		for(InetSocketAddress remote : _remotes) {
			byte[] remoteBytes = Sequence.getAddressInByteArray(remote);
			bb.position(offset);
			bb.put(remoteBytes);
			offset += remoteBytes.length;
		}
		
		return bbOffset - offset;
	}
	
	@Override
	public int getContentSize() {
		return super.getContentSize() +
			1 +
			4 + 4 + 1 +
			Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE +
			Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE * _remotes.size();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TNetworkCoding_PListReply plistObj = (TNetworkCoding_PListReply) obj;
		if(!super.equals(obj))
			return false;
		
		if(_remotesSize != plistObj._remotesSize)
			return false;
		
		if(_launch != plistObj._launch)
			return false;
		
		return _remotes.equals(plistObj._remotes);
	}

	@Override
	public String toString() {
		return super.toString() + (_launch ? "*" : "") + "(" + _remotesSize + "): " + _remotes;
	}
}
