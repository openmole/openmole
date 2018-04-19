package org.openmole.core.workflow.tools

import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.outputredirection.OutputRedirection
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workspace._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.random._

object Stubs {

  implicit val scope = org.openmole.core.workflow.builder.DefinitionScope.User

  val dir = java.io.File.createTempFile("test", "")
  dir.delete()
  dir.mkdirs()

  implicit val workspace = Workspace(dir)

  implicit lazy val cypher = Cypher("")
  implicit lazy val preference = Preference(workspace.persistentDir)
  implicit lazy val newFile = NewFile(workspace)
  implicit lazy val seeder = Seeder()
  implicit val serializer = SerializerService()
  implicit val threadProvider = ThreadProvider(Some(10))
  implicit val eventDispatcher = EventDispatcher()
  implicit val fileService = FileService()
  implicit val randomProvider = RandomProvider(seeder.newRNG)
  implicit val outputRedirection = OutputRedirection()
  implicit val network = NetworkService()
}
