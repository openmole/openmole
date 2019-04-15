package org.openmole.core.workflow.mole

import monocle.macros._
import org.openmole.core.workflow.builder._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workflow.validation
import org.openmole.core.workflow.validation.ValidateHook
import org.openmole.core.workspace.NewFile
import org.openmole.tool.random.RandomProvider

object FromContextHook {

  implicit def isBuilder: InputOutputBuilder[FromContextHook] = InputOutputBuilder(config)
  implicit def isInfo = InfoBuilder(FromContextHook.info)

  case class Parameters(context: Context, executionContext: MoleExecutionContext, implicit val random: RandomProvider, implicit val newFile: NewFile, implicit val fileService: FileService)
  case class ValidateParameters(inputs: Seq[Val[_]], implicit val newFile: NewFile, implicit val fileService: FileService)

  def apply(f: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = FromContextHook(name.value)(f)
  def apply(className: String)(f: Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook = FromContextHook(className, f, _ ⇒ Seq.empty, InputOutputConfig(), InfoConfig())

}

@Lenses case class FromContextHook(
  override val className: String,
  f:                      FromContextHook.Parameters ⇒ Context,
  v:                      FromContextHook.ValidateParameters ⇒ Seq[Throwable],
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = validation.Validate { p ⇒ v(FromContextHook.ValidateParameters(inputs, p.newFile, p.fileService)) }

  override protected def process(executionContext: MoleExecutionContext) = FromContext { p ⇒
    val fcp = FromContextHook.Parameters(p.context, executionContext, p.random, p.newFile, p.fileService)
    f(fcp)
  }

  def validate(validate: FromContextHook.ValidateParameters ⇒ Seq[Throwable]) = {
    def nv(p: FromContextHook.ValidateParameters) = v(p) ++ validate(p)
    copy(v = nv)
  }

}