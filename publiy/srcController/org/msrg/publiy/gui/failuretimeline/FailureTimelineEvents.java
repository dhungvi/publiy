package org.msrg.publiy.gui.failuretimeline;

import java.awt.Color;

import org.msrg.publiy.utils.SystemTime;

final class EventColor {
	static Color DEFAULT_AUTO_COLOR = Color.blue;
	static Color ISSUING_AUTO_COLOR = Color.blue;
	static Color ERROR_AUTO_COLOR = Color.red;
	
	static Color DEFAULT_COLOR = Color.black;
	static Color ISSUING_COLOR = Color.yellow;
	static Color ERROR_COLOR = Color.pink;
	
	static Color DEFAULT_HIGHLIGHTED_COLOR = Color.RED;
}

class PrivateCounter {
	static int COUNTER=-1;
}

public enum FailureTimelineEvents {
	/*
	 * Add additional Timeline event 'triples' below at the mark (***).
	 * Otherwise, you need to change "makeIssuing()", "makeReceived()" and "makeError()"
	 * since the indeces get scewed up.
	 */
	
	FT_PAUSE_TIMELINE('ᐦ', EventColor.DEFAULT_COLOR, null, "s", 				PrivateCounter.COUNTER++),
	FT_NO_EVENT(' ', EventColor.DEFAULT_COLOR, null, "\n", 					PrivateCounter.COUNTER++),

	/*
	 * (***): Add additional Timeline event 'triples' HERE!
	 */

	FT_BFT_MSG_MANIULATE ('X', EventColor.DEFAULT_HIGHLIGHTED_COLOR, "BFTMsgManip", "X",			PrivateCounter.COUNTER++),
	FT_BFT_MSG_MANIULATE_ISSUING ('X', EventColor.DEFAULT_HIGHLIGHTED_COLOR, "BFTMsgManip", "X",			PrivateCounter.COUNTER++),
	FT_BFT_MSG_MANIULATE_ERROR ('X', EventColor.DEFAULT_HIGHLIGHTED_COLOR, "BFTMsgManip", "X",			PrivateCounter.COUNTER++),

	FT_AUTO_KILL ('▼', EventColor.DEFAULT_AUTO_COLOR, "AKill", "K",			PrivateCounter.COUNTER++),
	FT_AUTO_KILL_ISSUING ('▼', EventColor.ISSUING_AUTO_COLOR, "AKill", "K",	PrivateCounter.COUNTER++),
	FT_AUTO_KILL_ERROR ('▼', EventColor.ERROR_AUTO_COLOR, "AKill", "K",		PrivateCounter.COUNTER++),
	
	FT_AUTO_RECOVER ('▲', EventColor.DEFAULT_AUTO_COLOR, "ARecover", "R",	PrivateCounter.COUNTER++),
	FT_AUTO_RECOVER_ISSUING ('▲', EventColor.ISSUING_AUTO_COLOR, "ARecover", "R",	PrivateCounter.COUNTER++),
	FT_AUTO_RECOVER_ERROR ('▲', EventColor.ERROR_AUTO_COLOR, "ARecover", "R",PrivateCounter.COUNTER++),

	FT_AUTO_PAUSE ( '◄', EventColor.DEFAULT_AUTO_COLOR, "APause", "Z",		PrivateCounter.COUNTER++),
	FT_AUTO_PAUSE_ISSUING ( '◄', EventColor.ISSUING_AUTO_COLOR, "APause", "Z",	PrivateCounter.COUNTER++),
	FT_AUTO_PAUSE_ERROR ( '◄', EventColor.ERROR_AUTO_COLOR, "PAause", "Z",	PrivateCounter.COUNTER++),
	
	FT_JOIN ('♦', EventColor.DEFAULT_COLOR, "Join", "j", 					PrivateCounter.COUNTER++),
	FT_JOIN_ISSUING ('♦', EventColor.ISSUING_COLOR, "Join", "j",			PrivateCounter.COUNTER++),
	FT_JOIN_ERROR ('♦', EventColor.ERROR_COLOR, "Join", "j",				PrivateCounter.COUNTER++),
	
	FT_DEPART ('◊', EventColor.DEFAULT_COLOR, "Depart", "d",				PrivateCounter.COUNTER++),
	FT_DEPART_ISSUING ('◊', EventColor.ISSUING_COLOR, "Depart", "d",		PrivateCounter.COUNTER++),
	FT_DEPART_ERROR ('◊', EventColor.ERROR_COLOR, "Depart", "d",			PrivateCounter.COUNTER++),
	
