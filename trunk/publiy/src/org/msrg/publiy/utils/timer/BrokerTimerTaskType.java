package org.msrg.publiy.utils.timer;

public enum BrokerTimerTaskType {

	BTimerTask_PurgeMQ						(false, false),
	BTimerTask_Send								(false, false),
	BTimerTask_SendTPing					(false, false),
	BTimerTask_SendDack					(false, false),
	BTimerTask_CasualLogger				(false, false),
	BTimerTask_LoadWeight					(false, false),
	BTimerTask_ForcedConfAck			(false, false),
	BTimerTask_CPUProfiler					(false, false),
	BTimerTask_ConnectionClose			(true, true),
	BTimerTask_Candidate					(false, true),
	BTimerTask_Publish							(false, false),
	BTimerTask_Subscribe					(false, false),
	BTimerTask_GC									(false, false),
	BTimerTask_DestroyConnection	(true, true),
	BTimerTask_RenewSesssion			(true, true),
	BTimerTask_FDReceive					(false, true),
	BTimerTask_FDSend						(false, false),
	
	BTimerTask_BFT_DACK_Send			(false, false),
	BTimerTask_BFT_DACK_Receive		(false, false),
	
	;
	
	final boolean _doLogCreation;
	final boolean _doLogRun;
	
	BrokerTimerTaskType(boolean doLogCreation, boolean doLogRun) {
		_doLogCreation = doLogCreation;
		_doLogRun = doLogRun;
	}
}
