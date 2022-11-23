package org.openmole.gui.client.core.files

import org.openmole.gui.client.tool.plot.Plot.*
import org.openmole.plotlyjs.PlotlyImplicits.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.plot.{PlotSettings, Plotter, ScatterPlot, Tools, XYPlot}
import org.openmole.gui.ext.data.SequenceData
import org.openmole.plotlyjs.*

object ResultPlot {

  def fromCSV(plotData: ColumnData) = {


    val oneTwoNRadio = {
      val plotModeStates = Seq(OneColumn, TwoColumn, NColumn).zip(Seq("1", "2", "N")).map { case (pm, name) =>
        ToggleState(pm, name, "btn " + btn_danger_string) //toView(pd, pm))
      }
      exclusiveRadio[NumberOfColumToBePlotted](plotModeStates, btn_secondary_string, 0)
    }

    val headers = plotData.columns.map {
      _.header
    }
    val selectedHeaders: Var[Seq[String]] = Var(Seq())
    val plot: Var[HtmlElement] = Var(div())

    // Axis selection
    // 1- only one selection ( 1 column button is set)
    //    - case array: plot n times indexes x selection values in XY mode
    //    - case scalar: plot indexes x selection values in XY mode
    // 2- 2 selections ( 2 column button is set)
    //    - arrays are not proposed
    //    - scalars: selection value 1 x selection value 2 in Scatter mode
    // 3- 'N column' selection
    //    - arrays are not proposed -> Splom for all selection values

    def axisCheckBoxes(numberOfColumToBePlotted: NumberOfColumToBePlotted) = {

      val allHeaders = plotData.columns.map {
        _.header
      }
      val (arrayColumn, scalarColumn) = plotData.columns.partition {
        _.content match {
          case ArrayColumn(_) => true
          case _ => false
        }
      }

      val (availableHeaders, initialIndexes) = numberOfColumToBePlotted match {
        case OneColumn => (allHeaders, Seq(0))
        case TwoColumn | NColumn => (scalarColumn.map {
          _.header
        }, Seq(0, 1))
      }

      val todo: String => Unit = (h: String) => {
        numberOfColumToBePlotted match {
          case OneColumn =>
            selectedHeaders.set(Seq(h))
            val header = headers.indexOf(h)
            val columnContents = Column.contentToSeqOfSeq(plotData.columns(header).content)

            plot.set(
              XYPlot(columnContents, ("Records", h), PlotSettings()) // case Array
            )
          case TwoColumn =>
            selectedHeaders.update(sh => (sh.length match {
              case 1 => sh
              case 0 => headers.take(2)
              case _ => sh.tail
            }) :+ h)
            val selected = selectedHeaders.now()
            val contents = selected.map { sh =>
              Column.contentToSeqOfSeq(plotData.columns(headers.indexOf(sh)).content).head
            }
            plot.set(
              ScatterPlot(contents.head, contents.last, (selected.head, selected.last), PlotSettings())
            )
          case _ => Seq()
        }
      }
      val axisToggleStates = availableHeaders.map { ah => ToggleState[String](ah, ah, s"btn ${btn_danger_string}", todo) }

      exclusiveRadios(axisToggleStates, btn_secondary_string, initialIndexes)
    }

    val plotObserver = Observer[(HtmlElement, Seq[String])] { case (p, hs) =>
      Plotly.relayout(p.ref, baseLayout(hs.headOption.getOrElse(""), hs.lastOption.getOrElse("Records")))
    }

    div(
      oneTwoNRadio.element,
      child <-- oneTwoNRadio.selected.signal.map { as =>
        val activePlotMode = as.head
        div(
          axisCheckBoxes(activePlotMode.t).element.amend(display.block),
          child <-- plot.signal,
          plot.signal.combineWith(selectedHeaders) --> plotObserver
        )
      }
    )
  }


}
