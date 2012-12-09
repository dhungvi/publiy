package org.msrg.publiy.broker.controller.message;

import java.io.Serializable;

public enum ControlMessageTypes implements Serializable {
	
	CTRL_MESSAGE_TCOMMAND,
	
	CTRL_MESSAGE_KILL,
	CTRL_MESSAGE_RECOVER,
	CTRL_MESSAGE_JOIN,
	
	CTRL_MESSAGE_PAUSE,
	CTRL_MESSAGE_PLAY,
	CTRL_MESSAGE_SPEED,
	
	CTRL_MESSAGE_LOAD_PREPARED_SUBS,
	
	CTRL_MESSAGE_BYE,
	
	// BFT-related commands
	CTRL_MESSAGE_BFT_MSG_MANIPULATE,
}
