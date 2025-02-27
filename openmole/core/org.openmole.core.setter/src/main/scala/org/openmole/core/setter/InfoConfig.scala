package org.openmole.core.setter

import monocle._

object InfoConfig:
  def apply()(using name: sourcecode.Name, definitionScope: DefinitionScope): InfoConfig =
    new InfoConfig(Some(name.value), definitionScope)

case class InfoConfig(
  name:            Option[String],
  definitionScope: DefinitionScope)

object InfoBuilder:
  def apply[T](lens: Lens[T, InfoConfig]) = new InfoBuilder[T]:
    def name = lens andThen Focus[InfoConfig](_.name)


trait InfoBuilder[T] extends NameBuilder[T]
