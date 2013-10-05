/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.execution

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Timer
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.core.implementation.execution.local._
import org.openmole.core.model.execution.Environment
import org.openmole.ide.misc.visualization._
import org.openmole.ide.misc.widget._
import org.openmole.core.model.mole._
import org.openmole.ide.core.implementation.dialog.StatusBar
import scala.collection.mutable.HashMap
import scala.swing._
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.core.model.job.State
import org.openmole.core.model.execution.ExecutionState
import org.openmole.ide.core.implementation.workflow.{ MoleUI, CapsuleUI, ExecutionMoleSceneContainer }
import org.openmole.ide.core.implementation.builder.MoleFactory
import util.{ Failure, Success }
import org.openmole.misc.exception.ExceptionUtils
import scala.concurrent.stm._
import java.net.URL

import org.openmole.core.model.execution.ExecutionState._
import org.openmole.web.misc.tools.OMClient
import org.openmole.ide.core.implementation.serializer.ExecutionSerialiser

object ExecutionManager {
  implicit def executionStatesDecorator(s: scala.collection.mutable.Map[ExecutionState.ExecutionState, AtomicInteger]) = new {
    def states = new States(s(ExecutionState.READY).get, s(ExecutionState.SUBMITTED).get, s(ExecutionState.RUNNING).get)
  }
}

