package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.ext.client.Utils._

import scaladget.bootstrapnative.bsn._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client._
import org.openmole.gui.client.core._
import org.openmole.gui.client.core.alert.AbsolutePositioning.CenterPagePosition
import org.openmole.gui.client.tool.plot.Plot._
import org.openmole.gui.client.tool.plot._
import org.openmole.gui.ext.client.FileManager
import scaladget.bootstrapnative.Popup._
import scaladget.bootstrapnative.Selector.Options
import scaladget.bootstrapnative.Table.{BSTableStyle, DataRow}

import scala.scalajs.js.timers._

import com.raquo.laminar.api.L._

object TreeNodeTabs {

  sealed trait Activity

  object Active extends Activity

  object UnActive extends Activity
}

import TreeNodeTabs._

sealed trait TreeNodeTab {

  val safePath: SafePath
  //val activity: Activity = UnActive

  //def tabName = safePathTab.name

  //val id: String = getUUID

  lazy val buildTab: Tab[TreeNodeTab] = Tab(
    this,
    span(display.flex, flexDirection.row, alignItems.center,
      span(safePath.name),
      controlElement,
      span(cls := "close-button close-button-tab bi-x", onClick --> { e =>
        saveContent()
        panels.treeNodeTabs.remove(safePath)
        e.stopPropagation()
      })),
    block,
    onClicked = ()=> editor.foreach{_.editor.focus()}
  )

  def clone(newSafePath: SafePath): TreeNodeTab
  //  def activate = activity.set(Active)
  //
  //  def desactivate = activity.set(UnActive)

  // Get the file content to be saved
  def editableContent: Option[TreeNodeTab.EditableContent]

  def extension = safePath.extension //FileExtension(safePathTab.name)

  def editor: Option[EditorPanelUI]

  def saveContent(afterRefresh: () ⇒ Unit = () ⇒ {}): Unit

  def resizeEditor: Unit

  // controller to be added in menu bar
  val controlElement: HtmlElement

  // Graphical representation
  val block: HtmlElement
}

object TreeNodeTab {

  case class EditableContent(content: String, hash: String)

