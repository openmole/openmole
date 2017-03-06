package org.openmole.core.workspace

import java.util.concurrent.ConcurrentHashMap

import collection.JavaConverters._

object ConfigurationInfo {

  val all = new ConcurrentHashMap[Class[_], Seq[ConfigurationLocation[_]]]().asScala

  def add(clazz: Class[_], configurations: Seq[ConfigurationLocation[_]]) = all += (clazz → configurations)
  def remove(clazz: Class[_]) = all -= clazz

  import scala.language.experimental.macros
  import scala.reflect.macros.blackbox._

  def list[T](t: T): Seq[ConfigurationLocation[_]] = macro list_impl[T]

  def list_impl[T: c.WeakTypeTag](c: Context)(t: c.Expr[T]): c.Expr[Seq[ConfigurationLocation[_]]] = {
    import c.universe._
    val tType = weakTypeOf[T]
    val configurationLocationType = weakTypeOf[ConfigurationLocation[_]]

    val configurations =
      tType.members.collect {
        case m: MethodSymbol if m.returnType <:< configurationLocationType && m.paramLists.isEmpty && m.isPublic ⇒ m
        case m: TermSymbol if m.typeSignature <:< configurationLocationType && m.isPublic                        ⇒ m
      }

    val configurationValues = configurations.map { c ⇒ q"$t.$c" }
    val result = q"""Seq(..$configurationValues)"""

    c.Expr[Seq[ConfigurationLocation[_]]](result)
  }

}
