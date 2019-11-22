package org.openmole.core.workflow.mole

import monocle.macros._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
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
    context:                        Context,
    cache:                          KeyValueCache,
    implicit val preference:        Preference,
    implicit val threadProvider:    ThreadProvider,
    implicit val fileService:       FileService,
    implicit val workspace:         Workspace,
    implicit val outputRedirection: OutputRedirection,
    implicit val loggerService:     LoggerService,
    implicit val random:            RandomProvider,
    implicit val newFile:           TmpDirectory)

  case class ValidateParameters(inputs: Seq[Val[_]], implicit val newFile: TmpDirectory, implicit val fileService: FileService)

  def apply(f: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = FromContextHook(name.value)(f)
  def apply(className: String)(f: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = FromContextHook(className, f, _ ⇒ Seq.empty, InputOutputConfig(), InfoConfig())

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
  v:                      FromContextHook.ValidateParameters ⇒ Seq[Throwable],
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = validation.Validate { p ⇒ v(FromContextHook.ValidateParameters(inputs, p.newFile, p.fileService)) }

  override protected def process(executionContext: HookExecutionContext) = FromContext { p ⇒
    import p._
    val fcp = FromContextHook.Parameters(
      context,
      cache = executionContext.cache,
      preference = executionContext.preference,
      threadProvider = executionContext.threadProvider,
      fileService = executionContext.fileService,
      workspace = executionContext.workspace,
      outputRedirection = executionContext.outputRedirection,
      loggerService = executionContext.loggerService,
      random = executionContext.random,
      newFile = executionContext.newFile)
    f(fcp)
  }

  def validate(validate: FromContextHook.ValidateParameters ⇒ Seq[Throwable]) = {
    def nv(p: FromContextHook.ValidateParameters) = v(p) ++ validate(p)
    copy(v = nv)
  }

}