  case class OMS( safePath: SafePath,
                  initialContent: String,
                  initialHash: String,
                  showExecution: () ⇒ Unit,
                  //  setEditorErrors: Seq[ErrorWithLocation] ⇒ Unit
                ) extends TreeNodeTab {

    def clone(newSafePath: SafePath) = copy(safePath = newSafePath)

    val errors = Var(EditorErrors())
    val omsEditor = EditorPanelUI(this, safePath.extension, initialContent, initialHash)

    def editor = Some(omsEditor)

    def editableContent = {
      val (code, hash) = omsEditor.code
      Some(TreeNodeTab.EditableContent(code, hash))
    }

    def saveContent(onsaved: () ⇒ Unit) = {
      def saved(hash: String) = {
        omsEditor.initialContentHash = hash
        onsaved()
      }

      save(safePath, omsEditor, saved)
    }

    def resizeEditor = omsEditor.editor.resize()

    def indexAxises(header: SequenceHeader) = header.zipWithIndex.map { afe ⇒ IndexedAxis(afe._1, afe._2) }

    lazy val controlElement = {
      val compileDisabled = Var(false)
      val runOption = Var(false)

      def unsetErrors = {
        errors.set(EditorErrors())
        editor.foreach{_.errorMessageOpen.set(false)}
      } //setEditorErrors(Seq())

      def setError(errorDataOption: Option[ErrorData]) = {
        compileDisabled.set(false)
        errorDataOption match {
          case Some(ce: CompilationErrorData) ⇒
            errors.set(EditorErrors(
              errorsFromCompiler = ce.errors.map { ewl ⇒
                ErrorFromCompiler(ewl, ewl.line.flatMap { l ⇒
                  editor.map { x =>
                    x.editor.getSession().doc.getLine(l)
                  }
                }.getOrElse(""))
              },
              errorsInEditor = ce.errors.flatMap {
                _.line
              }
            )
            )
          case _ ⇒
        }
      }

      val yes = ToggleState("Yes", btn_primary_string, () ⇒ {})
      val no = ToggleState("No", btn_secondary_string, () ⇒ {})
      lazy val validateButton = toggle(yes, true, no, () ⇒ {})


      import scala.concurrent.duration._

      div(display.flex, flexDirection.row,
        child <-- compileDisabled.signal.map { compDisabled ⇒
          if (compDisabled) Waiter.waiter
          else
            button("CHECK", btn_secondary_outline, cls := "testButton", onClick --> { _ ⇒
              unsetErrors
              omsEditor.editor.getSession().clearBreakpoints()
              compileDisabled.set(true)
              saveContent(() ⇒
                Fetch.future(_.compileScript(ScriptData(safePath)).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { errorDataOption ⇒
                  setError(errorDataOption)
                  omsEditor.editor.focus()
                })
            })
        },

        div(display.flex, flexDirection.row,
          button("Run", btn_primary, marginLeft := "10", onClick --> { _ ⇒
            unsetErrors
            saveContent(() ⇒
              Fetch.future(_.runScript(ScriptData(safePath), validateButton.toggled.now).future, timeout = 120 seconds, warningTimeout = 60 seconds).foreach { execInfo ⇒
                showExecution()
              }
            )
          }),
          child <-- runOption.signal.map { rOption ⇒
            if (rOption) div(display.flex, flexDirection.row,
              div("Script validation", giFontFamily, fontSize := "13px", marginLeft := "10", display.flex, alignItems.center),
              validateButton.element,
              button(cls := "close closeRunOptions", tabClose, paddingBottom := "8", `type` := "button", onClick --> { _ ⇒ runOption.set(false) }, "x")
            )
            else div(cursor.pointer, marginLeft := "10", display.flex, alignItems.center, onClick --> { _ ⇒ runOption.set(true) })
          }
        )
      )
    }

    lazy val block = omsEditor.view
  }

  case class HTML(safePath: SafePath, content: HtmlElement) extends TreeNodeTab {

    def clone(newSafePath: SafePath) = copy(safePath = newSafePath)

    def editableContent = None

    def editor = None

    def saveContent(afterRefresh: () ⇒ Unit): Unit = () ⇒ {}

    def resizeEditor = {}

    lazy val controlElement: HtmlElement = div()

    lazy val block: HtmlElement = content
  }

  case class Editable(
                       safePath: SafePath,
                       dataTab: DataTab,
                       initialContent: String,
                       initialHash: String,
                       plotter: Plotter) extends TreeNodeTab {

    def clone(newSafePath: SafePath): TreeNodeTab = copy(safePath = newSafePath)

    val ed = Var(dataTab.editing)
    val editingObserver = Observer[Boolean](b => editorValue.setReadOnly(!b))

    //    lazy val isEditing = {
    //      ed.trigger(e ⇒ editorValue.setReadOnly(!e))
    //      ed
    //    }

    private lazy val editorValue = EditorPanelUI(this, safePath.extension, initialContent, initialHash)
    lazy val editor = Some(editorValue)

    def editableContent = {
      val (code, hash) = editorValue.code
      Some(TreeNodeTab.EditableContent(code, hash))
    }

    def isCSV = DataUtils.isCSV(safePath)

    val filteredSequence = dataTab.filter match {
      case First100 ⇒ dataTab.sequence.content.take(100)
      case Last100 ⇒ dataTab.sequence.content.takeRight(100)
      case _ ⇒ dataTab.sequence.content
    }

    val dataNbColumns = dataTab.sequence.header.length
    val dataNbLines = filteredSequence.size
    val plotModes = Seq((ScatterMode, ColumnPlot), (SplomMode, ColumnPlot), (HeatMapMode, LinePlot), (XYMode, ColumnPlot))

    /*if (Tools.isOneColumnTemporal(dataTab.sequence.content)) {
      Seq((XYMode, ColumnPlot))
    }
    else {
      Seq((ScatterMode, ColumnPlot), (SplomMode, ColumnPlot), (HeatMapMode, LinePlot))
    }*/

    def download(afterRefresh: () ⇒ Unit): Unit = editor.synchronized {
      // val safePath = safePath

      FileManager.download(
        safePath,
        (p: ProcessState) ⇒ {},
        (cont: String, hash) ⇒ {
          editorValue.setCode(cont, hash.get)
          if (isCSV) {
            Fetch.future(_.sequence(safePath).future).foreach {
              seq ⇒ //switchView(seq)
            }
          }
          else afterRefresh()
        },
        hash = true
      )
    }

    def saveContent(afterRefresh: () ⇒ Unit): Unit = {
      def saveTab = {
        def saved(hash: String) = {
          editorValue.initialContentHash = hash
          afterRefresh()
        }

        TreeNodeTab.save(safePath, editorValue, saved)
      }

      // if (isEditing.now) {
      if (isCSV) {
        if (dataTab.view == Raw) saveTab
      }
      //  else saveTab
      //}
      else afterRefresh()
    }

    def resizeEditor = editorValue.editor.resize()

    lazy val controlElement: HtmlElement =
      div(
        children <-- ed.signal.map { isE ⇒
          if (isE) Seq()
          else if (dataTab.view == Raw) {
            Seq(
              button(
                "Edit",
                btn_primary, onClick --> { _ ⇒ ed.update(!_) }
              )
            )
          }
          else Seq()
        },
        ed --> editingObserver
      )

    lazy val editorView = editorValue.view

    val switchString = dataTab.view match {
      case Table ⇒ Raw.toString
      case _ ⇒ Table.toString
    }

    //   def switchView(newSequence: SequenceData) = treeNodeTabs.switchEditableTo(this, dataTab.copy(sequence = newSequence), Plotter.default)

    //   def switchView(newView: EditableView) = {

    //      def switch(editing: Boolean) = treeNodeTabs.switchEditableTo(this, dataTab.copy(view = newView, editing = editing), plotter)
    //
    //      newView match {
    //        case Table ⇒ switch(false)
    //        case Plot ⇒ toView(ColumnPlot, ScatterMode)
    //        case _ ⇒
    //          if (isEditing.now)
    //            saveContent(() ⇒ {
    //              download(() ⇒ switch(isEditing.now))
    //            })
    //          else switch(isEditing.now)
    //      }
    //    }

    //    def toView(newFilter: RowFilter) = treeNodeTabs.switchEditableTo(this, dataTab.copy(filter = newFilter), plotter)
    //
    //    def toView(newAxis: Seq[Int]) = {
    //      treeNodeTabs.switchEditableTo(this, dataTab, plotter.copy(toBePlotted = ToBePloted(newAxis) /*, error = afe*/))
    //    }

    //    def toView(plotDimension: PlotDimension, newMode: PlotMode) = {
    //
    //      val indexes = {
    //        //   if (Tools.isOneColumnTemporal(dataTab.sequence.content)) ToBePloted(Seq(0, 0))
    //        // else
    //        val arrayColIndexes = dataTab.sequence.content.headOption.map {
    //          Tools.dataArrayIndexes
    //        }.getOrElse(Array()).toSeq
    //        val scalarColIndexes = (0 to dataNbColumns).toArray.filterNot(arrayColIndexes.contains).toSeq
    //
    //        ToBePloted(newMode match {
    //          case XYMode ⇒ arrayColIndexes
    //          case _ ⇒ scalarColIndexes
    //        })
    //        // plotter.toBePlotted
    //      }
    //
    //      val newPlotter = Plotter.toBePlotted(plotter.copy(plotDimension = plotDimension, plotMode = newMode, toBePlotted = indexes), dataTab.sequence)
    //      treeNodeTabs.switchEditableTo(this, dataTab.copy(view = Plot), newPlotter._1)
    //    }

    //   def toView(newError: Option[IndexedAxis]) = treeNodeTabs.switchEditableTo(this, dataTab, plotter.copy(error = newError))

    def toClosureView(newPlotClosure: Option[ClosureFilter]): Unit = {
      // treeNodeTabs.switchEditableTo(this, dataTab, plotter.copy(closureFilter = newPlotClosure))
    }

    def plotterAndSeqencedata(dataTab: DataTab, plotter: Plotter) = {
      plotter.plotMode match {
        case ScatterMode ⇒ Plotter.toBePlotted(plotter.copy(plotDimension = plotter.plotDimension, plotMode = plotter.plotMode), SequenceData(dataTab.sequence.header, filteredSequence))
        case _ ⇒ (plotter, dataTab.sequence)
      }
    }

    val raw = ToggleState("Raw", btn_primary_string, () ⇒ {}) //switchView(Raw))
    val table = ToggleState("Table", btn_primary_string, () ⇒ {}) //switchView(Table))
    val plot = ToggleState("Plot", btn_primary_string, () ⇒ {}) //switchView(Plot))

    lazy val switchButton = exclusiveRadio(Seq(raw, table, plot), btn_secondary_string, raw)

    val first100 = ToggleState("First 100", btn_primary_string, () ⇒ {}) //toView(First100))
    val last100 = ToggleState("Last 100", btn_primary_string, () ⇒ {}) //toView(Last100))
    val all = ToggleState("All", btn_danger_string, () ⇒ {}) //toView(All))

    lazy val filterRadios = exclusiveRadio(Seq(first100, last100, all), btn_secondary_string, first100)

    val rowStyle = Seq(
      display.table,
      width := "100%"
    )

    lazy val colStyle = display.tableCell

    lazy val axisCheckBoxes = {
      val (newPlotter, newSequenceData) = plotterAndSeqencedata(dataTab, plotter)

      val arrayColIndexes = newSequenceData.content.headOption.map { s => Tools.dataArrayIndexes(s.toArray) }.getOrElse(Array()).toSeq

      val ind = plotter.plotMode match {
        case XYMode ⇒ arrayColIndexes
        case _ ⇒ (0 to dataNbColumns - 1).toArray.filterNot(arrayColIndexes.contains).toSeq
      }

      val axisToggleStates = {
        newSequenceData.header.zipWithIndex.filter {
          case (_, i) ⇒
            ind.contains(i)
        }.map { a ⇒
          val todo = () ⇒ {
            val newAxis = newPlotter.plotMode match {
              case SplomMode ⇒
                if (newPlotter.toBePlotted.indexes.contains(a._2)) newPlotter.toBePlotted.indexes.filterNot(_ == a._2)
                else newPlotter.toBePlotted.indexes :+ a._2
              case HeatMapMode ⇒ Seq()
              case XYMode ⇒ Seq(a._2, a._2)
              case _ ⇒ Seq(newPlotter.toBePlotted.indexes.last, a._2)
            }
            //     toView(newAxis)
          }
          a._2 -> ToggleState(a._1, btn_primary_string, todo)
        }.toMap
      }

      radio(axisToggleStates.values.toSeq, newPlotter.toBePlotted.indexes.map {
        axisToggleStates
      }, btn_primary_string)
    }

    lazy val errorOptions: Options[IndexedAxis] = {
      val forErrors = IndexedAxis.noErrorBar +: Plotter.availableForError(dataTab.sequence.header, plotter)
      val init = forErrors.find { e ⇒ Some(e) == plotter.error }.map {
        _.fullSequenceIndex
      }

      forErrors.options(
        init.getOrElse(1) - 1,
        btn_secondary,
        (indexedAxis: IndexedAxis) ⇒ indexedAxis.title,
        () ⇒ {
          if (errorOptions.content.now.map {
            _.fullSequenceIndex
          }.getOrElse(-1) != -1) ??? //toView(errorOptions.content.now)
          else ??? //toView(None)
        }
      )
    }

    lazy val filterAxisOptions: Options[IndexedAxis] = {
      (IndexedAxis.noFilter +: dataTab.sequence.header.zipWithIndex.map { afe ⇒
        IndexedAxis(afe._1, afe._2)
      }).options(
        plotter.closureFilter.flatMap {
          _.filteredAxis.map {
            _.fullSequenceIndex
          }
        }.getOrElse(-1) + 1,
        btn_secondary,
        (indexedAxis: IndexedAxis) ⇒ indexedAxis.title,
        () ⇒ {
          if (filterAxisOptions.content.now.map {
            _.fullSequenceIndex
          }.getOrElse(-1) != -1) {
            toClosureView(Some(ClosureFilter(closure = closureInput.ref.value, filteredAxis = filterAxisOptions.content.now)))
          }
          else toClosureView(None)
        }
      )
    }

    lazy val closureInput = inputTag(
      plotter.closureFilter.map {
        _.closure
      }.getOrElse("")).amend(placeholder := "Filter closure. Ex: < 10", marginLeft := "10")

    lazy val inputFilterValidation = button(btn_primary, marginLeft := "10", "Apply", onClick --> { _ ⇒
      toClosureView(plotter.closureFilter.map {
        _.copy(closure = closureInput.ref.value)
      })
    })

    lazy val options = {
      plotter.plotDimension match {
        case ColumnPlot ⇒
          plotter.plotMode match {
            case SplomMode | HeatMapMode ⇒ div()
            case XYMode ⇒ div()
            case _ ⇒
              span(display.flex, flexDirection.row, alignItems.center,
                span("Error bars ", fontWeight.bold, marginRight := "10"),
                errorOptions.selector,
                span("Filter ", fontWeight.bold, marginLeft := "30"),
                span(marginLeft := "10", filterAxisOptions.selector),
                span(
                  maxHeight := "34",
                  children <-- filterAxisOptions.content.signal.map { faoc ⇒
                    if (faoc.map {
                      _.fullSequenceIndex
                    } == Some(-1)) Seq(span(maxHeight := "34"))
                    else Seq(form(closureInput, inputFilterValidation))
                  })
              )
          }
        case _ ⇒ div()
      }
    }

    lazy val columnsOrLines = Var(ColumnPlot)

    def plotModeRadios = {

      val plotModeStates = plotModes.map {
        case (pm, pd) ⇒
          pm -> ToggleState(pm.name, btn_secondary_string, () ⇒ ???) //toView(pd, pm))
      }.toMap

      exclusiveRadio(plotModeStates.values.toSeq, btn_secondary_string, plotModeStates(plotter.plotMode))
    }

    val infoStyle = Seq(
      fontWeight.bold,
      minWidth := "100",
      textAlign.right,
      marginRight := "20"
    )

    val plotModeInfo =
      div(glyph_info, cursor.pointer, marginLeft := "20").popover(
        div(display.flex, flexDirection.column, minWidth := "250",
          div(display.flex, flexDirection.row, span(infoStyle, "Scatter"), span("Plot a column dimension against an other one as points")),
          div(display.flex, flexDirection.row, span(infoStyle, "SPLOM"), span("Scatter plots matrix on all selected columns")),
          div(display.flex, flexDirection.row, span(infoStyle, "Series"), span("Plot a column of arrays.")),
          div(display.flex, flexDirection.row, span(infoStyle, "Heat map"), span("Plot the table as a matrix, colored by values."))
        ),
        Right
      ).render

    def jsClosure(value: String, col: Int) = {
      val closure = closureInput.ref.value
      if (closure.isEmpty) true
      else {
        plotter.closureFilter.map {
          _.filteredAxis.find(_.fullSequenceIndex == col).map { pc ⇒
            closure.replace("x", value)
          }.map { cf ⇒
            scala.util.Try(scala.scalajs.js.eval(s"function func() { return ${cf};} func()").asInstanceOf[Boolean]).toOption.getOrElse(true)
          }.getOrElse(true)
        }.getOrElse(true)
      }
    }

    lazy val block: HtmlElement = {
      div(
        div(
          if (isCSV) {
            dataTab.view match {
              case Table ⇒ div(switchButton, filterRadios)
              case Plot ⇒
                div(
                  vForm(
                    div(switchButton, filterRadios, plotModeRadios, plotModeInfo),
                    plotter.plotDimension match {
                      case LinePlot ⇒ div()
                      case _ ⇒ span(axisCheckBoxes).withLabel("x|y axis")
                    },
                    options
                  )
                )
              case _ ⇒ div(switchButton)
            }
          }
          else div()
        ),
        div(
          dataTab.view match {
            case Table ⇒
              div(overflow := "auto", height := "90%",
                {
                  if (!dataTab.sequence.header.isEmpty && !filteredSequence.isEmpty) {

                    dataTable(filteredSequence.map {
                      _.toSeq
                    })
                      .addHeaders(dataTab.sequence.header: _*)
                      .style(tableStyle = Seq(bordered_table))
                      .render.render
                    //  table(width := dataTab.sequence.header.length * 90)
                  }
                  else div()
                }
              )
            case Raw ⇒ editorView
            case _ ⇒
              val (newPlotter, newSequenceData) = plotterAndSeqencedata(dataTab, plotter)
              div()
            //              Plotter.plot(
            //                SequenceData(newSequenceData.header, newSequenceData.content),
            //                newPlotter
            //              )
          }
        )
      )
    }
  }

  def save(safePath: SafePath, editorPanelUI: EditorPanelUI, afterSave: String ⇒ Unit, overwrite: Boolean = false): Unit =
    editorPanelUI.synchronized {
      val (content, hash) = editorPanelUI.code
      Fetch.future(_.saveFile(safePath, content, Some(hash), overwrite).future).foreach {
        case (saved, savedHash) ⇒
          if (saved) afterSave(savedHash)
          else serverConflictAlert(safePath, editorPanelUI, afterSave)
      }
    }

  def serverConflictAlert(safePath: SafePath, editorPanelUI: EditorPanelUI, afterSave: String ⇒ Unit) = panels.alertPanel.string(
    s"The file ${safePath.name} has been modified on the sever. Which version do you want to keep?",
    okaction = { () ⇒ save(safePath, editorPanelUI, afterSave, true) },
    cancelaction = { () ⇒
      panels.treeNodePanel.downloadFile(safePath, saveFile = false, hash = true, onLoaded = (content: String, hash: Option[String]) ⇒ {
        editorPanelUI.setCode(content, hash.get)
      })
    },
    transform = CenterPagePosition,
    okString = "Yours",
    cancelString = "Server"
  )

  def rawBlock(htmlContent: String) = div(panelBody, htmlContent)


  def mdBlock(htmlContent: String) =
    div(
      panelBody,
      cls := "mdRendering", padding := "10",
      htmlContent
    )


  sealed trait EditableView {
    toString: String
  }

  object Raw extends EditableView {
    override def toString = "Raw"
  }

  object Table extends EditableView {
    override def toString = "Table"
  }

  object Plot extends EditableView {
    override def toString = "Plot"
  }

  sealed trait RowFilter

  object First100 extends RowFilter

  object Last100 extends RowFilter

  object All extends RowFilter

}

import TreeNodeTab._

case class DataTab(
                    sequence: SequenceData,
                    view: EditableView,
                    filter: RowFilter,
                    editing: Boolean
                  )

object DataTab {
  def build(
             sequence: SequenceData,
             view: EditableView = Raw,
             filter: RowFilter = First100,
             editing: Boolean = false) = DataTab(sequence, view, filter, editing)
}

class TreeNodeTabs {


