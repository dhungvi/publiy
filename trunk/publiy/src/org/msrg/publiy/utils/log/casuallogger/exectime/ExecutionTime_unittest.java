package org.msrg.publiy.utils.log.casuallogger.exectime;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.node.NodeTypes;

import junit.framework.TestCase;

public class ExecutionTime_unittest extends TestCase {

	protected final int _delta = 3;
	protected IBrokerShadow _brokerShadow;
	protected MockExecutionTimeLogger _execTimeLogger;
	protected MockExecutionTimeExecutor _execTimeExecutor;
	protected final ExecutionTimeType _execTimeType = ExecutionTimeType.EXEC_TIME_MATCHING;
	
	@Override
	public void setUp() {
		BrokerInternalTimer.start();
		_brokerShadow =
			new BrokerShadow(
					NodeTypes.NODE_BROKER,
					new InetSocketAddress("127.0.0.1", 2000)).setDelta(_delta);
		_execTimeLogger = new MockExecutionTimeLogger((BrokerShadow)_brokerShadow);
		_execTimeExecutor =
			new MockExecutionTimeExecutor(_execTimeType);
		_execTimeLogger.reRegister(_execTimeExecutor);
	}

	@Override
	public void tearDown() { }
	
	public void testExecutor() throws InterruptedException, IOException {
		int[] sleeps = {1000, 1020, 100};
		_execTimeExecutor.createSomeExecTimeEntities(sleeps, true);

		StringWriter writer = new StringWriter();
		_execTimeLogger.runMe(writer);
		
		int[] sleeps2 = {800, 400, 50, 12};
		_execTimeExecutor.createSomeExecTimeEntities(sleeps2, false);
		_execTimeLogger.runMe(writer);
		System.out.println("Log lines:");
		System.out.println(writer);
	}
}

class MockExecutionTimeLogger extends ExecutionTimeLogger {
	
	protected MockExecutionTimeLogger(BrokerShadow brokerShadow) {
		super(brokerShadow);
	}
}

class MockExecutionTimeExecutor extends GenericExecutionTimeExecutor {

	public MockExecutionTimeExecutor(ExecutionTimeType type) {
		super(type);
	}
	
	public void createSomeExecTimeEntities(int[] sleeps, boolean flatten) throws InterruptedException {
		IExecutionTimeEntity entity = new GenericExecutionTimeEntity(this, _types[0]);
		for (int sleep : sleeps) {
			entity.executionStarted();
			Thread.sleep(sleep);
			entity.executionEnded();
		}
		
		entity.finalized(flatten);
	}
	
}