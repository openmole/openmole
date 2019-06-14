package org.openmole.gui.client.tool.plot

import com.definitelyscala.plotlyjs._

object ToolPlot {

  def error(dataBuilder: PlotDataBuilder, serie: Option[Serie]): PlotDataBuilder = {
    serie.flatMap {
      _.yValues.headOption.map { y ⇒
        dataBuilder.errorY(ErrorY.array(
          y.toDimension._result.values.get
        ))
      }
    }.getOrElse(dataBuilder)
  }
}