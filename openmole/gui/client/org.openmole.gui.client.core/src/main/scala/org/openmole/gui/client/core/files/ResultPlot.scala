package org.openmole.gui.client.core.files

import org.openmole.gui.client.tool.plot.Plot.*
import org.openmole.plotlyjs.PlotlyImplicits.*
import scaladget.bootstrapnative.bsn.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool.plot.{ParallelPlot, PlotSettings, Plotter, ScatterPlot, SplomPlot, Tools, XYPlot}
import org.openmole.gui.shared.data.SequenceData
import org.openmole.plotlyjs.*

import scala.scalajs.js.timers
import org.openmole.gui.client.core.files.PlotContent.PlotState

class ResultPlot(plotData: ColumnData, plotState: PlotState):

  val axisRadios: Var[ExclusiveRadioButtons[String]] = Var(exclusiveRadios[String](Seq(), "", Seq()))

  val headers = plotData.columns.map {
      _.header
    }
  
  val plot: Var[HtmlElement] = Var(div())

  def doPlot(numberOfColumToBePlotted: NumberOfColumToBePlotted) = {
        val axisSelected = axisRadios.now().selected.now().map(_.t)
        numberOfColumToBePlotted match {
          case NumberOfColumToBePlotted.One =>
            val hIndex = headers.indexOf(axisSelected.head)
            val header = plotData.columns(hIndex).header
            val columnContents = Column.contentToSeqOfSeq(plotData.columns(hIndex).content)
            plot.set(XYPlot(columnContents, ("Records", header), PlotSettings())) // case Array)
          case NumberOfColumToBePlotted.Two =>
            val contents = axisSelected.map { sh =>
              Column.contentToSeqOfSeq(plotData.columns(headers.indexOf(sh)).content).head
            }
            plot.set(ScatterPlot(contents.head, contents.last, (axisSelected.head, axisSelected.last), PlotSettings()))
          case NumberOfColumToBePlotted.N =>
            val contents = axisSelected.map { sh =>
              Column.contentToSeqOfSeq(plotData.columns(headers.indexOf(sh)).content).head
            }
            plot.set(SplomPlot(contents, axisSelected, PlotSettings()))
          case NumberOfColumToBePlotted.Parallel=>
            val contents = axisSelected.map{sh=>
              Column.contentToSeqOfSeq(plotData.columns(headers.indexOf(sh)).content).head
            }
            plot.set(ParallelPlot(contents, axisSelected, PlotSettings()))
        }
      }

    val allHeaders = plotData.columns.map {
      _.header
    }

    val initIndexes = plotState.initialHeaders.map(ih=> allHeaders.indexOf(ih)).filter(_ >= 0)
  // Axis selection
    // 1- only one selection ( 1 column button is set)
    //    - case array: plot n times indexes x selection values in XY mode
    //    - case scalar: plot indexes x selection values in XY mode
    // 2- 2 selections ( 2 column button is set)
    //    - arrays are not proposed
    //    - scalars: selection value 1 x selection value 2 in Scatter mode
    // 3- 'N column' selection
    //    - arrays are not proposed -> Splom for all selection values
    def axisCheckBoxes(numberOfColumToBePlotted: NumberOfColumToBePlotted) = 
      
      val (arrayColumn, scalarColumn) = plotData.columns.partition {
        _.content match {
          case ArrayColumn(_) => true
          case _ => false
        }
      }

      val (availableHeaders, initialIndexes) = numberOfColumToBePlotted match {
        case NumberOfColumToBePlotted.One => 
          val indexes = 
            if initIndexes.length > 0 
            then Seq(initIndexes.head)
            else Seq(0)
      
          (allHeaders, indexes)
        case NumberOfColumToBePlotted.Two  => 
          val indexes = 
            if initIndexes.length > 1 
            then initIndexes.take(2)
            else Seq(0,1)
          (scalarColumn.map {
          _.header
        }, indexes)
        case NumberOfColumToBePlotted.N | NumberOfColumToBePlotted.Parallel =>  
          val indexes = 
            if initIndexes.length > 1 
            then initIndexes
            else Seq(0)
          (scalarColumn.map {
          _.header
        }, indexes)
      }

      lazy val todo: String => Unit = (h: String) => {
        doPlot(numberOfColumToBePlotted)
      }

      lazy val axisToggleStates = availableHeaders.map { ah => ToggleState[String](ah, ah, s"btn ${btn_danger_string}", todo) }

      val selectionMode = numberOfColumToBePlotted match {
        case NumberOfColumToBePlotted.N | NumberOfColumToBePlotted.Parallel => SelectionSize.Infinite
        case _ => SelectionSize.DefaultLength
      }

      axisRadios.set(exclusiveRadios(axisToggleStates, btn_secondary_string, initialIndexes, selectionMode))
    
    val plotModes = Seq(NumberOfColumToBePlotted.One, NumberOfColumToBePlotted.Two, NumberOfColumToBePlotted.N, NumberOfColumToBePlotted.Parallel)  

    val oneTwoNRadio = {
      val plotModeStates = plotModes.zip(Seq("1", "2", "N", "//")).map { case (pm, name) =>
        ToggleState(pm, name, "btn " + btn_danger_string, pm => {
          axisCheckBoxes(pm)
          doPlot(pm)
        })
      }
      exclusiveRadio[NumberOfColumToBePlotted](plotModeStates, btn_secondary_string, plotModes.indexOf(plotState._1))
    }

    val plotObserver = Observer[(HtmlElement, Seq[String], NumberOfColumToBePlotted)] { case (p, hs, n) =>
      n match
        case NumberOfColumToBePlotted.One => Plotly.relayout(p.ref, baseLayout("", hs.headOption.getOrElse("")))
        case NumberOfColumToBePlotted.Two => Plotly.relayout(p.ref, baseLayout(hs.lastOption.getOrElse(""), hs.headOption.getOrElse("")))
        case NumberOfColumToBePlotted.N | NumberOfColumToBePlotted.Parallel => Plotly.relayout(p.ref, splomLayout)
    }

   timers.setTimeout(500) {
      axisCheckBoxes(plotState.numberOfColumToBePlotted)
      doPlot(plotState.numberOfColumToBePlotted)
    }

  def fromColumnData = {
    div(
      child <-- axisRadios.signal.map { aRadio =>
        div(display.flex, flexDirection.column,
          div(display.flex, flexDirection.row,
            oneTwoNRadio.element.amend(margin := "10", height := "38"),
            aRadio.element.amend(display.block, margin := "10")
          ),
          plot.signal.combineWith(aRadio.selected.signal.map(_.map(_.t).reverse)).combineWith(oneTwoNRadio.selected.signal.map(_.map(_.t).head)) --> plotObserver,
          child <-- plot.signal
        )
      }
    )
  }