  val tabsElement = Tabs.tabs[TreeNodeTab](Seq()).build

  val timer: Var[Option[SetIntervalHandle]] = Var(None)

  val tabsObserver = Observer[Seq[Tab[TreeNodeTab]]] { tNodeTabs =>
    if (tNodeTabs.isEmpty) {
      timer.now.foreach { handle =>
        clearInterval(handle)
      }
      timer.set(None)
    }
  }

  val timerObserver = Observer[Option[SetIntervalHandle]] { handle =>
    handle match {
      case None => timer.set(Some(setInterval(15000) {
        tabsElement.tabs.now.foreach {
          _.t.saveContent()
        }
      }))
      case _ =>
    }
  }

  def tab(safePath: SafePath) = {
    tabsElement.tabs.now.filter { tab =>
      tab.t.safePath == safePath
    }.headOption
  }

  def alreadyDisplayed(safePath: SafePath) =
    tabsElement.tabs.now.find { t ⇒
      t.t.safePath.path == safePath.path
    }

  //  def setActive(treeNodeTab: TreeNodeTab) = {
  //    tabsElement.tabs.update { ts =>
  //      indexOf(treeNodeTab.safePath) match {
  //        case Some(i) => ts.updated(i, treeNodeTab.buildTab)
  //        case _ => ts
  //      }
  //    }
  //  }


