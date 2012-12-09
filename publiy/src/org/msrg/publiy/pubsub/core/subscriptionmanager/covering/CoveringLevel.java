package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

class CoveringLevel {
	final int _level;
	final int _size;
	final double _densityThatCovers;
	final double _densityThatSkips;
	
	CoveringLevel(int level, int size, double densityThatCovers, double densityThatSkips) {
		_densityThatCovers = densityThatCovers;
		_densityThatSkips = densityThatSkips;
		_level = level;
		_size = size;
	}
	
	@Override
	public String toString() {
		return "L(" + _level + "), S(" + _size + "), D(" + _densityThatCovers + "), SK(" + _densityThatSkips + ")";
	}
}
