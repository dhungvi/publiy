package org.msrg.publiy.networkcodes.engine;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.raccoon.engine.CodingEngineImpl;
import org.msrg.raccoon.engine.task.CodingTask;
import org.msrg.raccoon.engine.task.result.ByteMatrix_CodingResult;
import org.msrg.raccoon.engine.task.result.Equals_CodingResult;
import org.msrg.raccoon.engine.task.sequential.SequentialCodingTask;
import org.msrg.raccoon.engine.thread.CodingThread;

public class ClientCodingEngineImp extends CodingEngineImpl {
	
	protected IBrokerShadow _brokerShadow;
	
	public ClientCodingEngineImp(IBrokerShadow brokerShadow, int threadCount) {
		super(threadCount);
		
		_brokerShadow = brokerShadow;
	}

	@Override
	public final void codingTaskStarted(CodingThread codingThread, CodingTask codingTask) {
		super.codingTaskStarted(codingThread, codingTask);
		switch(codingTask._taskType) {
		case INVERSE:
			{
				CodingEngineLogger codingEngineLogger = _brokerShadow.getCodingEngineLogger();
				codingEngineLogger.inverseStarted();
			}
			break;
			
		case SEQUENCIAL:
			{
				SequentialCodingTask seqCodingTask = (SequentialCodingTask) codingTask;
				CodingEngineLogger codingEngineLogger = _brokerShadow.getCodingEngineLogger();
				switch(seqCodingTask._seqTaskType) {
				case ENCODE:
					if(codingEngineLogger!=null)
						codingEngineLogger.codingStarted();
					break;
					
				case DECODE:
					if(codingEngineLogger!=null)
						codingEngineLogger.decodingStarted();
					break;
					
				default:
					break;
				}
			}
			break;
			
		default:
			break;
		}
	}
	
	@Override
	public final void codingTaskFinished(CodingThread codingThread, CodingTask codingTask) {
		super.codingTaskFinished(codingThread, codingTask);
		switch(codingTask._taskType) {
		case INVERSE:
			{
				CodingEngineLogger codingEngineLogger = _brokerShadow.getCodingEngineLogger();
				ByteMatrix_CodingResult result =
					(ByteMatrix_CodingResult)codingTask._result;
				boolean success = (result.getResult() != null);
				codingEngineLogger.inverseFinished(success);
			}
			break;
			
		default:
			break;
		}
	}
	
	public void sequentialCodingTaskFinished(SequentialCodingTask seqCodingTask) {
		super.sequentialCodingTaskFinished(seqCodingTask);
		CodingEngineLogger codingEngineLogger = _brokerShadow.getCodingEngineLogger();
		switch(seqCodingTask._seqTaskType) {
		case ENCODE:
			if(codingEngineLogger!=null)
				codingEngineLogger.codingFinished();
			break;
			
		case DECODE:
			boolean success =
				((Equals_CodingResult)seqCodingTask.getCodingResults()).getResult();
			if(codingEngineLogger!=null)
				codingEngineLogger.decodingFinished(success);
			break;
			
		default:
			break;
		}
	}
}
