package org.openmole.ui

import java.io.File

import org.openmole.core.outputredirection.OutputRedirection
import org.openmole.core.services.{ Services, ServicesContainer }
import org.openmole.tool.file._

object Test {
  def withTmpServices[T](f: Services ⇒ T) = {
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

      implicit val ws = Workspace(workspace)
      implicit val cypher = Cypher(password)
      implicit val preference = Services.preference(ws)
      implicit val newFile = NewFile(workspace)
      implicit val seeder = Seeder()
      implicit val serializerService = SerializerService()
      implicit val threadProvider = ThreadProvider()
      implicit val replicaCatalog = ReplicaCatalog(org.openmole.core.db.memory())
      implicit val authenticationStore = Services.authenticationStore(ws)
      implicit val fileService = FileService()
      implicit val randomProvider = RandomProvider(seeder.newRNG)
      implicit val eventDispatcher = EventDispatcher()
      implicit val outputRedirection = OutputRedirection()
      implicit val fileServiceCache = FileServiceCache()

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
