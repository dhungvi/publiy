package org.msrg.publiy.communication.core.sessions;

import java.io.Serializable;

public enum SessionConnectionType implements Serializable {

	S_CON_T_UNKNOWN(
					"UNKNOWN",			/* NAME */
					false,				/* LOCALLY_ACTIVE */
					false,				/* PRIMARY_ACTIVE */
					true,				/* REAL */
					false,				/* IS_CONNECTING */
					false),				/* IN_MQ */
	
	// When the session is in `_unjoined' set of ConnectionManager
	S_CON_T_UNJOINED(
					"UNJOINED", 		/* NAME */                   
					false,      		/* LOCALLY_ACTIVE */         
					false,      		/* PRIMARY_ACTIVE */         
					true,       		/* REAL */                   
					false,      		/* IS_CONNECTING */          
					false),				/* IN_MQ */                  
	
	// When the session is in `_active' set of ConnectionManager
	S_CON_T_ACTIVE(
					"ACTIVE",      		/* NAME */                   
					true,          		/* LOCALLY_ACTIVE */         
					true,          		/* PRIMARY_ACTIVE */         
					true,          		/* REAL */                   
					false,         		/* IS_CONNECTING */          
					true),         		/* IN_MQ */                  
	
	S_CON_T_ACTIVATING(
					"ACTIVATING",    	/* NAME */                   
					false,           	/* LOCALLY_ACTIVE */         
					true,            	/* PRIMARY_ACTIVE */         
					true,            	/* REAL */                   
					true,            	/* IS_CONNECTING */          
					true),           	/* IN_MQ */                  
	
	// When the session is in `_inactive' set of ConnectionManager
	S_CON_T_INACTIVE(
					"INACTIVE",     	/* NAME */                   
					false,          	/* LOCALLY_ACTIVE */         
					false,          	/* PRIMARY_ACTIVE */         
					false,          	/* REAL */                   
					false,          	/* IS_CONNECTING */          
					false),				/* IN_MQ */                   
	
	// When a REPLACING session has not yet connected and is no longer needed since a closer node becomes ACTIVE
	S_CON_T_USELESS(
					"USELESS",       	/* NAME */                   
					false,           	/* LOCALLY_ACTIVE */         
					false,           	/* PRIMARY_ACTIVE */         
					false,           	/* REAL */                   
					false,           	/* IS_CONNECTING */          
					false),	         	/* IN_MQ */                  
	
	// When the session is ST_END but we are still in the process of connecting and registering `all' the farther neighbors.
	S_CON_T_BEINGREPLACED(
					"BEINGREPLACED",  	/* NAME */                   
					false,            	/* LOCALLY_ACTIVE */         
					true,             	/* PRIMARY_ACTIVE */         
					false,            	/* REAL */                   
					false,            	/* IS_CONNECTING */          
					true),	          	/* IN_MQ */                  
	
	// When a session is being used to bypass another session.
	S_CON_T_DELTA_VIOLATED(
					"DELTA_VIOLATED",  	/* NAME */                   
					true,            	/* LOCALLY_ACTIVE */         
					true,             	/* PRIMARY_ACTIVE */         
					true,            	/* REAL */                   
					false,             	/* IS_CONNECTING */          
					true),           	/* IN_MQ */  
					
	// When a session is being used to bypass another session.
	S_CON_T_REPLACING(
					"REPLACING",      	/* NAME */                   
					false,            	/* LOCALLY_ACTIVE */         
					true,             	/* PRIMARY_ACTIVE */         
					false,            	/* REAL */                   
					true,             	/* IS_CONNECTING */          
					false),           	/* IN_MQ */                  
	
	// When a session is being used to bypass another session.
	S_CON_T_REPLACING_BEINGREPLACED(
					"REPLACING_BEGIN_REPLACED", 	/* NAME */                   
					false,              /* LOCALLY_ACTIVE */         
					true,               /* PRIMARY_ACTIVE */         
					false,              /* REAL */                   
					true,               /* IS_CONNECTING */          
					true),              /* IN_MQ */                  

	// Dropped session (must have type `ST_END'.
	S_CON_T_DROPPED(
					"DROPPED",       	/* NAME */                   
					false,           	/* LOCALLY_ACTIVE */         
					false,           	/* PRIMARY_ACTIVE */         
					false,           	/* REAL */                   
					false,           	/* IS_CONNECTING */          
					false),          	/* IN_MQ */                  
					
	S_CON_T_FAILED(
					"FAILED",        	/* NAME */                   
					false,           	/* LOCALLY_ACTIVE */         
					false,           	/* PRIMARY_ACTIVE */         
					false,           	/* REAL */                   
					false,           	/* IS_CONNECTING */          
					false),          	/* IN_MQ */                  
	
	// When the session is in `_active' set of ConnectionManager
	S_CON_T_CANDIDATE(
					"CANDIDATE",         /* NAME */                 
					true,                /* LOCALLY_ACTIVE */       
					false,               /* PRIMARY_ACTIVE */       
					false,               /* REAL */                 
					false,               /* IS_CONNECTING */        
					true),               /* IN_MQ */                
					
	S_CON_T_SOFT(
					"SOFT",               /* NAME */                 
					true,                 /* LOCALLY_ACTIVE */       
					false,                /* PRIMARY_ACTIVE */       
					true,                 /* REAL */                 
					false,                /* IS_CONNECTING */        
					true),                /* IN_MQ */                
					
	S_CON_T_SOFTING(
					"SOFTING",            /* NAME */                 
					false,                /* LOCALLY_ACTIVE */       
					false,                /* PRIMARY_ACTIVE */       
					false,                /* REAL */                 
					true,                 /* IS_CONNECTING */        
					true),                /* IN_MQ */                

	S_CON_T_BYPASSING_AND_INACTIVE(
					"BYP-INACTIVE",          /* NAME */                 
					false,                 /* LOCALLY_ACTIVE */       
					false,                /* PRIMARY_ACTIVE */       
					true,                 /* REAL */                 
					false,                /* IS_CONNECTING */        
					false),                /* IN_MQ */                
							
	S_CON_T_BYPASSING_AND_ACTIVE(
					"BYP-ACTIVE",          /* NAME */                 
					true,                 /* LOCALLY_ACTIVE */       
					false,                /* PRIMARY_ACTIVE */       
					true,                 /* REAL */                 
					false,                /* IS_CONNECTING */        
					true),                /* IN_MQ */                
				
	S_CON_T_BYPASSING_AND_INACTIVATING(
					"BYP-INACTIVATING",       /* NAME */                 
					false,                /* LOCALLY_ACTIVE */       
					false,                /* PRIMARY_ACTIVE */       
					false,                /* REAL */                 
					true,                 /* IS_CONNECTING */        
					true),                /* IN_MQ */                

	;
	
	public final String _shortName;
	public final boolean _isLocallyActive;
	public final boolean _isPrimaryActive;
	public final boolean _isReal;
	public final boolean _isConnecting;
	public final boolean _isInMQ;
	
	SessionConnectionType(String shortName, boolean locallyActive, boolean isPrimaryActive, boolean isReal, boolean isConnecting, boolean isInMQ){
		_shortName = shortName;
		_isLocallyActive = locallyActive;
		_isPrimaryActive = isPrimaryActive;
		_isReal = isReal;
		_isConnecting = isConnecting;
		_isInMQ = isInMQ;
	}
	
	public String toStringShort(){
		return _shortName;
	}
}
