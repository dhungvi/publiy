package org.msrg.publiy.pubsub.core.subscriptionmanager.covering;

import java.util.Random;

public class CoveringLevelNormalMultiplier extends CoveringLevel {
	
	private static final Random RAND = new Random(CoveringMockSubscriptionGenerator.SEED);
	
	private final int _multiplierAvg;
	private final double _multiplierVariance;
	
	CoveringLevelNormalMultiplier(int level, int size, double densityThatCovers, double coveringSkip, int multiplierAvg, double multiplierVariance) {
		super(level, size, densityThatCovers, coveringSkip);
		
		_multiplierAvg = multiplierAvg;
		_multiplierVariance = multiplierVariance;
	}
	
	@Override
	public String toString() {
		return super.toString() + ", A(" + _multiplierAvg + "), V(" + _multiplierVariance + ")";
	}
	
	public int getMultiplier() {
		return (int) (RAND.nextGaussian() * _multiplierVariance + _multiplierAvg);
	}

	public static CoveringDescriptor makeRandomMultpliedCoveringDescriptor(
			int levels, int totalSize, double coveringDensity, double coveringSkip, int avgMultiplier, double varianceMultiplier) {
		CoveringDescriptor coveringDescriptor = new CoveringDescriptor();
		
		for(int i=0 ; i<levels ; i++ ) {
			int size = (int) (((double)totalSize)/levels/avgMultiplier + 0.5);
			CoveringLevel cLevel = new CoveringLevelNormalMultiplier(i, size, coveringDensity, coveringSkip, avgMultiplier, varianceMultiplier);
			coveringDescriptor.addCoveringLevel(cLevel);
		}
		
		return coveringDescriptor;
	}
	
}
