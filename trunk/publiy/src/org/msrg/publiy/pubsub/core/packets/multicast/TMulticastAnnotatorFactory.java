package org.msrg.publiy.pubsub.core.packets.multicast;

import org.msrg.publiy.utils.annotations.IAnnotator;
import org.msrg.publiy.utils.annotations.TMulticast_Annotator;

public class TMulticastAnnotatorFactory {
	
	private static TMulticast_Annotator _tmAnnotator;
	
	public static IAnnotator getTMulticast_Annotator(){
		if ( _tmAnnotator == null )
			_tmAnnotator = new TMulticast_Annotator();
		
		return _tmAnnotator;
	}

}
