package org.openmole.core.environmentprovider.ssh;


import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.execution.batch.BatchEnvironmentDescription;
import org.openmole.core.model.execution.batch.IBatchEnvironment;

public class SSHEnvironmentDescription extends BatchEnvironmentDescription<SSHEnvironment> {

    String sshHost;
    Integer port;
    String remoteDir;

    public SSHEnvironmentDescription(String sshHost, Integer port, String remoteDir, Integer nbJobs) {
        this.sshHost = sshHost;
        this.port = port;
        this.remoteDir = remoteDir;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SSHEnvironmentDescription other = (SSHEnvironmentDescription) obj;
        if ((this.sshHost == null) ? (other.sshHost != null) : !this.sshHost.equals(other.sshHost)) {
            return false;
        }
        if (this.port != other.port && (this.port == null || !this.port.equals(other.port))) {
            return false;
        }
        if ((this.remoteDir == null) ? (other.remoteDir != null) : !this.remoteDir.equals(other.remoteDir)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public SSHEnvironment createEnvironment() throws InternalProcessingError {
        return new SSHEnvironment(this);
    }



}
