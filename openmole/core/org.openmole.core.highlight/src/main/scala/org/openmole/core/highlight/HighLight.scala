package org.openmole.core.highlight

sealed trait HighLight {
  def name: String
}

object HighLight {

  implicit def fromString(s: String): WordHighLight = WordHighLight(s)
  implicit def classToString(c: Class[?]): String = c.getSimpleName

  def objectName(o: Any) = o.getClass.getSimpleName.reverse.dropWhile(_ == '$').reverse

  case class TaskHighLight(name: String) extends HighLight
  case class HookHighLight(name: String) extends HighLight
  case class SourceHighLight(name: String) extends HighLight
  case class EnvironmentHighLight(name: String) extends HighLight
  case class SetterHighLight(name: String) extends HighLight
  case class AdderHighLight(name: String) extends HighLight
  case class PatternHighLight(name: String) extends HighLight
  case class TransitionHighLight(name: String) extends HighLight
  case class SamplingHighLight(name: String) extends HighLight
  case class WordHighLight(name: String) extends HighLight
  case class DomainHighLight(name: String) extends HighLight
  case class OtherHighLight(name: String) extends HighLight
}
