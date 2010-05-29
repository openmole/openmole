/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.file;

import java.io.IOException;
import org.openmole.misc.backgroundexecutor.ITransferable;

import org.openmole.core.model.file.IURIFile;


public class URIFileCopy implements ITransferable {

	IURIFile source;
	IURIFile destination;


	public URIFileCopy(IURIFile source) {
		super();
		this.source = source;
		this.destination = null;
	}

	public URIFileCopy(IURIFile source, IURIFile destination) {
		super();
		this.source = source;
		this.destination = destination;
	}

	//@Override
	public IURIFile getSource() {
		return source;
	}

	//@Override
	public IURIFile getDestination() {
		return destination;
	}

	//@Override
	public void copy() throws IOException, InterruptedException {
		getSource().copy(getDestination());
	}



	@Override
	public void transfert() throws Exception {
		copy();
	}

	/*@Override
	public TransferableStatus getStatus() {
		return TransferableStatus.UNKNOWN;
	}*/






}
