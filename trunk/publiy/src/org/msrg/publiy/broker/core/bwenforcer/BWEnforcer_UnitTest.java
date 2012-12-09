package org.msrg.publiy.broker.core.bwenforcer;

import org.msrg.publiy.utils.SystemTime;
import junit.framework.TestCase;

public class BWEnforcer_UnitTest extends TestCase {

	protected BWEnforcer _bwEnforcer;
	protected int _availableBW = 1000;
	protected int _msgSize = 300;
	
	public void setUp() {
		_bwEnforcer = new BWEnforcer(_availableBW);
	}
	
	public void testBWEnforcer() {
		for (int i=0 ; i<20 ; i++) {
			SystemTime.setSystemTime(i*200);
			_bwEnforcer.addToUsedBW(_msgSize);
			System.out.println("@T=" + SystemTime.currentTimeMillis() + " [" +
					_bwEnforcer.getUsedBW() + " of " + _bwEnforcer.getTotalAvailableBW() + "=>" + _bwEnforcer.hasRemainingBW() +
					"]");
		}
	}
}
