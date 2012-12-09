package org.msrg.publiy.utils;

import junit.framework.TestCase;

public class FileUtilsTest extends TestCase {

	String _dirname = "." + FileUtils.separatorChar + "temp-" + getClass().getCanonicalName();
	String _nesteddirname = _dirname + FileUtils.separatorChar + "nested";
	
	public void testDirectoryCreationAndDelete() {
		assertTrue(FileUtils.createDirectory(_nesteddirname));
		assertTrue(FileUtils.createDirectory(_dirname));
		assertTrue(FileUtils.deleteDirectory(_dirname));
	}
}
