package org.openmole.gui.client

package object core {
  def post(implicit context: PostContext) =
    Post(timeout = context.timeout, warningTimeout = context.warningTimeout, alert = context.alert)
}
