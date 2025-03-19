package org.openmole.core.highlight

import org.openmole.core.highlight.HighLight.ObjectType.Other

sealed trait HighLight {
  def name: String
}

object HighLight:
  
  implicit def fromString(s: String): WordHighLight = WordHighLight(s)
  implicit def classToString(c: Class[?]): String = c.getSimpleName

  def objectName(o: Any) = o.getClass.getSimpleName.reverse.dropWhile(_ == '$').reverse

  enum ObjectType:
    case Task, Hook, Source, Environment, Pattern, Sampling, Domain, Other

  def TaskHighLight(name: String) = ObjectHighLight(name, ObjectType.Task)
  def HookHighLight(name: String) = ObjectHighLight(name, ObjectType.Hook)
  def SourceHighLight(name: String) = ObjectHighLight(name, ObjectType.Source)
  def EnvironmentHighLight(name: String) = ObjectHighLight(name, ObjectType.Environment)
  def PatternHighLight(name: String) = ObjectHighLight(name, ObjectType.Pattern)
  def SamplingHighLight(name: String) = ObjectHighLight(name, ObjectType.Sampling)
  def DomainHighLight(name: String) = ObjectHighLight(name, ObjectType.Domain)

  case class WordHighLight(name: String) extends HighLight
  case class ObjectHighLight(name: String, `type`: ObjectType = ObjectType.Other) extends HighLight


