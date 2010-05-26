package org.openmole.misc.clonning;

import org.openmole.commons.exception.InternalProcessingError;

public interface IClonningService {
//	public IContext cloneJobContext(IContext context, ITicket ticket, String taskName);
	public Object clone(Object object) throws InternalProcessingError;
}
