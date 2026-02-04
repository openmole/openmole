package org.openmole.gui.client.tool.bootstrapnative

import com.raquo.laminar.api.L.{*, given}

trait JSDependency:
  def path: String

object JSDependency:

  lazy val BOOTSTRAP_NATIVE = new JSDependency{ def path = "js/bootstrap-native.min.js" }


  def withBootstrapNative[T <: HtmlElement](f: => T): Unit = withJS(BOOTSTRAP_NATIVE)(f)

  def withJS[T <: HtmlElement](js: JSDependency*)(f: => T): Unit = {
    org.scalajs.dom.document.body.appendChild(f.ref)
    for {
      j <- js
    } yield {
      org.scalajs.dom.document.body.appendChild(
        scriptTag(`type` := "text/javascript", src := j.path).ref
      )
    }
  }
