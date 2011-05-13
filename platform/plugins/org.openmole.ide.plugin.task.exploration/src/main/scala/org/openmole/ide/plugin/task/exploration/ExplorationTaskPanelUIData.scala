/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.exploration

import org.openmole.core.model.sampling.ISampling
import org.openmole.ide.core.properties.PanelUIData

class ExplorationTaskPanelUIData extends PanelUIData {
  var sampling: Option[ISampling] = None
}
