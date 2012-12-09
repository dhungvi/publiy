package org.msrg.publiy.broker.core.contentManager;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.utils.SystemTime;

public class ContentLifecycle implements IContentLifecycle, Serializable {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -8932346891704536024L;
	
	/*
	 * All times are in milliseconds.
	 */
	public final Content _content;
	protected final long _creationTime;
	protected long _decodeStartTime = -1;
	protected long _decodeCompleteTime = -1;
	protected int _decodeAttempts = 0;
	
	protected List<ContentLifecycleEvent> _metadataArrivalTimes =
		new LinkedList<ContentLifecycleEvent>();
	protected List<ContentLifecycleEvent> _metadataSendTimes =
		new LinkedList<ContentLifecycleEvent>();
	protected List<ContentLifecycleEvent> _piecesArrivalTimes =
		new LinkedList<ContentLifecycleEvent>();
	protected List<ContentLifecycleEvent> _piecesSentTimes =
		new LinkedList<ContentLifecycleEvent>();

	protected long _msLastTimeToRequestMetadata = -1;
	protected final Object _LOCK = new Object();
	
	ContentLifecycle(Content content) {
		_content = content;
		_creationTime = SystemTime.currentTimeMillis();
	}
	
	@Override
	public long getCreationTime() {
		return _creationTime;
	}
	
	@Override
	public List<ContentLifecycleEvent> getPiecesArrivalTimes() {
		return _piecesArrivalTimes;
	}
	
	@Override
	public List<ContentLifecycleEvent> getMetadataArrivedTimes() {
		return _metadataArrivalTimes;
	}
	
	@Override
	public List<ContentLifecycleEvent> getPiecesSentTimes() {
		return _piecesSentTimes;
	}

	@Override
	public List<ContentLifecycleEvent> getMetadataSentTimes() {
		return _metadataSendTimes;
	}
	
	@Override
	public long getDecodeStartTime() {
		return _decodeStartTime;
	}

	@Override
	public long getDecodeCompleteTime() {
		return _decodeCompleteTime;
	}
	
	@Override
	public int getDecodeAttempts() {
		return _decodeAttempts;
	}

	protected boolean setDecodeCompleteTime() {
		synchronized(_LOCK) {
			if(_decodeCompleteTime != -1)
				return false;

			_decodeCompleteTime = SystemTime.currentTimeMillis();
			return true;
		}
	}
	
	protected void metadataSent(InetSocketAddress remote) {
		synchronized(_LOCK) {
			_metadataSendTimes.add(new ContentLifecycleEvent(remote));
		}
	}
	
	protected void metadataArrived(InetSocketAddress senderRemote) {
		synchronized(_LOCK) {
			_metadataArrivalTimes.add(new ContentLifecycleEvent(senderRemote));
		}
	}
	
	protected void setDecodeStartTime() {
		synchronized(_LOCK) {
			_decodeAttempts++;
			_decodeStartTime = SystemTime.currentTimeMillis();
		}
	}

	protected void newPieceArrived(InetSocketAddress senderRemote) {
		synchronized(_LOCK) {
			_piecesArrivalTimes.add(new ContentLifecycleEvent(senderRemote));
		}
	}
	
	protected void newPieceSent(InetSocketAddress senderRemote) {
		synchronized(_LOCK) {
			_piecesSentTimes.add(new ContentLifecycleEvent(senderRemote));
		}
	}
	
	protected boolean requestMetaData(int msLntervalBetweenRequests) {
		synchronized(_LOCK) {
			if(_msLastTimeToRequestMetadata < -1 ||
					_msLastTimeToRequestMetadata + msLntervalBetweenRequests <= SystemTime.currentTimeMillis()) {
				_msLastTimeToRequestMetadata = SystemTime.currentTimeMillis(); 
				return true;
			}
			
			return false;
		}
	}
}
