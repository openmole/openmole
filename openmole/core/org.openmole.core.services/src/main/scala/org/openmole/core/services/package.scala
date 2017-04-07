package org.openmole.core

import org.openmole.core.authentication._
import org.openmole.core.event._
import org.openmole.core.fileservice._
import org.openmole.core.preference._
import org.openmole.core.replication._
import org.openmole.core.serializer._
import org.openmole.core.threadprovider._
import org.openmole.core.workspace._
import org.openmole.tool.crypto._
import org.openmole.tool.random._
import org.openmole.tool.file._

package object services {

  object Services {

    def withServices[T](workspace: File, password: String)(f: Services ⇒ T) = {
      val services = Services(workspace, password)
      try f(services)
      finally dispose(services)
    }

    def preference(workspace: Workspace) = Preference(workspace.persistentDir)
    def authenticationStore(workspace: Workspace) = AuthenticationStore(workspace.persistentDir)

    def apply(workspace: File, password: String) = {
      implicit val ws = Workspace(workspace)
      implicit val cypher = Cypher(password)
      implicit val preference = Services.preference(ws)
      implicit val newFile = NewFile(workspace)
      implicit val seeder = Seeder()
      implicit val serializerService = SerializerService()
      implicit val threadProvider = ThreadProvider()
      implicit val replicaCatalog = ReplicaCatalog()
      implicit val authenticationStore = Services.authenticationStore(ws)
      implicit val fileService = FileService()
      implicit val randomProvider = RandomProvider(seeder.newRNG)
      implicit val eventDispatcher = EventDispatcher()
      new ServicesContainer()
    }

    def dispose(services: Services) = {
      util.Try(services.workspace.tmpDir.recursiveDelete)
      util.Try(services.threadProvider.stop())
    }

    def resetPassword(implicit authenticationStore: AuthenticationStore, preference: Preference) = {
      authenticationStore.delete()
      preference.clear()
    }

  }

  trait Services {
    implicit def workspace: Workspace
    implicit def preference: Preference
    implicit def cypher: Cypher
    implicit def threadProvider: ThreadProvider
    implicit def seeder: Seeder
    implicit def replicaCatalog: ReplicaCatalog
    implicit def newFile: NewFile
    implicit def authenticationStore: AuthenticationStore
    implicit def serializerService: SerializerService
    implicit def fileService: FileService
    implicit def randomProvider: RandomProvider
    implicit def eventDispatcher: EventDispatcher
  }

  class ServicesContainer(implicit
    val workspace: Workspace,
                          val preference:          Preference,
                          val cypher:              Cypher,
                          val threadProvider:      ThreadProvider,
                          val seeder:              Seeder,
                          val replicaCatalog:      ReplicaCatalog,
                          val newFile:             NewFile,
                          val authenticationStore: AuthenticationStore,
                          val serializerService:   SerializerService,
                          val fileService:         FileService,
                          val randomProvider:      RandomProvider,
                          val eventDispatcher:     EventDispatcher) extends Services

}
