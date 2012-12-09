package org.msrg.publiy.broker.core.contentManager.activities;

import org.msrg.publiy.broker.core.contentManager.IContentListener;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimerReading;

public abstract class ContentManagerActivity {

	protected final BrokerInternalTimerReading _creationTime = BrokerInternalTimer.read();
	public final ContentManagerActivityTypes _codingActivityType;
	public final IContentListener _contentListener;
	
	ContentManagerActivity(ContentManagerActivityTypes codingActivityType, IContentListener contentListener) {
		_codingActivityType = codingActivityType;
		_contentListener = contentListener;
	}
	
	@Override
	public String toString() {
		return "Activity[" + _creationTime + "," + _codingActivityType + "," + _contentListener + "]";
	}
}
