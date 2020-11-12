package org.openmole.gui.client.core.files

import org.openmole.gui.ext.data._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import autowire._
import org.openmole.gui.ext.client.Utils._

import scala.concurrent.duration._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.ext.api.Api
import org.scalajs.dom.raw.{ Event, HTMLElement }
import scalatags.JsDom.all.{ raw, _ }
import scalatags.JsDom.TypedTag
import scalatags.JsDom._
import org.openmole.gui.ext.client._
import org.openmole.gui.client.core._
import DataUtils._
import net.scalapro.sortable._
import org.openmole.gui.client.core.files.TreeNodeTab.{ EditableView, First100, Raw, RowFilter }
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.client.tool.plot.Plot._
import org.openmole.gui.client.tool.plot._
import org.openmole.gui.ext.client.FileManager
import scaladget.bootstrapnative.Popup._
import rx._
import scaladget.bootstrapnative.Selector.Options
import scalatags.JsDom

import scala.scalajs.js.timers._

object TreeNodeTabs {

  sealed trait Activity

  object Active extends Activity

  object UnActive extends Activity

  private def findOMSTab(treeNodeTabs: TreeNodeTabs, safePath: SafePath) =
    treeNodeTabs.find(safePath).collect { case x: TreeNodeTab.OMS ⇒ x }

  def errorsFromCompiler(treeNodeTabs: TreeNodeTabs, safePath: SafePath): Rx[Seq[ErrorFromCompiler]] =
    findOMSTab(treeNodeTabs, safePath).map { oms ⇒
      oms.errors.map(_.errorsFromCompiler)
    }.getOrElse(Rx(Seq()))

  def errorsInEditor(treeNodeTabs: TreeNodeTabs, safePath: SafePath): Rx[Seq[Int]] =
    findOMSTab(treeNodeTabs, safePath).collect { case x: TreeNodeTab.OMS ⇒ x }.map { oms ⇒
      oms.errors.map(_.errorsInEditor)
    }.getOrElse(Rx(Seq()))

  def errors(treeNodeTabs: TreeNodeTabs, safePath: SafePath) =
    findOMSTab(treeNodeTabs, safePath).map(_.errors).getOrElse(Rx(EditorErrors()))

  def setErrors(treeNodeTabs: TreeNodeTabs, safePath: SafePath, errors: Seq[ErrorWithLocation]) =
    for {
      tab ← findOMSTab(treeNodeTabs, safePath)
      editor ← tab.editor
    } {
      editor.changed.update(false)
      tab.errors() = EditorErrors(
        errorsFromCompiler = errors.map { ewl ⇒ ErrorFromCompiler(ewl, ewl.line.map { l ⇒ editor.editor.getSession().doc.getLine(l) }.getOrElse("")) },
        errorsInEditor = errors.flatMap {
          _.line
        }
      )
    }

}

import TreeNodeTabs._

sealed trait TreeNodeTab {

  val safePathTab: Var[SafePath]
  val activity: Var[Activity] = Var(UnActive)

  val tabName = Var(safePathTab.now.name)
  val id: String = getUUID

  def activate = {
    activity() = Active
  }

  def desactivate = {
    activity() = UnActive
  }

  def extension: FileExtension = FileExtension(safePathTab.now.name)

  // Get the file content to be saved
  def editableContent: Option[String]

  def editor: Option[EditorPanelUI]

  def refresh(afterRefresh: () ⇒ Unit = () ⇒ {}): Unit

  def resizeEditor: Unit

  // controller to be added in menu bar
  val controlElement: TypedTag[HTMLElement]

  // Graphical representation
  val block: TypedTag[_ <: HTMLElement]
}

object TreeNodeTab {

  implicit val ctx = Ctx.Owner.safe()

