package org.openmole.misc.workspace;

import org.openmole.commons.exception.InternalProcessingError;

public interface IPasswordProvider {
	String getPassword() throws InternalProcessingError;
}
