package org.msrg.publiy.utils.log.casuallogger.exectime;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.AbstractCasualLogger;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class ExecutionTimeLogger extends AbstractCasualLogger implements IExecutionTimeExecutorInternal  {
	
	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	public boolean _compact = false; 
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;

	protected IBrokerShadow _brokerShadow;
	protected String _execTimeLogFilename;

	protected final Map<ExecutionTimeType, IExecutionTimeExecutorInternal> _executionTimeExecutors;
	protected static final ExecutionTimeType[] _supportedExecutionTypes =
			{
				ExecutionTimeType.EXEC_TIME_CHECKOUT_PUB,
				ExecutionTimeType.EXEC_TIME_MATCHING,
				ExecutionTimeType.EXEC_TIME_PATH_COMPUTATION,
				ExecutionTimeType.EXEC_TIME_MQ_MATCHING,
				ExecutionTimeType.EXEC_TIME_WORKING_OVERLAY_MAN_CREATE,
				ExecutionTimeType.EXEC_TIME_WORKING_SUBSCRIPTION_MAN_CREATE,
				ExecutionTimeType.EXEC_TIME_CONSIDERATION,
				ExecutionTimeType.EXEC_TIME_PROCEED,
				ExecutionTimeType.EXEC_TIME_COVERING_COMPUTATION_TIME,
			};

	public ExecutionTimeLogger(BrokerShadow brokerShadow) {
		brokerShadow.setExecutionTimeLogger(this);
		_brokerShadow = brokerShadow;
		_execTimeLogFilename = _brokerShadow.getExecTimeLogFilename();
		
		_executionTimeExecutors = new HashMap<ExecutionTimeType, IExecutionTimeExecutorInternal>();
		registerAllSupportedTypes();
	}
	
	public IExecutionTimeExecutorInternal getExecutionTimeExecutors(ExecutionTimeType type) {
		return _executionTimeExecutors.get(type);
	}
	
	public void registerAllSupportedTypes() {
		for(ExecutionTimeType supportedType : _supportedExecutionTypes) {
			IExecutionTimeExecutor prevExecutor =
					_executionTimeExecutors.put(supportedType, new GenericExecutionTimeExecutor(supportedType));
			if(prevExecutor != null)
				throw new IllegalStateException(prevExecutor.toString());
		}
	}

	public void reRegister(IExecutionTimeExecutorInternal execTimeExecutor) {
		synchronized (_lock) {
			ExecutionTimeType[] types = execTimeExecutor.getSupportedTypes();
			for(ExecutionTimeType type : types ) {
				IExecutionTimeExecutor prevExecutor = _executionTimeExecutors.put(type, execTimeExecutor);
				if(prevExecutor!= null)
					LoggerFactory.getLogger().warn(this, "Previous executor existed! " + prevExecutor);
			}
			
			try {
				if(!_firstTime)
					writeHeader(_logFileWriter);
			} catch (IOException iox) {
				iox.printStackTrace();
			}
		}
	}

	protected DecimalFormat _decf2 = new DecimalFormat ("0.00");
	protected DecimalFormat _decf3 = new DecimalFormat ("0.000");
	@Override
	protected void runMe() throws IOException {
		runMe(_logFileWriter);
	}
	
	protected void runMe(Writer ioWriter) throws IOException {
		synchronized (_lock) {
			if( _firstTime ) {
				writeHeader(ioWriter);
				_firstTime = false;
			}
		
			long currTime = SystemTime.currentTimeMillis();
			StringBuilder logLine = new StringBuilder();
			
			logLine.append((_compact ? "" : BrokerInternalTimer.read().toString()) + "\t");
			
			for (ExecutionTimeType type : _supportedExecutionTypes) {
				IExecutionTimeExecutorInternal executionTimeExecutorInternal =
						_executionTimeExecutors.get(type);
				if(executionTimeExecutorInternal == null) {
					logLine.append("NOT ACTIVE");
				} else {
					for (ExecutionTimeSummary summary : executionTimeExecutorInternal.getExecutionTimeSummaryForAllTypes(true))
						logLine.append(summary + " ");
				}
				
				logLine.append("\t");
			}
			
			logLine.append("\n");
			ioWriter.write(logLine.toString());
			
			_lastWrite = currTime;
		}
	}
	
	protected void writeHeader(Writer ioWriter) throws IOException {
		if(ioWriter == null)
			return;
		
		String headerLine = (_compact ? "" : ("TIME\t") );
		
		for (ExecutionTimeType type : _supportedExecutionTypes) {
			IExecutionTimeExecutorInternal executionTimeExecutorInternal = _executionTimeExecutors.get(type);
			headerLine +=
					executionTimeExecutorInternal == null ?
							(type + "\t") :
								(executionTimeExecutorInternal.getHeader(type) + "\t");
		}
		
		headerLine += "\n";
		ioWriter.write(headerLine);
	}
	
	@Override
	public boolean isEnabled() {
		return Broker.LOG_EXECTIME && super.isEnabled();
	}

	@Override
	protected String getFileName() {
		return _execTimeLogFilename;
	}

	@Override
	public String toString() {
		return "ExecTimeLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_EXECTIME_LOGGER;
	}
	

	@Override
	public ExecutionTimeSummary getExecutionTimeSummary(ExecutionTimeType type, boolean reset) {
		if(!Broker.LOG_EXECTIME)
			return null;
		
		synchronized (_executionTimeExecutors) {
			return _executionTimeExecutors.get(type).getExecutionTimeSummary(type, reset);
		}
	}

	@Override
	public String getHeader(ExecutionTimeType type) {
		return type.toStringShort();
	}

	@Override
	public void executionFinalized(IExecutionTimeEntity entity) {
		if(!Broker.LOG_EXECTIME)
			return;
		
		synchronized (_executionTimeExecutors) {
			_executionTimeExecutors.get(entity.getType()).executionFinalized(entity);
		}
	}

	@Override
	public ExecutionTimeSummary[] getExecutionTimeSummaryForAllTypes(boolean reset) {
		if(!Broker.LOG_EXECTIME)
			return new ExecutionTimeSummary[0];
		
		ExecutionTimeSummary[] summaries = new ExecutionTimeSummary[_supportedExecutionTypes.length];
		for (int i=0 ; i<_supportedExecutionTypes.length ; i++)
			summaries[i] = getExecutionTimeSummary(_supportedExecutionTypes[i], reset);
		
		return summaries;
	}

	@Override
	public String getHeaderForAllTypes() {
		if(!Broker.LOG_EXECTIME)
			return "";

		String retStr = "";
		for (int i=0 ; i<_supportedExecutionTypes.length ; i++)
			retStr += (getHeader(_supportedExecutionTypes[i]) + (i<_supportedExecutionTypes.length-1?" ":""));
		return retStr;
	}

	@Override
	public ExecutionTimeType[] getSupportedTypes() {
		return _supportedExecutionTypes;
	}
}
