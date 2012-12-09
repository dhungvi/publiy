package org.msrg.publiy.broker.core.sequence;

public class SequenceCounter extends Sequence {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -6809354920583880661L;
	
	private int _counter = 0;

	public SequenceCounter(Sequence sequence) {
		this(sequence, 0);
	}

	protected SequenceCounter(Sequence sequence, int initCounter) {
		super(sequence._address, sequence._epoch, sequence._order);
		
		_counter = initCounter; 
	}

	public boolean increaseAndCompareLess(int upperBoundExclusive, boolean increase) {
		return increaseAndCompareLess(1, upperBoundExclusive, increase);
	}
	
	public boolean increaseAndCompareLess(int val, int upperBoundExclusive, boolean increase) {
		if(increase)
			_counter+=val;
		
		if(upperBoundExclusive < 0)
			return true;
		
		return (upperBoundExclusive > _counter);
	}

}
