package org.openmole.core.workflow

import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workspace._
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.file._
import org.openmole.tool.random.Seeder

object Services {

  val dir = java.io.File.createTempFile("test", "")
  dir.delete()
  dir.mkdirs()

  val workspace = Workspace(dir)

  implicit lazy val cypher = Cypher("")
  implicit lazy val preference = Preference(workspace.persistentDir)
  implicit lazy val newFile = NewFile(workspace)
  implicit lazy val seeder = Seeder()
  implicit val serializer = SerializerService()
  implicit val threadProvider = ThreadProvider(Some(10))

}
