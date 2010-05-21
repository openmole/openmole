package org.openmole.misc.workspace;

import org.openmole.misc.exception.InternalProcessingError;

public interface IPasswordProvider {
	String getPassword() throws InternalProcessingError;
}
