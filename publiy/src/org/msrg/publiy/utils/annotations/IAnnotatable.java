package org.msrg.publiy.utils.annotations;

import org.msrg.publiy.broker.core.sequence.Sequence;

public interface IAnnotatable {
	
	public void annotate(String annotation);
	public String getAnnotations();
	Sequence getSourceSequence();

}
