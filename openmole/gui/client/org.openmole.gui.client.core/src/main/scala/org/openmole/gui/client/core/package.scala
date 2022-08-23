package org.openmole.gui.client

import org.openmole.gui.ext.api

package object core {
  def post(implicit context: PostContext) =
    Post(timeout = context.timeout, warningTimeout = context.warningTimeout, alert = context.alert)


}
