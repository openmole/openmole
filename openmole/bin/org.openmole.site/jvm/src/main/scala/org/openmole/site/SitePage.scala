package org.openmole.site

import scalatags.Text.TypedTag
import scalatags.Text.all.Frag

trait SitePage {
  def element: TypedTag[_ <: String]
  def header: TypedTag[_ <: String]
}
case class IntegratedPage(
  header:    TypedTag[_ <: String],
  element:   TypedTag[_ <: String],
  leftMenu:  Frag,
  rightMenu: Option[TypedTag[_ <: String]]) extends SitePage

case class ContentPage(header: TypedTag[_ <: String], element: TypedTag[_ <: String]) extends SitePage
