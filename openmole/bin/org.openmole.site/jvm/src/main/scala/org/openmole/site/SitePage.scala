package org.openmole.site

import scalatags.Text.TypedTag
import scalatags.Text.all.Frag

trait SitePage {
  def element: TypedTag[_ <: String]
  def header: TypedTag[_ <: String]
  def parents: Seq[PageTree]
}
case class IntegratedPage(
  header:    TypedTag[_ <: String],
  element:   TypedTag[_ <: String],
  leftMenu:  Frag,
  rightMenu: Option[TypedTag[_ <: String]],
  parents:   Seq[PageTree]) extends SitePage

case class ContentPage(header: TypedTag[_ <: String], element: TypedTag[_ <: String], parents: Seq[PageTree] = Seq()) extends SitePage
