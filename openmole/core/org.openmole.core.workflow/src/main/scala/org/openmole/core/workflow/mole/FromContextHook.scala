package org.openmole.core.workflow.mole

import monocle.macros._
import org.openmole.core.workflow.builder._
import org.openmole.core.context._
import org.openmole.core.expansion._
import org.openmole.core.workflow.validation
import org.openmole.core.workflow.validation.ValidateHook

object FromContextHook {

  implicit def isBuilder: InputOutputBuilder[FromContextHook] = InputOutputBuilder(config)
  implicit def isInfo = InfoBuilder(FromContextHook.info)

  def apply(className: String)(fromContext: FromContext.Parameters ⇒ Context)(implicit name: sourcecode.Name, definitionScope: DefinitionScope): FromContextHook =
    FromContextHook(className, FromContext(fromContext), InputOutputConfig(), InfoConfig())
}

@Lenses case class FromContextHook(
  override val className: String,
  fromContext:            FromContext[Context],
  config:                 InputOutputConfig,
  info:                   InfoConfig) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]): validation.Validate = validation.Validate { p ⇒
    import p._
    fromContext.validate(inputs)
  }

  override protected def process(executionContext: MoleExecutionContext) = fromContext
}