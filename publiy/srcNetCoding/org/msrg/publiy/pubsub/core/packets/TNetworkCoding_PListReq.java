package org.msrg.publiy.pubsub.core.packets;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;


import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.publishSubscribe.Publication;

public class TNetworkCoding_PListReq extends TNetworkCoding_CodedPieceIdReq {

	public final Publication _publication;
	public final boolean _fromSource;
	public final int _rows;
	public final int _cols;
	
	public TNetworkCoding_PListReq(
			InetSocketAddress sender,
			Sequence sequence,
			int rows, int cols,
			Publication publication,
			boolean fromSource) {
		super(CodedPieceIdReqTypes.PLIST_REQ, sender, sequence);

		_rows = rows;
		_cols = cols;
		_publication = publication;
		_fromSource = fromSource;
	}

	public TNetworkCoding_PListReq(ByteBuffer bb, int bbOffset) {
		super(bb, bbOffset);
		
		int offset = bbOffset + super.getContentSize();
		_fromSource = (bb.get(offset)==1)?true:false;
		offset++;

		_rows = bb.getInt(offset);
		offset += 4;
		
		_cols = bb.getInt(offset);
		offset += 4;
		
		byte publicationSize = bb.get(offset);
		offset++;
		
		byte[] pubInBytesArray = new byte[publicationSize];
		bb.position(offset);
		bb.get(pubInBytesArray);
		
		_publication = Publication.decode(new String(pubInBytesArray));
	}

	@Override
	public int putObjectInBuffer(LocalSequencer localSequencer, ByteBuffer bb, int bbOffset) {
		int superSize = super.putObjectInBuffer(localSequencer, bb, bbOffset);
		
		int offset = bbOffset + superSize;
		bb.put(offset, _fromSource?(byte)1:(byte)0);
		offset++;
		
		bb.putInt(offset, _rows);
		offset += 4;
		
		bb.putInt(offset, _cols);
		offset += 4;
		
		String pubStr = Publication.encode(_publication);
		int pubSize = pubStr.length();
		if(pubSize > Byte.MAX_VALUE)
			throw new IllegalStateException("Publication length is too large: " + pubSize);
		
		bb.put(offset, (byte)pubSize);
		offset++;
		
		bb.position(offset);
		bb.put(pubStr.getBytes());
		
		return offset - bbOffset;
	}
	
	@Override
	public int getContentSize() {
		return super.getContentSize() + 1 + 1 + 4 + 4 + Publication.encode(_publication).length();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!super.equals(obj))
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		TNetworkCoding_PListReq pListReqObj = (TNetworkCoding_PListReq) obj;
		if(!_publication.equals(pListReqObj._publication))
			return false;
		if(_fromSource != pListReqObj._fromSource)
			return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		return super.toString() + "(" + _fromSource + "): " + _publication;
	}
}
