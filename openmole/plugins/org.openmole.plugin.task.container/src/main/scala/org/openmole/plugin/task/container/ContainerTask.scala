package org.openmole.plugin.task.container

/*
 * Copyright (C) 2019 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import monocle.macros.Lenses
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.fileservice.FileService
import org.openmole.core.networkservice.NetworkService
import org.openmole.core.preference.{ ConfigurationLocation, Preference }
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder.{ DefinitionScope, InfoBuilder, InfoConfig, InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.task.{ Task, TaskExecutionContext }
import org.openmole.core.workflow.validation.ValidateTask
import org.openmole.core.workspace.{ NewFile, Workspace }
import org.openmole.plugin.task.container.ContainerTask.{ Commands, downloadImage, extractImage, repositoryDirectory }
import org.openmole.plugin.task.external.{ External, ExternalBuilder }
import org.openmole.tool.cache.{ CacheKey, WithInstance }
import org.openmole.tool.lock._
import org.openmole.tool.outputredirection._
import org.openmole.tool.stream._

object ContainerTask {

  implicit def isTask: InputOutputBuilder[ContainerTask] = InputOutputBuilder(ContainerTask.config)
  implicit def isExternal: ExternalBuilder[ContainerTask] = ExternalBuilder(ContainerTask.external)
  implicit def isInfo = InfoBuilder(ContainerTask.info)

  val RegistryTimeout = ConfigurationLocation("ContainerTask", "RegistryTimeout", Some(1 minutes))
  val RegistryRetryOnError = ConfigurationLocation("ContainerTask", "RegistryRetryOnError", Some(5))

  lazy val installLockKey = LockKey()

  case class Commands(value: Vector[FromContext[String]])

  object Commands {
    implicit def fromContext(f: FromContext[String]) = Commands(Vector(f))
    implicit def fromString(f: String) = Commands(Vector(f))
    implicit def seqOfString(f: Seq[String]) = Commands(f.map(x ⇒ x: FromContext[String]).toVector)
    implicit def seqOfFromContext(f: Seq[FromContext[String]]) = Commands(f.toVector)
  }

  def extractImage(image: SavedDockerImage, directory: File): _root_.container.SavedImage = {
    import _root_.container._
    ImageBuilder.extractImage(image.file, directory, compressed = image.compressed)
  }

  def downloadImage(image: DockerImage, repository: File)(implicit preference: Preference, threadProvider: ThreadProvider): _root_.container.SavedImage = {
    import _root_.container._

    ImageDownloader.downloadContainerImage(
      _root_.container.RegistryImage(
        imageName = image.image,
        tag = image.tag,
        registry = image.registry
      ),
      repository,
      timeout = preference(RegistryTimeout),
      retry = Some(preference(RegistryRetryOnError)),
      executor = ImageDownloader.Executor.parallel(threadProvider.pool)
    )
  }

  def repositoryDirectory(workspace: Workspace) = workspace.persistentDir /> "container" /> "repos"

  def installProot(installDirectory: File) = {
    val proot = installDirectory / "proot"

    def retrieveResource(candidateFile: File, resourceName: String, executable: Boolean = false) =
      if (!candidateFile.exists()) {
        withClosable(this.getClass.getClassLoader.getResourceAsStream(resourceName))(_.copy(candidateFile))
        if (executable) candidateFile.setExecutable(true)
        candidateFile
      }

    retrieveResource(proot, "proot", true)

    proot
  }

  def apply(
    image:              ContainerImage,
    command:            Commands,
    containerSystem:    ContainerSystem                                    = Proot(),
    install:            Seq[String]                                        = Vector.empty,
    workDirectory:      OptionalArgument[String]                           = None,
    errorOnReturnValue: Boolean                                            = true,
    returnValue:        OptionalArgument[Val[Int]]                         = None,
    reuseContainer:     Boolean                                            = true,
    containerPoolKey:   CacheKey[WithInstance[_root_.container.FlatImage]] = CacheKey())(implicit name: sourcecode.Name, definitionScope: DefinitionScope, newFile: NewFile, networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, fileService: FileService, outputRedirection: OutputRedirection) = {
    new ContainerTask(
      containerSystem,
      executeInstall(containerSystem, localImage(image), install, workDirectory.option),
      command,
      workDirectory,
      errorOnReturnValue,
      returnValue,
      reuseContainer = reuseContainer,
      config = InputOutputConfig(),
      info = InfoConfig(),
      external = External(),
      containerPoolKey = containerPoolKey)
  }

  def executeInstall(containerSystem: ContainerSystem, image: _root_.container.FlatImage, install: Seq[String], workDirectory: Option[String])(implicit newFile: NewFile, outputRedirection: OutputRedirection) =
    if (install.isEmpty) image
    else {
      val retCode =
        newFile.withTmpDir { directory ⇒
          val (proot, noSeccomp, kernel) =
            containerSystem match {
              case proot: Proot ⇒ (installProot(directory), proot.noSeccomp, proot.kernel)
            }

          _root_.container.Proot.execute(
            image,
            directory / "tmp",
            commands = install,
            workDirectory = workDirectory,
            proot = proot.getAbsolutePath,
            logger = scala.sys.process.ProcessLogger.apply(s ⇒ outputRedirection.output.println(s), s ⇒ outputRedirection.error.println(s)),
            kernel = Some(kernel),
            noSeccomp = noSeccomp
          )
        }

      if (retCode != 0) throw new UserBadDataError(s"Process exited a non 0 return code ($retCode)")
      image
    }

  def localImage(image: ContainerImage)(implicit networkService: NetworkService, workspace: Workspace, threadProvider: ThreadProvider, preference: Preference, newFile: NewFile, fileService: FileService) = {
    val flattened =
      image match {
        case image: DockerImage ⇒ downloadImage(image, repositoryDirectory(workspace))
        case image: SavedDockerImage ⇒
          val imageDirectory = newDir("image")
          fileService.deleteWhenGarbageCollected(imageDirectory)
          extractImage(image, imageDirectory)
      }

    val containersDirectory = newFile.newDir("container")
    fileService.deleteWhenGarbageCollected(containersDirectory)
    _root_.container.ImageBuilder.flattenImage(flattened, containersDirectory)
  }

}

@Lenses case class ContainerTask(
  containerSystem:    ContainerSystem,
  image:              _root_.container.FlatImage,
  command:            Commands,
  workDirectory:      OptionalArgument[String],
  errorOnReturnValue: Boolean,
  returnValue:        OptionalArgument[Val[Int]],
  reuseContainer:     Boolean,
  config:             InputOutputConfig,
  external:           External,
  info:               InfoConfig,
  containerPoolKey:   CacheKey[WithInstance[_root_.container.FlatImage]]) extends Task with ValidateTask { self ⇒

  //override def validate = container.validateContainer(commands.value, uDocker.environmentVariables, external, inputs)
  def validate = Seq()

  override def process(executionContext: TaskExecutionContext) = FromContext[Context] { parameters ⇒
    import parameters._

    val (proot, noSeccomp, kernel) =
      containerSystem match {
        case proot: Proot ⇒
          executionContext.lockRepository.withLock(ContainerTask.installLockKey) {
            (ContainerTask.installProot(executionContext.tmpDirectory), proot.noSeccomp, proot.kernel)
          }
      }

    def createPool =
      WithInstance { () ⇒
        val containersDirectory = newFile.newDir("container")
        _root_.container.ImageBuilder.duplicateFlatImage(image, containersDirectory)
      }(close = _.file.recursiveDelete, pooled = reuseContainer)

    val pool = executionContext.cache.getOrElseUpdate(containerPoolKey, createPool)

    pool { container ⇒
      val retCode =
        newFile.withTmpDir { directory ⇒
          _root_.container.Proot.execute(
            container,
            directory / "tmp",
            commands = command.value.map(_.from(context)),
            workDirectory = workDirectory.option,
            proot = proot.getAbsolutePath,
            logger = scala.sys.process.ProcessLogger.apply(s ⇒ executionContext.outputRedirection.output.println(s), s ⇒ executionContext.outputRedirection.error.println(s)),
            noSeccomp = noSeccomp,
            kernel = Some(kernel)
          )
        }

      if (errorOnReturnValue && !returnValue.isDefined && retCode != 0)
        throw new UserBadDataError(s"Process exited a non 0 return code ($retCode), you can chose ignore this by settings errorOnReturnValue = true")

      context ++ returnValue.option.map(v ⇒ Variable(v, retCode))
    }
  }

}
