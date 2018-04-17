package org.openmole.core.workflow.builder

import monocle._
import monocle.macros.Lenses

object InfoConfig {
  def apply()(implicit name: sourcecode.Name, definitionScope: DefinitionScope): InfoConfig =
    new InfoConfig(Some(name.value), definitionScope)
}

@Lenses case class InfoConfig(
  name:            Option[String],
  definitionScope: DefinitionScope)

object InfoBuilder {

  def apply[T](lens: Lens[T, InfoConfig]) = new InfoBuilder[T] {
    def name = lens composeLens InfoConfig.name
  }

}

trait InfoBuilder[T] extends NameBuilder[T]
