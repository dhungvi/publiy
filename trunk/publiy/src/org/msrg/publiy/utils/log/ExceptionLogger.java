package org.msrg.publiy.utils.log;

import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.info.ExceptionInfo;

public class ExceptionLogger {

	private static List<ExceptionInfo> _exceptionInfos = new LinkedList<ExceptionInfo>();

	public static void exceptionHappened(LocalSequencer localSequencer, LoggingSource source, Exception x, String arguments){
		ExceptionInfo exInfo = new ExceptionInfo(localSequencer, source, x, arguments);
		synchronized (_exceptionInfos) {
			_exceptionInfos.add(exInfo);	
		}
	}
	
	public static List<ExceptionInfo> collectExceptoinInfos(){
		synchronized (_exceptionInfos) {
			List<ExceptionInfo> retList = _exceptionInfos;
			_exceptionInfos = new LinkedList<ExceptionInfo>();
			return retList;
		}
	}

}
