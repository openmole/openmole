package org.openmole.core.workflow.test

import java.util.logging.Level
import org.openmole.core.event.EventDispatcher
import org.openmole.core.fileservice.{FileService, FileServiceCache}
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.timeservice.TimeService
import org.openmole.core.workflow.mole.MoleServices
import org.openmole.core.workspace.{TmpDirectory, Workspace}
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.crypto.Cypher
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.{RandomProvider, Seeder}

object Stubs:

  implicit val scope: org.openmole.core.setter.DefinitionScope = org.openmole.core.setter.DefinitionScope.User

  val dir = java.io.File.createTempFile("test", "")
  dir.delete()
  dir.mkdirs()

  implicit val workspace: Workspace = Workspace(dir)

  implicit lazy val cypher: Cypher = Cypher("")
  implicit lazy val preference: Preference = Preference.memory()
  implicit lazy val tmpDirectory: TmpDirectory = TmpDirectory(dir)
  implicit lazy val seeder: Seeder = Seeder()
  implicit val serializer: SerializerService = SerializerService()
  implicit val threadProvider: ThreadProvider = ThreadProvider(Some(10))
  implicit val eventDispatcher: EventDispatcher = EventDispatcher()
  implicit val fileService: FileService = FileService()
  implicit val randomProvider: RandomProvider = RandomProvider(seeder.newRNG)
  implicit val outputRedirection: OutputRedirection = OutputRedirection()
  implicit val network: NetworkService = NetworkService(None)
  implicit val fileServiceCache: FileServiceCache = FileServiceCache()
  implicit val loggerService: LoggerService = LoggerService()
  implicit val timeService: TimeService = TimeService()
  implicit val cache: KeyValueCache = KeyValueCache()

  implicit val moleServices: MoleServices = MoleServices.create(dir)