  //  def stopTimerIfNoTabs = {
  //    if (tabs.now.isEmpty) {
  //      timer.signal.map {
  //        _.foreach {
  //          clearInterval
  //        }
  //      }
  //      timer.set(None)
  //    }
  //  }
  //
  //  def startTimerIfStopped =
  //    timer.now match {
  //      case None ⇒
  //        timer.set(Some(setInterval(15000) {
  //          tabs.now.foreach {
  //            _.saveContent()
  //          }
  //        }))
  //      case _ ⇒
  //    }

  //  def setActive(tab: TreeNodeTab) = {
  //    if (tabs.now.contains(tab)) {
  //      unActiveAll
  //    }
  //    tab.activate
  //  }

  // def isActive(safePath: SafePath) = tabsElement.activeTab.map{at => at.t.safePathTab.signal.map{_ == safePath}}.getOrElse(Var(false))


  def isActive(safePath: SafePath) = tabsElement.activeTab.map { at => at.map {
    _.t.safePath == safePath
  }
  }
  // def isActive(safePath: SafePath) = tabsElement.activeTab.map { at => at.t.safePath == safePath }.getOrElse(false)


  //  def isActive(safePath: SafePath) =
  //    tabs.now.filter {
  //      _.safePathTab.now == safePath
  //    }.map {
  //      _.activity
  //    }.headOption.getOrElse(Var(TreeNodeTabs.UnActive))
  //
  //  def unActiveAll = tabs.signal.foreach {
  //    _.foreach { t ⇒ t.desactivate }
  //  }

