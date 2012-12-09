package org.msrg.publiy.utils.annotations;

public enum AnnotationEvent {

	ANNOTATION_EVENT_PUBLICATION_GENERATION ( "Pub_Generated" ),
	ANNOTATION_EVENT_PUBLICATION_LOCALLY_CONFIRMED ( "Pub_Locally_Confirmed" ),
	ANNOTATION_EVENT_PUBLICATION_GLOBALLY_CONFIRMED ( "Pub_Globally_Confirmed" ),
	
	ANNOTATION_EVENT_PUBLICATION_RECEVED ( "Pub_Delivered" ),
	
	ANNOTATION_EVENT_MESSAGE_RECEIVED ( "Message_Rcvd"),
	ANNOTATION_EVENT_MESSAGE_SENT ( "Message_Sent" ),
	ANNOTATION_EVENT_MESSAGE_OUT_QUEUED ( "Message_OQued" ),
	
	ANNOTATION_EVENT_MESSAGE_HANDLING ( "Message_Hndl" ),
	ANNOTATION_EVENT_MQ_ENTER ( "Message_MQENT" ),
	ANNOTATION_EVENT_MQ_EXIT ( "Message_MQEXT" ),
	
	ANNOTATION_EVENT_MQ_PROCESSING_TIME ( "Processing_Time" )
	;
	
	private String _name;
	
	private AnnotationEvent(String name){
		_name = name;
	}
	
	public String toString(){
		return _name;
	}

}
