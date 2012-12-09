package org.msrg.publiy.broker.controller.sequence;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.sequence.Sequence;

public class SequenceUpdateable<T extends ISequenceUpdateListener> extends Sequence {

	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 7566368581639045885L;
	
	private transient SequenceUpdateStatus _updateStatus;
	private transient T _updateListener;
	private transient Object _lock = new Object();
	private transient String _comment;

	protected SequenceUpdateable(InetSocketAddress addr, long epoch, int order) {
		super(addr, epoch, order);
		_updateStatus = SequenceUpdateStatus.SEQ_UPDATE_STATUS_NOT_UPDATED;		
	}

	public void updateSequence(SequenceUpdateStatus status){
		synchronized (_lock){
			_updateStatus = status;
			if ( _updateListener == null )
				return;
			
			_updateListener.sequenceUpdated(this);
		}
	}
	
	public synchronized void attachComment(String comment) {
		if(_comment == null)
			_comment = comment;
		else
			_comment += (" >> " + comment);
	}
	
	public String getComment() {
		return (_comment == null) ? "NONE" : _comment;
	}
	
	public SequenceUpdateStatus getSequenceUpdateStatus(){
		return _updateStatus;
	}
	
	public boolean registerUpdateListener(T seqUpdateListener){
		synchronized(_lock){
			if ( _updateListener != null )
				return false;
			
			_updateListener = seqUpdateListener;
			_updateListener.sequenceUpdated(this);
			
			return true;
		}
	}
}
