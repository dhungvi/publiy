package org.msrg.publiy.utils;

public interface ListBucketElement extends ListElement {
	
	public void invalidate();
	public boolean isValid(long validityPeriod);
	public void addToBucket(int i, int val);
	public long getCreationTime();
	public int getUpdateCounter(int i);

}
