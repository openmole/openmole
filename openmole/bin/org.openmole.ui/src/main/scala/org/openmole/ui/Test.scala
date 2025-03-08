package org.openmole.ui

import org.openmole.core.authentication.AuthenticationStore
import org.openmole.core.preference.Preference

import java.io.File
import org.openmole.core.services.{Services, ServicesContainer}
import org.openmole.core.timeservice.TimeService
import org.openmole.tool.file._
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection

object Test {
  def withTmpServices[T](f: Services => T) = {
    val tmpDirectory = {
      val newF = java.io.File.createTempFile("workspace", "")
      newF.delete()
      newF.mkdirs()
      newF
    }

    def build(workspace: File, password: String) = {
      import org.openmole.core.event._
      import org.openmole.core.fileservice._
      import org.openmole.core.replication._
      import org.openmole.core.serializer._
      import org.openmole.core.threadprovider._
      import org.openmole.core.workspace._
      import org.openmole.tool.crypto._
      import org.openmole.tool.random._
      import org.openmole.core.networkservice._

      implicit val ws: Workspace = Workspace(workspace)
      implicit val cypher: Cypher = Cypher(password)
      implicit val preference: Preference = Services.preference(ws)
      implicit val newFile: TmpDirectory = TmpDirectory(workspace)
      implicit val seeder: Seeder = Seeder()
      implicit val serializerService: SerializerService = SerializerService()
      implicit val threadProvider: ThreadProvider = ThreadProvider()
      implicit val replicaCatalog: ReplicaCatalog = ReplicaCatalog(org.openmole.core.db.memory())
      implicit val authenticationStore: AuthenticationStore = Services.authenticationStore(ws)
      implicit val fileService: FileService = FileService()
      implicit val randomProvider: RandomProvider = RandomProvider(seeder.newRNG)
      implicit val eventDispatcher: EventDispatcher = EventDispatcher()
      implicit val outputRedirection: OutputRedirection = OutputRedirection()
      implicit val networkService: NetworkService = NetworkService(None)
      implicit val fileServiceCache: FileServiceCache = FileServiceCache()
      implicit val loggerService: LoggerService = LoggerService()
      implicit val timeService: TimeService = TimeService()

      new ServicesContainer()
    }

    val services = build(tmpDirectory, "")

    try f(services)
    finally {
      Services.dispose(services)
      tmpDirectory.recursiveDelete
    }
  }
}
