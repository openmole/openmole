package com.ice.tar;

import java.io.IOException;

/**
 * This exception is used to indicate that there is a problem with a TAR archive
 * header.
 * 
 * This library is under the Apache License Version 2.0
 * 
 * Authors:
 * 
 * @author Jeremy Lucier
 * @author Timothy Gerard Endres (Original Author)
 * 
 */

public class InvalidHeaderException extends IOException {

	private static final long serialVersionUID = 1L;

	public InvalidHeaderException() {
		super();
	}

	public InvalidHeaderException(String msg) {
		super(msg);
	}

}
