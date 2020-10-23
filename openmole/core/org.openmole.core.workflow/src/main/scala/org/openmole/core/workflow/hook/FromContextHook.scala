package org.openmole.core.workflow.hook

import monocle.macros._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.serializer.SerializerService
import org.openmole.core.threadprovider.ThreadProvider
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.validation
import org.openmole.core.workflow.validation.ValidateHook
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.tool.cache.KeyValueCache
import org.openmole.tool.logger.LoggerService
import org.openmole.tool.outputredirection.OutputRedirection
import org.openmole.tool.random.RandomProvider

object FromContextHook {

  implicit def isBuilder: InputOutputBuilder[FromContextHook] = InputOutputBuilder(config)
  implicit def isInfo = InfoBuilder(FromContextHook.info)

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
    implicit def newFile: TmpDirectory = executionContext.newFile
    implicit def serializerService: SerializerService = executionContext.serializerService
  }

  //case class ValidateParameters(inputs: Seq[Val[_]], implicit val newFile: TmpDirectory, implicit val fileService: FileService)

  def apply(f: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = FromContextHook(name.value)(f)
  def apply(className: String)(f: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    FromContextHook(className, f, _ ⇒ Validate.success, InputOutputConfig(), InfoConfig())

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
@Lenses case class FromContextHook(
  override val className: String,
  f:                      FromContextHook.Parameters ⇒ Context,
  v:                      Seq[Val[_]] ⇒ Validate,
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = v(inputs)

  override protected def process(executionContext: HookExecutionContext) = FromContext { p ⇒
    import p._
    val fcp = FromContextHook.Parameters(
      context,
      cache = executionContext.cache,
      executionContext = executionContext)
    f(fcp)
  }

  def withValidate(validate: Seq[Val[_]] ⇒ Validate) = {
    def nv(inputs: Seq[Val[_]]) = v(inputs) ++ validate(inputs)
    copy(v = nv)
  }

}