  //  def safePaths = tabs.signal.map {
  //    _.map {
  //      _.safePathTab
  //    }
  //  }

  def find(safePath: SafePath) = {
    tabsElement.tabs.now.find { tab => tab.t.safePath == safePath }
  }

  def indexOf(safePath: SafePath) = {
    val ind = tabsElement.tabs.now().indexWhere { t => t.t.safePath == safePath }
    if (ind > 0) Some(ind)
    else None
  }


  //
  //    tabs.map{ts=> ts.map{_.safePathTab.signal.map{sp=> sp == safePath}}.headOption.getOrElse(Var(false))}
  //  }

  //    tabs.now.find { t ⇒
  //    t.safePathTab.now == safePath
  //  }

  def findOMSTab(safePath: SafePath) = find(safePath).map {
    _.t
  }.collect { case x: TreeNodeTab.OMS ⇒ x }


  //  def errorsFromCompiler(safePath: SafePath) = //: Rx[Seq[ErrorFromCompiler]] =
  //    findOMSTab(safePath).map { ooms ⇒
  //      ooms.map { oms =>
  //        oms.errors.errorsFromCompiler
  //      }.getOrElse(Seq())
  //    }
  //
  //  def errorsInEditor(safePath: SafePath) =
  //    findOMSTab(safePath).map { ooms ⇒
  //      ooms.map { oms =>
  //        oms.errors.errorsInEditor
  //      }.getOrElse(Seq())
  //    }

