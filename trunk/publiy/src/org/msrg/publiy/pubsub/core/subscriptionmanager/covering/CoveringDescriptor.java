package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.io.StringWriter;
import java.util.Vector;


public class CoveringDescriptor {
	final Vector<CoveringLevel> _coveringLevels;
	private int _subsCount = -1;
	private int _subsUnmultipliedCount = -1;
	
	CoveringDescriptor() {
		_coveringLevels = new Vector<CoveringLevel>();
	}
	
	void addCoveringLevel(CoveringLevel cLevel) {
		_coveringLevels.add(cLevel._level, cLevel);
	}
	
	public String toString() {
		StringWriter writer = new StringWriter(100);
		for(CoveringLevel cl : _coveringLevels) {
			writer.append(cl.toString() + "\n");
		}
		
		return writer.toString();
	}
	
	public void setCount(int count) {
		_subsCount = count;
	}
	
	public int getCount() {
		return _subsCount;
	}

	public void setUnmultipliedCount(int count) {
		_subsUnmultipliedCount = count;
	}

	public int getUnmultipliedCount() {
		return _subsUnmultipliedCount;
	}
}
