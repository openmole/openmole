package org.openmole.misc.workspace;

import org.openmole.commons.exception.InternalProcessingError;

public class ForbidenPasswordProvider implements IPasswordProvider {

	@Override
	public String getPassword() throws InternalProcessingError {
		throw new InternalProcessingError("This application is not supposed to ask for a password.");
	}

}
