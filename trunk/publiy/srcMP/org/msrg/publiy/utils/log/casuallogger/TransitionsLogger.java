package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.communication.core.sessions.SessionConsiderationTransition;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

public class TransitionsLogger  extends AbstractCasualLogger {
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int PROFILER_CACHE_SIZE = 5;
	
	private final IBrokerShadow _brokerShadow;
	private final String _transitionsFilename;
	private final List<SessionConsiderationTransition> _recentTransitions = new LinkedList<SessionConsiderationTransition>();
	
	public TransitionsLogger(BrokerShadow brokerShadow) {
		brokerShadow.setTransitionsLogger(this);
		_brokerShadow = brokerShadow;
		_transitionsFilename = _brokerShadow.getTransitionsFileName();
	}
	
	@Override
	protected String getFileName() {
		return _transitionsFilename;
	}

	public void logTransitions(Set<SessionConsiderationTransition> transitionsList) {
		synchronized(_recentTransitions) {
			Iterator<SessionConsiderationTransition> transitionsListIt = transitionsList.iterator();
			while ( transitionsListIt.hasNext() )
				_recentTransitions.add(transitionsListIt.next());
		}
	}
	
	public void logTransition(SessionConsiderationTransition transition) {
		synchronized(_recentTransitions) {
			_recentTransitions.add(transition);
		}
	}
	
	@Override
	protected void runMe() throws IOException {
		synchronized(_recentTransitions) {
			for ( Iterator<SessionConsiderationTransition> transitionsIt = _recentTransitions.iterator() ; transitionsIt.hasNext() ; ) {
				SessionConsiderationTransition transition = transitionsIt.next();
				String logLine = transition.toString();
				_logFileWriter.write(logLine + "\n");
			}
			_recentTransitions.clear();
		}
	}

	@Override
	public String toString() {
		return "TransLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}
}