	FT_KILL ('▼', EventColor.DEFAULT_COLOR, "Kill", "k",					PrivateCounter.COUNTER++),
	FT_KILL_ISSUING ('▼', EventColor.ISSUING_COLOR, "Kill", "k",			PrivateCounter.COUNTER++),
	FT_KILL_ERROR ('▼', EventColor.ERROR_COLOR, "Kill", "k",				PrivateCounter.COUNTER++),
	
	FT_RECOVER ('▲', EventColor.DEFAULT_COLOR, "Recover", "r",				PrivateCounter.COUNTER++),
	FT_RECOVER_ISSUING ('▲', EventColor.ISSUING_COLOR, "Recover", "r",		PrivateCounter.COUNTER++),
	FT_RECOVER_ERROR ('▲', EventColor.ERROR_COLOR, "Recover", "r",			PrivateCounter.COUNTER++),

	FT_LOAD_PREPARED_SUBS ( 'L', EventColor.DEFAULT_COLOR, "LoadSubs", "l",	PrivateCounter.COUNTER++),
	FT_LOAD_PREPARED_SUBS_ISSUING ( 'L', EventColor.ISSUING_COLOR, "LoadSubs", "l",	PrivateCounter.COUNTER++),
	FT_LOAD_PREPARED_SUBS_ERROR ( 'L', EventColor.ERROR_COLOR, "LoadSubs", "l", PrivateCounter.COUNTER++),
	
	FT_MARK ( '§', EventColor.DEFAULT_COLOR, "Mark", "m",					PrivateCounter.COUNTER++),
	FT_MARK_ISSUING ( '§', EventColor.ISSUING_COLOR, "Mark", "m",			PrivateCounter.COUNTER++),
	FT_MARK_ERROR ( '§', EventColor.ERROR_COLOR, "Mark", "m",				PrivateCounter.COUNTER++),
	
	FT_BYE ( '☠', EventColor.DEFAULT_COLOR, "Bye", "b",						PrivateCounter.COUNTER++), //■
	FT_BYE_ISSUING ( '☠', EventColor.ISSUING_COLOR, "Bye", "b",				PrivateCounter.COUNTER++),
	FT_BYE_ERROR ( '☠', EventColor.ERROR_COLOR, "Bye", "b",					PrivateCounter.COUNTER++),
	
	FT_PAUSE ( '◄', EventColor.DEFAULT_COLOR, "Pause", "z",					PrivateCounter.COUNTER++),
	FT_PAUSE_ISSUING ( '◄', EventColor.ISSUING_COLOR, "Pause", "z",			PrivateCounter.COUNTER++),
	FT_PAUSE_ERROR ( '◄', EventColor.ERROR_COLOR, "Pause", "z",				PrivateCounter.COUNTER++),
	
	FT_PLAY_PUBLISHER ( '►', EventColor.DEFAULT_COLOR, "PlayPub", "p",			PrivateCounter.COUNTER++),
	FT_PLAY_PUBLISHER_ISSUING ( '►', EventColor.ISSUING_COLOR, "PlayPub", "p",	PrivateCounter.COUNTER++),
	FT_PLAY_PUBLISHER_ERROR ( '►', EventColor.ERROR_COLOR, "PlayPub", "p",		PrivateCounter.COUNTER++),
	
	FT_PLAY_SUBSCRIBER ( '▷', EventColor.DEFAULT_COLOR, "PlaySub", "P",			PrivateCounter.COUNTER++),
	FT_PLAY_SUBSCRIBER_ISSUING ( '▷', EventColor.ISSUING_COLOR, "PlaySub", "P",	PrivateCounter.COUNTER++),
	FT_PLAY_SUBSCRIBER_ERROR ( '▷', EventColor.ERROR_COLOR, "PlaySub", "P",		PrivateCounter.COUNTER++),
	
	FT_SPEED1 ( '1', EventColor.DEFAULT_COLOR, "Speed1", "1",				PrivateCounter.COUNTER++),
	FT_SPEED1_ISSUING ( '1', EventColor.ISSUING_COLOR, "Speed1", "1",		PrivateCounter.COUNTER++),
	FT_SPEED1_ERROR ( '1', EventColor.ERROR_COLOR, "Speed1", "1",			PrivateCounter.COUNTER++),
	
	FT_SPEED2 ( '2', EventColor.DEFAULT_COLOR, "Speed2", "2",				PrivateCounter.COUNTER++),
	FT_SPEED2_ISSUING ( '2', EventColor.ISSUING_COLOR, "Speed2", "2",		PrivateCounter.COUNTER++),
	FT_SPEED2_ERROR ( '2', EventColor.ERROR_COLOR, "Speed2", "2",			PrivateCounter.COUNTER++),
	
