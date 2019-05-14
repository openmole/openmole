package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._

object ToolPlot {

  def error(dataBuilder: PlotDataBuilder, serie: Option[Serie]): PlotDataBuilder = {
    serie.map { e ⇒
      dataBuilder.errorY(ErrorY.array(e.yValues.head.toDimension._result.values.get))
    }.getOrElse(dataBuilder)
  }
}