class ExecutionManager(manager: MoleUI,
                       executionContainer: ExecutionMoleSceneContainer,
                       val mole: IMole,
                       val capsuleMapping: Map[CapsuleUI, ICapsule]) extends PluginPanel("", "[grow,fill]", "")
    with Publisher {
  executionManager ⇒
  val logTextArea = new TextArea
  logTextArea.columns = 20
  //logTextArea.rows = 20
  logTextArea.editable = false

  val executionJobExceptionTextArea = new StatusBar

  val moleExecutionExceptionTextArea = new StatusBar

  val printStream = new PrintStream(new TextAreaOutputStream(logTextArea), true)
  var moleExecution: Option[IMoleExecution] = None
  var status = HashMap(State.READY -> new AtomicInteger,
    State.RUNNING -> new AtomicInteger,
    State.COMPLETED -> new AtomicInteger,
    State.FAILED -> new AtomicInteger,
    State.CANCELED -> new AtomicInteger)

  val wfPiePlotter = new PiePlotter

  val titlePanel = new PluginPanel("wrap", "[center]", "")
  titlePanel.contents += new TitleLabel("Workflow")
  titlePanel.peer.add(wfPiePlotter.panel)

  val envBarPanel = new PluginPanel("", "[][grow,fill]", "[top]")
  envBarPanel.contents += titlePanel

  val environments = new HashMap[Environment, PlotState]
  val timer = new Timer(5000, timerAction)

  lazy val timerAction = new ActionListener {
    def actionPerformed(ae: ActionEvent) = {
      environments.foreach {
        e ⇒
          e._2.plotter.update(new States(e._2.statuses(READY).intValue, e._2.statuses(SUBMITTED).intValue, e._2.statuses(RUNNING).intValue))
      }
    }
  }

  val downloads = Ref((0, 0))
  var uploads = Ref((0, 0))

  val tabbedPane = new TabbedPane {
    opaque = true
    background = new Color(77, 77, 77)
  }
  tabbedPane.pages += new TabbedPane.Page("Progress", new ScrollPane(logTextArea) {
    verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
  })
  tabbedPane.pages += new TabbedPane.Page("Errors", new ScrollPane(executionJobExceptionTextArea) {
    verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
  })
  tabbedPane.pages += new TabbedPane.Page("Environments errors", new ScrollPane(moleExecutionExceptionTextArea) {
    verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded
  })

  contents += tabbedPane

  def start(server: Option[URL] = None,
            executionContext: ExecutionContext = ExecutionContext.local) = synchronized {
    tabbedPane.selection.index = 0
    cancel
    initBarPlotter

    server match {
      case Some(url: URL) ⇒
        val client = new OMClient(url)
        client.createMole(ExecutionSerialiser(manager, true), None)
      case _ ⇒
        buildMoleExecution match {
          case Success((mE, envNames)) ⇒
            val mExecution = mE.toExecution(manager.context, executionContext.copy(out = printStream))
            moleExecution = Some(mExecution)
            EventDispatcher.listen(mExecution: IMoleExecution, new JobSatusListener(this), classOf[IMoleExecution.JobStatusChanged])
            EventDispatcher.listen(mExecution: IMoleExecution, new JobSatusListener(this), classOf[IMoleExecution.Finished])
            EventDispatcher.listen(mExecution: IMoleExecution, new JobCreatedListener(this), classOf[IMoleExecution.JobCreated])
            EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.ExceptionRaised])
            EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.JobFailed])
            EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.HookExceptionRaised])
            EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.SourceExceptionRaised])
            envNames.foreach {
              case (e, _) ⇒
                e match {
                  case e: BatchEnvironment ⇒
                    EventDispatcher.listen(e, new UploadFileListener(this), classOf[BatchEnvironment.BeginUpload])
                    EventDispatcher.listen(e, new UploadFileListener(this), classOf[BatchEnvironment.EndUpload])
                    EventDispatcher.listen(e, new UploadFileListener(this), classOf[BatchEnvironment.BeginDownload])
                    EventDispatcher.listen(e, new UploadFileListener(this), classOf[BatchEnvironment.EndDownload])
                  case _ ⇒
                }
            }
            envNames.foreach {
              case (env, name) ⇒
                EventDispatcher.listen(env: Environment, new EnvironmentExceptionListener(this), classOf[Environment.ExceptionRaised])
                buildEmptyEnvPlotter(env, name)
            }

            if (envBarPanel.peer.getComponentCount == 2) envBarPanel.peer.remove(1)
            if (environments.size > 0) {
              envBarPanel.peer.add(new PluginPanel("wrap", "[center]", "") {
                contents += new TabbedPane {
                  environments.foreach {
                    e ⇒
                      pages += new TabbedPane.Page(e._2.name, new PluginPanel("") {
                        contents += e._2.plotter.panel
                      })
                  }
                }
              }.peer)
            }
            initPieChart
            repaint
            revalidate
            timer.start
            mExecution.start
          case Failure(e) ⇒
            executionJobExceptionTextArea.block(e.getMessage, None, ExceptionUtils.prettify(e))
            tabbedPane.selection.index = 1
            None
        }
    }
  }

  def buildMoleExecution = MoleFactory.buildMoleExecution(mole,
    manager,
    capsuleMapping)

  def incrementEnvironmentState(environment: Environment,
                                state: ExecutionState.ExecutionState) = synchronized {
    environments(environment).statuses(state).incrementAndGet
  }

  def decrementEnvironmentState(environment: Environment,
                                state: ExecutionState.ExecutionState) = synchronized {
    environments(environment).statuses(state).decrementAndGet
  }

  def cancel = synchronized {
    timer.stop
    moleExecution match {
      case Some(mE: IMoleExecution) ⇒ mE.cancel
      case _                        ⇒
    }
  }

  def initBarPlotter = synchronized {
    environments.clear
    buildEmptyEnvPlotter(LocalEnvironment.asInstanceOf[Environment], "Local")
  }

  def buildEmptyEnvPlotter(env: Environment, name: String) = {
    val m = HashMap(ExecutionState.SUBMITTED -> new AtomicInteger,
      ExecutionState.READY -> new AtomicInteger,
      ExecutionState.RUNNING -> new AtomicInteger,
      ExecutionState.DONE -> new AtomicInteger,
      ExecutionState.FAILED -> new AtomicInteger,
      ExecutionState.KILLED -> new AtomicInteger)
    environments += env -> new PlotState(name, m)

    moleExecution match {
      case Some(mE: IMoleExecution) ⇒
        EventDispatcher.listen(env, new JobStateChangedOnEnvironmentListener(this, mE, env), classOf[Environment.JobStateChanged])
      case _ ⇒
    }
  }

  def initPieChart = synchronized {
    status.keys.foreach(k ⇒ status(k) = new AtomicInteger)
    environments.values.foreach(env ⇒ env.statuses.keys.foreach(k ⇒ env.statuses(k) = new AtomicInteger))
  }

  def displayFileTransfer = atomic {
    implicit ctx ⇒
      executionContainer.updateFileTransferLabels(downloads()._1 + " / " + downloads()._2,
        uploads()._1 + " / " + uploads()._2)
  }

  case class PlotState(val name: String,
                       val statuses: HashMap[ExecutionState.ExecutionState, AtomicInteger],
                       val plotter: XYPlotter = new XYPlotter(5000, 120))

}
