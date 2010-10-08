/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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


package org.openmole.core.replicacatalog.internal;


import org.openmole.core.model.file.IURIFile;

import com.db4o.activation.ActivationPurpose;
import com.db4o.activation.Activator;
import com.db4o.ta.Activatable;
import java.io.File;
import org.openmole.commons.tools.io.IHash;
import org.openmole.core.replicacatalog.IReplica;
import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;
import org.openmole.core.model.execution.batch.IBatchServiceDescription;


public class Replica implements IReplica, Activatable {

    final File source;
    final IHash hash;
    final IBatchServiceDescription storageDescription;
    final IBatchServiceAuthenticationKey authenticationKey;
    final IURIFile destination;

   
    transient Activator activator;


    public Replica(File source, IHash hash, IBatchServiceDescription storageDescription, IBatchServiceAuthenticationKey authenticationKey, IURIFile destination) {
        this.source = source;
        this.hash = hash;
        this.storageDescription = storageDescription;
        this.authenticationKey = authenticationKey;
        this.destination = destination;
    }


    @Override
    public IURIFile getDestination() {
        activate(ActivationPurpose.READ);
        return destination;
    }



    @Override
    public File getSource() {
        activate(ActivationPurpose.READ);
        return source;
    }

    @Override
    public IBatchServiceDescription getStorageDescription() {
        activate(ActivationPurpose.READ);
        return storageDescription;
    }

    @Override
    public IBatchServiceAuthenticationKey getAuthenticationKey() {
        activate(ActivationPurpose.READ);
        return authenticationKey;
    }

    @Override
    public void activate(ActivationPurpose purpose) {
        if (activator == null) {
            return;
        }
        activator.activate(purpose);
    }

    @Override
    public void bind(Activator activator) {
        if (this.activator == activator) {
            return;
        }
        if (activator != null && this.activator != null) {
            throw new IllegalStateException();
        }

        this.activator = activator;
    }

    public boolean hashOfSrcMatch(IHash hash) {
        if (getSourceHash() == null || hash == null) {
            return false;
        }
        return getSourceHash().equals(hash);
    }

    @Override
    public IHash getSourceHash() {
        activate(ActivationPurpose.READ);
        return hash;
    }
   
    @Override
    public String toString() {
        return "Replica [destination=" + getDestination() + ", authenticationKey=" + getAuthenticationKey() + ", hash=" + getSourceHash() + ", source=" + getSource() + ", storageDescription=" + getStorageDescription() + "]";
    }


}