  //  def errors(safePath: SafePath) =
  //    findOMSTab(safePath).signal.map(_.map {
  //      _.errors
  //    }.getOrElse(EditorErrors()))
  //
  //  def setErrors(safePath: SafePath, errors: Seq[ErrorWithLocation]) = {
  //    val tab = findOMSTab(safePath)
  //    val editor = tab.editor

  // }
  //
  //  for {
  //    tab ← findOMSTab(safePath)
  //    editor ← tab.editor
  //  } {
  //    editor.changed.update(false)
  //    tab.errors() = EditorErrors(
  //      errorsFromCompiler = errors.map { ewl ⇒ ErrorFromCompiler(ewl, ewl.line.map { l ⇒ editor.editor.getSession().doc.getLine(l) }.getOrElse("")) },
  //      errorsInEditor = errors.flatMap {
  //        _.line
  //      }
  //    )
  //  }
  //}

  def add(treeNodeTab: TreeNodeTab) = tabsElement.add(treeNodeTab.buildTab)

  //  tabs.update(t => t :+ tab)
  //  startTimerIfStopped
  //  setActive(tab)


  def remove(treeNodeTab: TreeNodeTab): Unit = {
    //tab.desactivate
    //    val newTabs = tabsElement.tabs.now.filterNot {
    //      _ == tab
    //    }
    //    tabsElement.set(newTabs)

    remove(treeNodeTab.safePath)

    //    if (tabs.now.isEmpty) temporaryControl.set(div())
    //    newTabs.lastOption.map {
    //      t ⇒ setActive(t)
    //    }.isDefined
  }

