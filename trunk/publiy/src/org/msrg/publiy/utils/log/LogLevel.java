package org.msrg.publiy.utils.log;

import java.io.Serializable;

public enum LogLevel implements Serializable {

	LOG_INFO					("INFO",	1),
	LOG_EXCEPTION				("EXCPN", 	2),
	LOG_ERROR					("ERROR",	3),
	LOG_INFOX					("INFOX",	4),
	LOG_DEBUG					("DEBUG",	5),
	LOG_SPECIAL					("SPECL",	6),
	LOG_WARN					("WARN",	7),
	;
	
	private String _name;
	private final int _index;
	
	private LogLevel(String name, int index){
		_name = name;
		_index = index;
	}
	
	public String toString(){
		return _name;
	}
	
	final int getIndex(){
		return _index;
	}
}