	FT_SPEED3 ( '3', EventColor.DEFAULT_COLOR, "Speed3", "3",				PrivateCounter.COUNTER++),
	FT_SPEED3_ISSUING ( '3', EventColor.DEFAULT_COLOR, "Speed3", "3",		PrivateCounter.COUNTER++),
	FT_SPEED3_ERROR ( '3', EventColor.DEFAULT_COLOR, "Speed3", "3",			PrivateCounter.COUNTER++),
	
	FT_SPEED4 ( '4', EventColor.DEFAULT_COLOR, "Speed4", "4",				PrivateCounter.COUNTER++),
	FT_SPEED4_ISSUING ( '4', EventColor.ISSUING_COLOR, "Speed4", "4",		PrivateCounter.COUNTER++),
	FT_SPEED4_ERROR ( '4', EventColor.ERROR_COLOR, "Speed4", "4",			PrivateCounter.COUNTER++),
	
	FT_SPEED5 ( '5', EventColor.DEFAULT_COLOR, "Speed5", "5",				PrivateCounter.COUNTER++),
	FT_SPEED5_ISSUING ( '5', EventColor.ISSUING_COLOR, "Speed5", "5",		PrivateCounter.COUNTER++),
	FT_SPEED5_ERROR ( '5', EventColor.ERROR_COLOR, "Speed5", "5",			PrivateCounter.COUNTER++),
	
	FT_SPEED6 ( '6', EventColor.DEFAULT_COLOR, "Speed5", "6",				PrivateCounter.COUNTER++),
	FT_SPEED6_ISSUING ( '6', EventColor.ISSUING_COLOR, "Speed5", "6",		PrivateCounter.COUNTER++),
	FT_SPEED6_ERROR ( '6', EventColor.ERROR_COLOR, "Speed5", "6",			PrivateCounter.COUNTER++),
	
	FT_SPEED7 ( '7', EventColor.DEFAULT_COLOR, "Speed5", "7",				PrivateCounter.COUNTER++),
	FT_SPEED7_ISSUING ( '7', EventColor.ISSUING_COLOR, "Speed5", "7",		PrivateCounter.COUNTER++),
	FT_SPEED7_ERROR ( '7', EventColor.ERROR_COLOR, "Speed5", "7",			PrivateCounter.COUNTER++),
	
	FT_SPEED8 ( '8', EventColor.DEFAULT_COLOR, "Speed5", "8",				PrivateCounter.COUNTER++),
	FT_SPEED8_ISSUING ( '8', EventColor.ISSUING_COLOR, "Speed5", "8",		PrivateCounter.COUNTER++),
	FT_SPEED8_ERROR ( '8', EventColor.ERROR_COLOR, "Speed5", "8",			PrivateCounter.COUNTER++),
	
	FT_SPEED9 ( '9', EventColor.DEFAULT_COLOR, "Speed5", "9",				PrivateCounter.COUNTER++),
	FT_SPEED9_ISSUING ( '9', EventColor.ISSUING_COLOR, "Speed5", "9",		PrivateCounter.COUNTER++),
	FT_SPEED9_ERROR ( '9', EventColor.ERROR_COLOR, "Speed5", "9",			PrivateCounter.COUNTER++),
	
	FT_SPEED0 ( '0', EventColor.DEFAULT_COLOR, "Speed5", "0",				PrivateCounter.COUNTER++),
	FT_SPEED0_ISSUING ( '0', EventColor.ISSUING_COLOR, "Speed5", "0",		PrivateCounter.COUNTER++),
	FT_SPEED0_ERROR ( '0', EventColor.ERROR_COLOR, "Speed5", "0",			PrivateCounter.COUNTER++)
	
	;
	
	public final char _codedValue;
	public final Color _colorCode;
	String _commandname;
	String _keyboardShortcut;
	final int _internalCode;
	
	private static final int BEGIN_INDEX = 1;
	
	private FailureTimelineEvents(char codedValue, Color colorCode, String commandName, String shortcut, int internalCode) {
		_codedValue = codedValue;
		_colorCode = colorCode;
		_keyboardShortcut = shortcut;
		_commandname = commandName;
		_internalCode = internalCode;
	}
	
	public static FailureTimelineEvents[] changeFTEventsLength(FailureTimelineEvents[] oldEvents, int newLength) {
		FailureTimelineEvents[] newEvents = new FailureTimelineEvents[newLength];
		int oldLength = oldEvents.length;
		
		if(newLength > oldLength) {
			for(int i=0 ; i<oldLength ; i++)
				newEvents[i] = oldEvents[i];
			for(int i=oldLength ; i<newLength ; i++)
				newEvents[i] = getFailureTimelineEvent(' ');
		}
		
		else {
			for(int i=0 ; i<newLength ; i++)
				newEvents[i] = oldEvents[i];
		}
		
		return newEvents;
	}
	