  def remove(safePath: SafePath): Unit = findOMSTab(safePath).map { t =>

    tab(safePath).foreach { t =>
      tabsElement.remove(t.tabID)
    }
  }

  //  def switchEditableTo(tab: TreeNodeTab, dataTab: DataTab, plotter: Plotter) =
  //    tab.editableContent.foreach {
  //      editable ⇒
  //        val newTab = TreeNodeTab.Editable(this, tab.safePathTab.now, editable.content, editable.hash, dataTab, plotter)
  //        switchTab(tab, newTab)
  //    }

  //  def switchTab(tab: TreeNodeTab, to: TreeNodeTab) = {
  //    val index = {
  //      val i = tabs.now.indexOf(tab)
  //      if (i == -1) tabs.now.size
  //      else i
  //    }
  //
  //    remove(tab)
  //    tabs.update(t => t.take(index) ++ Seq(to) ++ t.takeRight(tabs.now.size - index))
  //
  //    // setActive(to)
  //  }

  //  def alterables: Seq[AlterableFileContent] =
  //    tabs.now.flatMap { t ⇒ t.editableContent.map(c ⇒ AlterableFileContent(t.safePathTab.now, c.content, c.hash)) }
  //
  //  def saveAllTabs(onsave: () ⇒ Unit) = {
  //    //if(org.scalajs.dom.document.hasFocus())
  //    org.openmole.gui.client.core.Post()[Api].saveFiles(alterables).call().foreach { s ⇒ onsave() }
  //  }

