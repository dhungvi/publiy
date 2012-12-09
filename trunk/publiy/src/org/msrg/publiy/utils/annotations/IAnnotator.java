package org.msrg.publiy.utils.annotations;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

public interface IAnnotator {

	public IAnnotatable annotate(LocalSequencer localSequencer, IAnnotatable annotatable, AnnotationEvent annSrc, String msg);
	
	public String getAnnotations(IAnnotatable annotatable);
	public void dumpAnnotations(IAnnotatable annotatable, String filename);
	void forceFlush();
	
}
