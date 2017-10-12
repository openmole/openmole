package org.openmole.plugin.environment

package object oar {

  import org.openmole.plugin.environment.batch.storage.StorageService
  import org.openmole.plugin.environment.ssh.Frontend
  import org.openmole.plugin.environment.batch.jobservice.BatchJobService

  case class Cluster(frontend: Frontend, storageService: StorageService[_], jobService: BatchJobService[_])

}