  def checkTabs = tabsElement.tabs.now.foreach { tab =>
    Fetch.future(_.exists(tab.t.safePath).future).foreach {
      e ⇒ if (!e) remove(tab.t)
    }
  }

  def rename(sp: SafePath, newSafePath: SafePath) = {
    tabsElement.tabs.update {
      _.map { tab =>
        tab.copy(title = span(newSafePath.name), t = tab.t.clone(newSafePath))
      }
    }
  }

  def fontSizeLink(size: Int) = {
    div("A", fontSize := s"${
      size
    }px", cursor.pointer, padding := "3", onClick --> {
      _ ⇒
        for {
          ts ← tabsElement.tabs.now
          t ← ts.t.editor
        } yield {
          t.updateFont(size)
        }
    }
    )

  }

  //  def setErrors(path: SafePath, errors: Seq[ErrorWithLocation]) =
  //    find(path).foreach { tab ⇒ tab.editor.foreach { _.setErrors(errors) } }

  val fontSizeControl = div(cls := "file-content", display.flex, flexDirection.row, alignItems.baseline, justifyContent.flexEnd,
    fontSizeLink(15),
    fontSizeLink(25),
    fontSizeLink(35)
  )


  //  val render = div(
  //    //Headers
  //    Rx {
  //      val tabList = ul(navTabs, tab_list_role)(
  //        for (t ← tabs()) yield {
  //          li(
  //            color := "yellow",
  //            marginTop := "-30",
  //            presentation_role,
  //            `class` := {
  //              t.activity() match {
  //                case Active ⇒ "active"
  //                case _      ⇒ ""
  //              }
  //            }
  //          )(
  //              a(
  //                id := t.id,
  //                tab_role,
  //                pointer,
  //                t.activity() match {
  //                  case Active ⇒ activeTab
  //                  case _      ⇒ unActiveTab
  //                },
  //                data("toggle") := "tab", onclick := { () ⇒
  //                  t.editor.foreach {
  //                    _.editor.focus
  //                  }
  //                  setActive(t)
  //                }
  //              )(
  //                  button(
  //                    ms("close") +++ tabClose,
  //                    `type` := "button",
  //                    onclick := {
  //                      () ⇒
  //                        t.saveContent(() ⇒ ()) // save
  //                        remove(t)
  //                    }
  //                  )(raw("&#215")),
  //                  t.tabName()
  //                )
  //            )
  //        }
  //      )
  //
  //      val tabDiv =
  //        div(tabContent) {
  //          tabs() match {
  //            case t if t.isEmpty ⇒
  //              div(centerElement, marginTop := "40px", "To start, create and/or open a file with a .oms extension using the file panel on the left (ex: test.oms).")
  //            case tabs ⇒
  //              for { t ← tabs } yield {
  //                div(
  //                  role := "tabpanel",
  //                  ms("tab-pane " + {
  //                    t.activity() match {
  //                      case Active ⇒ "active"
  //                      case _      ⇒ ""
  //                    }
  //                  }), id := t.id
  //                )({
  //                    t.activity() match {
  //                      case Active ⇒
  //                        temporaryControl() = t.controlElement
  //                        t.block
  //                      case UnActive ⇒ div()
  //                    }
  //                  }
  //                  )
  //              }
  //          }
  //        }
  //
  //      Sortable(
  //        tabList,
  //        SortableOptions.onEnd(
  //          (event: EventS) ⇒ {
  //            val oldI = event.oldIndex.asInstanceOf[Int]
  //            val newI = event.newIndex.asInstanceOf[Int]
  //            tabs.update(t=> t.updated(oldI, t(newI)).updated(newI, t(oldI)))
  //            setActive(tabs.now(newI))
  //          }
  //        ))
  //
  //      div( tab_panel_role,
  //        fontSizeControl,
  //        tabList,
  //        tabDiv
  //      )
  //    }
  //  )

  val render =
    tabsElement.render("editor-content").amend(
      tabsElement.tabs --> tabsObserver,
      timer --> timerObserver
    )

}
