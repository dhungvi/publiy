package org.msrg.publiy.broker.core.sequence;

public class SequenceHolder {
	
	private Sequence _seq;
	
	public SequenceHolder(Sequence seq){
		if (seq == null)
			throw new IllegalArgumentException();
		_seq = seq;
	}
	
	public void updateSequence(Sequence seq){
		_seq = seq;
	}
	
	public Sequence getSequence(){
		return _seq;
	}

}
