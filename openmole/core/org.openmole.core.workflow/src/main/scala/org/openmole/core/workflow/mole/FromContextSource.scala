package org.openmole.core.workflow.mole

import org.openmole.core.context.{ Context, Val }
import org.openmole.core.argument.{ FromContext, Validate }
import org.openmole.core.fileservice.FileService
import org.openmole.core.setter.{ DefinitionScope, InfoBuilder, InfoConfig, InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.validation.ValidateSource
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.random.RandomProvider
import org.openmole.core.workflow.validation
import monocle.Focus

object FromContextSource {

  implicit def isBuilder: InputOutputBuilder[FromContextSource] = InputOutputBuilder(Focus[FromContextSource](_.config))
  implicit def isInfo: InfoBuilder[FromContextSource] = InfoBuilder(Focus[FromContextSource](_.info))

  case class Parameters(context: Context, executionContext: MoleExecutionContext)(implicit val random: RandomProvider, val newFile: TmpDirectory, val fileService: FileService)

  def apply(f: Parameters => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextSource = FromContextSource(name.value)(f)
  def apply(className: String)(f: FromContextSource.Parameters => Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    new FromContextSource(
      className,
      f,
      Validate.success,
      config = InputOutputConfig(),
      info = InfoConfig()
    )
}

case class FromContextSource(
  override val className: String,
  f:                      FromContextSource.Parameters => Context,
  v:                      Validate,
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Source with ValidateSource {

  override def validate = v

  override protected def process(executionContext: MoleExecutionContext) = FromContext { p =>
    val fcp = FromContextSource.Parameters(p.context, executionContext)(p.random, p.tmpDirectory, p.fileService)
    f(fcp)
  }

  def withValidate(validate: Validate) = copy(v = v ++ validate)

}

