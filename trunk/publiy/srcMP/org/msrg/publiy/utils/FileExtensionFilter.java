package org.msrg.publiy.utils;

import java.io.File;
import java.io.FilenameFilter;

public class FileExtensionFilter implements FilenameFilter {

	private String _filename; 
	private String _extension; 

	public FileExtensionFilter(String filename, String extension) {
	_filename = filename;
	_extension = extension;
	}

	@Override
	public boolean accept(File directory, String filename) {
		if (_filename != null)
			if (!filename.startsWith(_filename))
				return false;
		
			if (_extension != null)
				if (!filename.endsWith('.' + _extension))
				return false;
			    
		return true;
	}
}