  case class OMS(
    treeNodeTabs:    TreeNodeTabs,
    safePath:        SafePath,
    initialContent:  String,
    showExecution:   () ⇒ Unit,
    setEditorErrors: Seq[ErrorWithLocation] ⇒ Unit) extends TreeNodeTab {

    val errors = Var(EditorErrors())

    lazy val safePathTab = Var(safePath)

    lazy val omsEditor = EditorPanelUI(treeNodeTabs, safePath, FileExtension.OMS, initialContent)

    def editor = Some(omsEditor)

    def editableContent = Some(omsEditor.code)

    def refresh(onsaved: () ⇒ Unit) = save(safePathTab.now, omsEditor, onsaved)

    def resizeEditor = omsEditor.editor.resize()

    def indexAxises(header: SequenceHeader) = header.zipWithIndex.map { afe ⇒ IndexedAxis(afe._1, afe._2) }

    lazy val controlElement = {
      val compileDisabled = Var(false)
      val runOption = Var(false)

      def unsetErrors = setEditorErrors(Seq())

      def setError(errorDataOption: Option[ErrorData]) = {
        compileDisabled.update(false)
        errorDataOption match {
          case Some(ce: CompilationErrorData) ⇒ setEditorErrors(ce.errors)
          case _                              ⇒
        }
      }

      lazy val validateButton = toggle(true, "Yes", "No")

      div(display.flex, flexDirection.row)(
        Rx {
          if (compileDisabled()) Waiter.waiter
          else
            button("Test", btn_default, onclick := { () ⇒
              unsetErrors
              compileDisabled.update(true)
              refresh(() ⇒
                Post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].compileScript(ScriptData(safePathTab.now)).call().foreach { errorDataOption ⇒
                  setError(errorDataOption)
                })
            })
        },
        div(display.flex, flexDirection.row)(
          button("Run", btn_primary, marginLeft := 10, onclick := { () ⇒
            unsetErrors
            refresh(() ⇒
              Post(timeout = 120 seconds, warningTimeout = 60 seconds)[Api].runScript(ScriptData(safePathTab.now), validateButton.position.now).call().foreach { execInfo ⇒
                showExecution()
              }
            )
          }),
          Rx {
            if (runOption()) div(display.flex, flexDirection.row)(
              div("Script validation", giFontFamily, fontSize := "13px", marginLeft := 10, display.flex, alignItems.center),
              validateButton.render(border := "1px solid black", marginLeft := 10),
              button(ms("close closeRunOptions") +++ tabClose :+ (paddingBottom := 8), `type` := "button", onclick := { () ⇒ runOption.update(false) })(raw("&#215")))
            else div(OMTags.options, pointer, marginLeft := 10, display.flex, alignItems.center)(onclick := { () ⇒ runOption.update(true) })
          }
        )
      )
    }

