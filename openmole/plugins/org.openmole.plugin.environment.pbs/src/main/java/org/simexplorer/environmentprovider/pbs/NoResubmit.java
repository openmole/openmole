package org.openmole.environmentprovider.pbs;

import java.util.Date;

import org.openmole.jobupdater.IWorkloadManagmentStrategie;

import eu.geclipse.core.model.IGridJobStatus;

public class NoResubmit implements IWorkloadManagmentStrategie{

	@Override
	public boolean resubmit(IGridJobStatus status, Date submited) {
		return false;
	}

}
