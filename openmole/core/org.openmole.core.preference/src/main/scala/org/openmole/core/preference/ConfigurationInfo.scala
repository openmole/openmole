package org.openmole.core.preference

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.reflect.macros.whitebox._

object ConfigurationInfo {

  val all = new ConcurrentHashMap[AnyRef, Seq[ConfigurationLocation[_]]]().asScala

  def register(id: AnyRef, configurations: Seq[ConfigurationLocation[_]]) = all += (id → configurations)
  def unregister(clazz: AnyRef) = all -= clazz

  import scala.language.experimental.macros

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