	public static FailureTimelineEvents[] getEvents(String str, int length) {

		FailureTimelineEvents[] events = new FailureTimelineEvents[length];
		char[] chars; 
		if(str == null)
			chars = new char[0];
		else
			chars = str.toCharArray();
		
		for(int i=0 ; i<chars.length && i<length ; i++)
			events[i] = getFailureTimelineEvent(chars[i]);
		
		for(int i=chars.length ; i<length ; i++)
			events[i] = getFailureTimelineEvent(' ');
		
		return events;
	}
	
	public String getKeyboardShortcut() {
		return _keyboardShortcut;
	}
	
	public static FailureTimelineEvents getFailureTimelineEvent(char ch) {
		switch( ch) {
		
			case 'j':
			case '♦':
				return FT_JOIN;
			
			case 'd':
			case '◊':
				return FT_DEPART;
			
			case 'l':
			case 'L':
				return FT_LOAD_PREPARED_SUBS;
				
			case 'm':
			case '§':
				return FT_MARK;
				
			case 'b':
			case '☠':
				return FT_BYE;
				
			case ' ':
				return FT_NO_EVENT;
			
			case 'x':
			case 'X':
			case '†':
				return FT_BFT_MSG_MANIULATE;
				
			case 'k':
			case '▼':
				return FT_KILL;
			
			case 'r':
			case '▲':
				return FT_RECOVER;
				
			case 's':
			case 'ᐦ':
				return FT_PAUSE_TIMELINE;
				
			case 'z':
			case '◄':
				return FT_PAUSE;
			
			case 'P':
			case '▷':
				return FT_PLAY_SUBSCRIBER;
				
			case 'p':
			case '►':
				return FT_PLAY_PUBLISHER;
				
			case '1':
				return FT_SPEED1;
			
			case '2':
				return FT_SPEED2;
			
			case '3':
				return FT_SPEED3;
			
			case '4':
				return FT_SPEED4;
				
			case '5':
				return FT_SPEED5;

			case '6':
				return FT_SPEED6;
			
			case '7':
				return FT_SPEED7;
			
			case '8':
				return FT_SPEED8;
			
			case '9':
				return FT_SPEED9;
			
			case '0':
				return FT_SPEED0;
				
			default:
				return FT_NO_EVENT;
		}
	}
	
	public String getCommand() {
		return _commandname;
	}
	
	public char getCodedValue() {
		return _codedValue;
	}
	
	public Color getColor() {
		if(_colorCode == EventColor.DEFAULT_HIGHLIGHTED_COLOR)
			if((SystemTime.currentTimeMillis() / 500) % 2 == 0 )
				return EventColor.DEFAULT_HIGHLIGHTED_COLOR;
			else
				return EventColor.DEFAULT_AUTO_COLOR;
			
		return _colorCode;
	}

	public static String getString(FailureTimelineEvents[] events) {
		if(events == null)
			return null;
		
		String retString = "";
		for(int i=0 ; i<events.length ; i ++)
			retString += events[i]._codedValue;
		
		return retString;
	}
	
	public boolean equalsInternally(Object obj) {
		if(obj == null || !obj.getClass().equals(this.getClass()))
			return false;
		
		FailureTimelineEvents eventObj = (FailureTimelineEvents)obj;
		return ((int) ((this._internalCode+1)/2)) == ((int) ((eventObj._internalCode+1)/2));
	}
	
	FailureTimelineEvents makeIssuing() {
		if(_internalCode <= 0)
			return this;
		
		int newIndex = (int)((_internalCode-1)/3)*3 + 2;
		return FailureTimelineEvents.values()[newIndex + BEGIN_INDEX];
	}
	
	FailureTimelineEvents makeReceived() {
		if(_internalCode <= 0)
			return this;
		
		int newIndex = (int)((_internalCode-1)/3)*3 + 1;
		return FailureTimelineEvents.values()[newIndex + BEGIN_INDEX];
	}
	
	FailureTimelineEvents makeError() {
		if(_internalCode <= 0)
			return this;
		
		int newIndex = (int)((_internalCode-1)/3)*3 + 3;
		return FailureTimelineEvents.values()[newIndex + BEGIN_INDEX];
	}

	public static void main(String [] argv) {
		System.out.println(FailureTimelineEvents.FT_PLAY_PUBLISHER.makeError().makeReceived()); // OK
		
		System.out.println(FailureTimelineEvents.FT_PLAY_PUBLISHER.makeError().makeReceived()); // OK
		
	}

}
