package org.openmole.core.workflow.hook

import monocle.macros._
import org.openmole.core.context._
import org.openmole.core.argument._
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.setter._
import org.openmole.core.workflow.validation
import org.openmole.core.workflow.validation.ValidateHook
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.RandomProvider
import monocle.Focus

object FromContextHook {

  implicit def isBuilder: InputOutputBuilder[FromContextHook] = InputOutputBuilder(Focus[FromContextHook](_.config))
  implicit def isInfo: InfoBuilder[FromContextHook] = InfoBuilder(Focus[FromContextHook](_.info))

  case class Parameters(
    context:          Context,
    cache:            KeyValueCache,
    executionContext: HookExecutionContext) {
    implicit def preference: Preference = executionContext.preference
    implicit def threadProvider: ThreadProvider = executionContext.threadProvider
    implicit def fileService: FileService = executionContext.fileService
    implicit def workspace: Workspace = executionContext.workspace
    implicit def outputRedirection: OutputRedirection = executionContext.outputRedirection
    implicit def loggerService: LoggerService = executionContext.loggerService
    implicit def random: RandomProvider = executionContext.random
    implicit def newFile: TmpDirectory = executionContext.tmpDirectory
    implicit def serializerService: SerializerService = executionContext.serializerService
  }

  //case class ValidateParameters(inputs: Seq[Val[?]], implicit val newFile: TmpDirectory, implicit val fileService: FileService)

  def apply(f: Parameters => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = FromContextHook(name.value)(f)
  def apply(className: String)(f: Parameters => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    FromContextHook(className, f, Validate.success, InputOutputConfig(), InfoConfig())

}

/**
 * Generic class to write hooks from context
 *
 * @param className
 * @param f hook function
 * @param v validation
 * @param config
 * @param info
 */
case class FromContextHook(
  override val className: String,
  f:                      FromContextHook.Parameters => Context,
  v:                      Validate,
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Hook with ValidateHook {

  override def validate = v

  override protected def process(executionContext: HookExecutionContext) = FromContext { p =>
    import p._
    val fcp = FromContextHook.Parameters(
      context,
      cache = executionContext.cache,
      executionContext = executionContext)
    f(fcp)
  }

  def withValidate(validate: Validate) = copy(v = v ++ validate)
}