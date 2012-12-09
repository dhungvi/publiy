package org.msrg.publiy.broker.core.bwenforcer;

public interface IBWEnforcer {
	
	public double getNormalizedAvailableBWPercentile();
	public int getTotalAvailableBW();
	public int getRemainingBW();
	public int getUsedBW();
	
	public boolean hasRemainingBW();
	
	public void addToUsedBW(int usedBW);
	public void setTotalAvailableBW(int totalAvailableBW);
	
	public boolean passedUsedThreshold(double usedThreshold);

}