    lazy val block = omsEditor.view
  }

  case class HTML(safePath: SafePath, _block: TypedTag[_ <: HTMLElement]) extends TreeNodeTab {
    lazy val safePathTab = Var(safePath)

    def editableContent = None

    def editor = None

    def refresh(afterRefresh: () ⇒ Unit): Unit = () ⇒ {}

    def resizeEditor = {}

    lazy val controlElement: TypedTag[HTMLElement] = div()
    lazy val block: TypedTag[_ <: HTMLElement] = _block
  }

  case class Editable(
    treeNodeTabs:   TreeNodeTabs,
    safePath:       SafePath,
    initialContent: String,
    dataTab:        DataTab,
    plotter:        Plotter) extends TreeNodeTab {

    lazy val safePathTab = Var(safePath)

    lazy val isEditing = {
      val ed = Var(dataTab.editing)
      ed.trigger(e ⇒ editableEditor.setReadOnly(!e))
      ed
    }

    def editableContent = Some(editableEditor.code)

    def isCSV = DataUtils.isCSV(safePath)

    val filteredSequence = dataTab.filter match {
      case First100 ⇒ dataTab.sequence.content.take(100)
      case Last100  ⇒ dataTab.sequence.content.takeRight(100)
      case _        ⇒ dataTab.sequence.content
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

    lazy val editableEditor = EditorPanelUI(treeNodeTabs, safePath, extension, initialContent, if (isCSV) paddingBottom := 80 else emptyMod)

    def editor = Some(editableEditor)

    def download(afterRefresh: () ⇒ Unit): Unit = editor.synchronized {
      FileManager.download(
        safePathTab.now,
        (p: ProcessState) ⇒ {},
        (cont: String) ⇒ {
          editableEditor.setCode(cont)
          if (isCSV) {
            Post()[Api].sequence(safePathTab.now).call().foreach {
              seq ⇒ switchView(seq)
            }
          }
          else afterRefresh()
        }
      )
    }

    def refresh(afterRefresh: () ⇒ Unit): Unit = {
      def saveTab = TreeNodeTab.save(safePathTab.now, editableEditor, afterRefresh)

      if (isEditing.now) {
        if (isCSV) {
          if (dataTab.view == Raw) saveTab
        }
        else saveTab
      }
      else afterRefresh()
    }

    def resizeEditor = editableEditor.editor.resize()

    lazy val controlElement: TypedTag[HTMLElement] =
      div(
        Rx {
          if (isEditing()) Seq()
          else if (dataTab.view == Raw) {
            Seq(
              button(
                "Edit",
                btn_primary, onclick := { () ⇒ isEditing() = !isEditing.now }
              )
            )
          }
          else Seq()
        }
      )

    lazy val editorView = editableEditor.view

    val switchString = dataTab.view match {
      case Table ⇒ Raw.toString
      case _     ⇒ Table.toString
    }

    def switchView(newSequence: SequenceData) = treeNodeTabs.switchEditableTo(this, dataTab.copy(sequence = newSequence), Plotter.default)

    def switchView(newView: EditableView) = {

      def switch(editing: Boolean) = treeNodeTabs.switchEditableTo(this, dataTab.copy(view = newView, editing = editing), plotter)

      newView match {
        case Table ⇒ switch(false)
        case Plot  ⇒ toView(ColumnPlot, ScatterMode)
        case _ ⇒
          if (isEditing.now)
            refresh(() ⇒ {
              download(() ⇒ switch(isEditing.now))
            })
          else switch(isEditing.now)
      }
    }

    def toView(newFilter: RowFilter) = treeNodeTabs.switchEditableTo(this, dataTab.copy(filter = newFilter), plotter)

    def toView(newAxis: Seq[Int]) = {
      treeNodeTabs.switchEditableTo(this, dataTab, plotter.copy(toBePlotted = ToBePloted(newAxis) /*, error = afe*/ ))
    }

    def toView(plotDimension: PlotDimension, newMode: PlotMode) = {

      val indexes = {
        //   if (Tools.isOneColumnTemporal(dataTab.sequence.content)) ToBePloted(Seq(0, 0))
        // else
        val arrayColIndexes = dataTab.sequence.content.headOption.map {
          Tools.dataArrayIndexes
        }.getOrElse(Array()).toSeq
        val scalarColIndexes = (0 to dataNbColumns).toArray.filterNot(arrayColIndexes.contains).toSeq

        ToBePloted(newMode match {
          case XYMode ⇒ arrayColIndexes
          case _      ⇒ scalarColIndexes
        })
        // plotter.toBePlotted
      }

      val newPlotter = Plotter.toBePlotted(plotter.copy(plotDimension = plotDimension, plotMode = newMode, toBePlotted = indexes), dataTab.sequence)
      treeNodeTabs.switchEditableTo(this, dataTab.copy(view = Plot), newPlotter._1)
    }

    def toView(newError: Option[IndexedAxis]) = treeNodeTabs.switchEditableTo(this, dataTab, plotter.copy(error = newError))

    def toClosureView(newPlotClosure: Option[ClosureFilter]): Unit = {
      treeNodeTabs.switchEditableTo(this, dataTab, plotter.copy(closureFilter = newPlotClosure))
    }

    def plotterAndSeqencedata(dataTab: DataTab, plotter: Plotter) = {
      plotter.plotMode match {
        case ScatterMode ⇒ Plotter.toBePlotted(plotter.copy(plotDimension = plotter.plotDimension, plotMode = plotter.plotMode), SequenceData(dataTab.sequence.header, filteredSequence))
        case _           ⇒ (plotter, dataTab.sequence)
      }
    }

    lazy val switchButton = radios(margin := 20)(
      selectableButton("Raw", dataTab.view == Raw, onclick = () ⇒ switchView(Raw)),
      selectableButton("Table", dataTab.view == Table, onclick = () ⇒ switchView(Table)),
      selectableButton("Plot", dataTab.view == Plot, onclick = () ⇒ switchView(Plot))
    )

    lazy val filterRadios = radios(marginLeft := 40)(
      selectableButton("First 100", dataTab.filter == First100, onclick = () ⇒ toView(First100)),
      selectableButton("Last 100", dataTab.filter == Last100, onclick = () ⇒ toView(Last100)),
      selectableButton("All", dataTab.filter == All, modifierSeq = btn_danger, onclick = () ⇒ toView(All))
    )

    val rowStyle: ModifierSeq = Seq(
      display.table,
      width := "100%"
    )

    lazy val colStyle: ModifierSeq = Seq(
      display.`table-cell`
    )

    lazy val axisCheckBoxes = {
      val (newPlotter, newSequenceData) = plotterAndSeqencedata(dataTab, plotter)

      val arrayColIndexes = newSequenceData.content.headOption.map {
        Tools.dataArrayIndexes
      }.getOrElse(Array()).toSeq

      val ind = plotter.plotMode match {
        case XYMode ⇒ arrayColIndexes
        case _      ⇒ (0 to dataNbColumns - 1).toArray.filterNot(arrayColIndexes.contains).toSeq
      }
      checkboxes(colStyle +++ (margin := 20))(
        newSequenceData.header.zipWithIndex.filter {
          case (_, i) ⇒
            ind.contains(i)
        }.map { a ⇒
          val defaultActive = {
            plotter.plotMode match {
              case XYMode ⇒ newPlotter.toBePlotted.indexes.head == a._2
              case _      ⇒ newPlotter.toBePlotted.indexes.contains(a._2)
            }
          }
          selectableButton(a._1, defaultActive, onclick = () ⇒ {
            val newAxis = newPlotter.plotMode match {
              case SplomMode ⇒
                if (newPlotter.toBePlotted.indexes.contains(a._2)) newPlotter.toBePlotted.indexes.filterNot(_ == a._2)
                else newPlotter.toBePlotted.indexes :+ a._2
              case HeatMapMode ⇒ Seq()
              case XYMode      ⇒ Seq(a._2, a._2)
              case _           ⇒ Seq(newPlotter.toBePlotted.indexes.last, a._2)
            }
            toView(newAxis)
          })
        }: _*)
    }

    lazy val errorOptions: Options[IndexedAxis] = {
      val forErrors = IndexedAxis.noErrorBar +: Plotter.availableForError(dataTab.sequence.header, plotter)
      val init = forErrors.find { e ⇒ Some(e) == plotter.error }.map {
        _.fullSequenceIndex
      }

      forErrors.options(
        init.getOrElse(1) - 1,
        btn_default,
        (indexedAxis: IndexedAxis) ⇒ indexedAxis.title,
        () ⇒ {
          if (errorOptions.content.now.map {
            _.fullSequenceIndex
          }.getOrElse(-1) != -1) toView(errorOptions.content.now)
          else toView(None)
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
        btn_default,
        (indexedAxis: IndexedAxis) ⇒ indexedAxis.title,
        () ⇒ {
          if (filterAxisOptions.content.now.map {
            _.fullSequenceIndex
          }.getOrElse(-1) != -1) {
            toClosureView(Some(ClosureFilter(closure = closureInput.value, filteredAxis = filterAxisOptions.content.now)))
          }
          else toClosureView(None)
        }
      )
    }

    lazy val closureInput = input(placeholder := "Filter closure. Ex: < 10", marginLeft := 10)(value := plotter.closureFilter.map {
      _.closure
    }.getOrElse("")).render

    lazy val inputFilterValidation = button(btn_primary, marginLeft := 10, "Apply", onclick := { () ⇒
      toClosureView(plotter.closureFilter.map {
        _.copy(closure = closureInput.value)
      })
    })

    lazy val options = {
      plotter.plotDimension match {
        case ColumnPlot ⇒
          plotter.plotMode match {
            case SplomMode | HeatMapMode ⇒ div()
            case XYMode                  ⇒ div()
            case _ ⇒
              scalatags.JsDom.tags.span(display.flex, flexDirection.row, alignItems.center)(
                scalatags.JsDom.tags.span("Error bars ", fontWeight.bold, marginRight := 10),
                errorOptions.selector,
                scalatags.JsDom.tags.span("Filter ", fontWeight.bold, marginLeft := 30),
                scalatags.JsDom.tags.span(marginLeft := 10)(filterAxisOptions.selector),
                scalatags.JsDom.tags.span(maxHeight := 34)(
                  Rx {
                    if (filterAxisOptions.content().map {
                      _.fullSequenceIndex
                    } == Some(-1)) Seq(scalatags.JsDom.tags.span(maxHeight := 34))
                    else Seq(form(closureInput, inputFilterValidation))
                  })
              )
          }
        case _ ⇒ div()
      }
    }

    lazy val columnsOrLines = Var(ColumnPlot)

    def plotModeRadios = {

      radios(marginLeft := 40)(
        plotModes.map {
          case (pm, pd) ⇒
            selectableButton(pm.name, plotter.plotMode == pm, onclick = () ⇒ toView(pd, pm))
        }: _*
      )
    }

    val infoStyle: ModifierSeq = Seq(
      fontWeight.bold,
      minWidth := 100,
      textAlign := "right",
      marginRight := 20
    )

    val plotModeInfo =
      org.openmole.gui.client.tool.Popover(
        div(glyph_info, pointer, marginLeft := 20),
        0,
        div(styles.display.flex, flexDirection.column, minWidth := 250)(
          div(styles.display.flex, flexDirection.row)(tags.span(infoStyle)("Scatter"), tags.span("Plot a column dimension against an other one as points")),
          div(styles.display.flex, flexDirection.row)(tags.span(infoStyle)("SPLOM"), tags.span("Scatter plots matrix on all selected columns")),
          div(styles.display.flex, flexDirection.row)(tags.span(infoStyle)("Series"), tags.span("Plot a column of arrays.")),
          div(styles.display.flex, flexDirection.row)(tags.span(infoStyle)("Heat map"), tags.span("Plot the table as a matrix, colored by values."))
        ),
        Right
      ).render

    def jsClosure(value: String, col: Int) = {
      val closure = closureInput.value
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

    lazy val block: TypedTag[_ <: HTMLElement] = {
      div(
        if (isCSV) {
          dataTab.view match {
            case Table ⇒ div(switchButton.render, filterRadios.render)
            case Plot ⇒
              div(
                vForm(
                  div(switchButton.render, filterRadios.render, plotModeRadios.render, plotModeInfo).render,
                  plotter.plotDimension match {
                    case LinePlot ⇒ div().render
                    case _        ⇒ scalatags.JsDom.tags.span(axisCheckBoxes.render).render.withLabel("x|y axis")
                  },
                  options.render
                )
              )
            case _ ⇒ div(switchButton.render, div.render)
          }
        }
        else div,
        dataTab.view match {
          case Table ⇒
            div(overflow := "auto", height := "90%")({
              if (!dataTab.sequence.header.isEmpty && !filteredSequence.isEmpty) {
                val table =
                  scaladget.bootstrapnative.DataTable(
                    Some(scaladget.bootstrapnative.Table.Header(dataTab.sequence.header)),
                    filteredSequence.map {
                      scaladget.bootstrapnative.DataTable.DataRow(_)
                    }.toSeq,
                    scaladget.bootstrapnative.Table.BSTableStyle(bordered_table, emptyMod), true)
                table.render(width := dataTab.sequence.header.length * 90)
              }
              else div()
            }
            )
          case Raw ⇒ editorView
          case _ ⇒
            val (newPlotter, newSequenceData) = plotterAndSeqencedata(dataTab, plotter)
            Plotter.plot(
              SequenceData(newSequenceData.header, newSequenceData.content),
              newPlotter
            )
        }
      )
    }

  }

  def save(safePath: SafePath, editorPanelUI: EditorPanelUI, afterSave: () ⇒ Unit) =
    editorPanelUI.synchronized {
      Post()[Api].saveFile(safePath, editorPanelUI.code).call().foreach {
        _ ⇒
          afterSave()
      }
    }

  def rawBlock(htmlContent: String) =
    div(editorContainer +++ container)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(JsDom.RawFrag(htmlContent))
      )
    )

  def mdBlock(htmlContent: String) =
    div(editorContainer +++ container)(
      div(panelClass +++ panelDefault)(
        div(panelBody)(
          ms("mdRendering") +++ (padding := 10),
          JsDom.RawFrag(htmlContent)
        )
      )
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

case class DataTab(
  sequence: SequenceData,
  view:     EditableView,
  filter:   RowFilter,
  editing:  Boolean
)

object DataTab {
  def build(
    sequence: SequenceData,
    view:     EditableView = Raw,
    filter:   RowFilter    = First100,
    editing:  Boolean      = false) = DataTab(sequence, view, filter, editing)
}

class TreeNodeTabs {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val tabs: Var[Seq[TreeNodeTab]] = Var(Seq())
  val timer: Var[Option[SetIntervalHandle]] = Var(None)
  val temporaryControl: Var[TypedTag[HTMLElement]] = Var(div())

  def stopTimerIfNoTabs = {
    if (tabs.now.isEmpty) {
      timer.map {
        _.foreach {
          clearInterval
        }
      }
      timer() = None
    }
  }

  def startTimerIfStopped =
    timer.now match {
      case None ⇒
        timer() = Some(setInterval(15000) {
          tabs.now.foreach {
            _.refresh()
          }
        })
      case _ ⇒
    }

  def setActive(tab: TreeNodeTab) = {
    if (tabs.now.contains(tab)) {
      unActiveAll
    }
    tab.activate
  }

  def isActive(safePath: SafePath) =
    tabs.now.filter {
      _.safePathTab.now == safePath
    }.map {
      _.activity
    }.headOption.getOrElse(Var(TreeNodeTabs.UnActive))

  def unActiveAll = tabs.foreach {
    _.foreach { t ⇒ t.desactivate }
  }

  def add(tab: TreeNodeTab) = {
    tabs() = tabs.now :+ tab
    startTimerIfStopped
    setActive(tab)
  }

  def remove(tab: TreeNodeTab) = {
    tab.desactivate
    val newTabs = tabs.now.filterNot {
      _ == tab
    }
    tabs() = newTabs
    if (tabs.now.isEmpty) temporaryControl() = div()
    newTabs.lastOption.map { t ⇒ setActive(t) }.isDefined
  }

  def remove(safePath: SafePath): Unit = find(safePath).map {
    remove
  }

  def switchEditableTo(tab: TreeNodeTab, dataTab: DataTab, plotter: Plotter) = {
    val newTab = TreeNodeTab.Editable(this, tab.safePathTab.now, tab.editableContent.getOrElse(""), dataTab, plotter)
    switchTab(tab, newTab)
  }

  def switchTab(tab: TreeNodeTab, to: TreeNodeTab) = {
    val index = {
      val i = tabs.now.indexOf(tab)
      if (i == -1) tabs.now.size
      else i
    }

    remove(tab)
    tabs() = tabs.now.take(index) ++ Seq(to) ++ tabs.now.takeRight(tabs.now.size - index)

    setActive(to)
  }

  def alterables: Seq[AlterableFileContent] =
    tabs.now.flatMap { t ⇒ t.editableContent.map(c ⇒ AlterableFileContent(t.safePathTab.now, c)) }

  def saveAllTabs(onsave: () ⇒ Unit) = {
    //if(org.scalajs.dom.document.hasFocus())
    org.openmole.gui.client.core.Post()[Api].saveFiles(alterables).call().foreach { s ⇒ onsave() }
  }

  def checkTabs = tabs.now.foreach { t: TreeNodeTab ⇒
    org.openmole.gui.client.core.Post()[Api].exists(t.safePathTab.now).call().foreach { e ⇒
      if (!e) remove(t)
    }
  }

  def rename(sp: SafePath, newSafePath: SafePath) = {
    find(sp).map { tab ⇒
      tab.tabName() = newSafePath.name
      tab.safePathTab() = newSafePath
    }
  }

  def find(safePath: SafePath) = tabs.now.find { t ⇒
    t.safePathTab.now == safePath
  }

  implicit def modToModSeq(m: Modifier): ModifierSeq = Seq(m)

  def fontSizeLink(size: Int) = {
    div("A", fontSize := s"${size}px", pointer, padding := 3, onclick := { () ⇒
      for {
        ts ← tabs.now
        t ← ts.editor
      } yield {
        t.updateFont(size)
      }
    }
    )

  }

  //  def setErrors(path: SafePath, errors: Seq[ErrorWithLocation]) =
  //    find(path).foreach { tab ⇒ tab.editor.foreach { _.setErrors(errors) } }

  val fontSizeControl = div(display.flex, flexDirection.row, alignItems.baseline, justifyContent.flexEnd)(
    fontSizeLink(15),
    fontSizeLink(25),
    fontSizeLink(35)
  )

  val render = div(
    //Headers
    Rx {
      val tabList = ul(nav +++ navTabs, tab_list_role)(
        for (t ← tabs()) yield {
          li(
            omsheet.color("yellow"),
            marginTop := -30,
            presentation_role,
            `class` := {
              t.activity() match {
                case Active ⇒ "active"
                case _      ⇒ ""
              }
            }
          )(
              a(
                id := t.id,
                tab_role,
                pointer,
                t.activity() match {
                  case Active ⇒ activeTab
                  case _      ⇒ unActiveTab
                },
                data("toggle") := "tab", onclick := { () ⇒
                  t.editor.foreach {
                    _.editor.focus
                  }
                  setActive(t)
                }
              )(
                  button(
                    ms("close") +++ tabClose,
                    `type` := "button",
                    onclick := {
                      () ⇒
                        t.refresh(() ⇒ ()) // save
                        remove(t)
                    }
                  )(raw("&#215")),
                  t.tabName()
                )
            )
        }
      ).render

      val tabDiv =
        div(tabContent) {
          tabs() match {
            case t if t.isEmpty ⇒
              div(centerElement, marginTop := "40px", "To start, create and/or open a file with a .oms extension using the file panel on the left (ex: test.oms).")
            case tabs ⇒
              for { t ← tabs } yield {
                div(
                  role := "tabpanel",
                  ms("tab-pane " + {
                    t.activity() match {
                      case Active ⇒ "active"
                      case _      ⇒ ""
                    }
                  }), id := t.id
                )({
                    t.activity() match {
                      case Active ⇒
                        temporaryControl() = t.controlElement
                        t.block
                      case UnActive ⇒ div()
                    }
                  }
                  )
              }
          }
        }

      Sortable(
        tabList,
        SortableOptions.onEnd(
          (event: EventS) ⇒ {
            val oldI = event.oldIndex.asInstanceOf[Int]
            val newI = event.newIndex.asInstanceOf[Int]
            tabs() = tabs.now.updated(oldI, tabs.now(newI)).updated(newI, tabs.now(oldI))
            setActive(tabs.now(newI))
          }
        ))

      div(role := "tabpanel")(
        fontSizeControl,
        tabList,
        tabDiv
      )
    }
  )

}
