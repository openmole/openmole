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
import org.openmole.core.model.data._
import org.openmole.core.model.execution.ExecutionState
import org.openmole.ide.core.implementation.workflow.{ IMoleUI, CapsuleUI, ExecutionMoleSceneContainer }
import org.openmole.ide.core.implementation.builder.MoleFactory
import util.{ Failure, Success }
import org.openmole.misc.exception.ExceptionUtils
import scala.concurrent.stm._

object ExecutionManager {
  implicit def executionStatesDecorator(s: scala.collection.mutable.Map[ExecutionState.ExecutionState, AtomicInteger]) = new {
    def states = new States(s(ExecutionState.READY).get, s(ExecutionState.SUBMITTED).get, s(ExecutionState.RUNNING).get)
  }
}

class ExecutionManager(manager: IMoleUI,
                       executionContainer: ExecutionMoleSceneContainer,
                       val mole: IMole,
                       val capsuleMapping: Map[CapsuleUI, ICapsule]) extends PluginPanel("", "[grow,fill]", "")
    with IExecutionManager
    with Publisher {
  executionManager ⇒
  val logTextArea = new TextArea
  logTextArea.columns = 20
  //logTextArea.rows = 20
  logTextArea.editable = false

  val executionJobExceptionTextArea = new StatusBar

  val moleExecutionExceptionTextArea = new StatusBar

  override val printStream = new PrintStream(new TextAreaOutputStream(logTextArea), true)
  var moleExecution: Option[IMoleExecution] = None
  var status = HashMap(State.READY -> new AtomicInteger,
    State.RUNNING -> new AtomicInteger,
    State.COMPLETED -> new AtomicInteger,
    State.FAILED -> new AtomicInteger,
    State.CANCELED -> new AtomicInteger)

  //var hooksInExecution = List.empty[IHook]
  val wfPiePlotter = new PiePlotter
  val envBarPlotter = new XYPlotter(5000, 120)

  val titlePanel = new PluginPanel("wrap", "[center]", "")
  titlePanel.contents += new TitleLabel("Workflow")
  titlePanel.peer.add(wfPiePlotter.panel)

  val envBarPanel = new PluginPanel("", "[][grow,fill]", "[top]")
  envBarPanel.contents += titlePanel

  var states = new States(0, 0, 0)
  val timerAction = new ActionListener {
    def actionPerformed(e: ActionEvent) = {
      envBarPlotter.update(states)
    }
  }

  val timer = new Timer(5000, timerAction)
  var environments = new HashMap[Environment, (String, HashMap[ExecutionState.ExecutionState, AtomicInteger])]

  val downloads = Ref((0, 0))
  var uploads = Ref((0, 0))

  val tabbedPane = new TabbedPane {
    opaque = true
    background = new Color(77, 77, 77)
  }
  tabbedPane.pages += new TabbedPane.Page("Progress", new ScrollPane(logTextArea) { verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded })
  tabbedPane.pages += new TabbedPane.Page("Errors", new ScrollPane(executionJobExceptionTextArea) { verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded })
  tabbedPane.pages += new TabbedPane.Page("Environments errors", new ScrollPane(moleExecutionExceptionTextArea) { verticalScrollBarPolicy = ScrollPane.BarPolicy.AsNeeded })

  contents += tabbedPane

  def start = synchronized {
    tabbedPane.selection.index = 0
    cancel
    initBarPlotter

    buildMoleExecution match {
      case Success((mE, envNames)) ⇒
        val mExecution = mE.toExecution(Context.empty, ExecutionContext.local.copy(out = printStream))
        moleExecution = Some(mExecution)
        EventDispatcher.listen(mExecution: IMoleExecution, new JobSatusListener(this), classOf[IMoleExecution.JobStatusChanged])
        EventDispatcher.listen(mExecution: IMoleExecution, new JobSatusListener(this), classOf[IMoleExecution.Finished])
        EventDispatcher.listen(mExecution: IMoleExecution, new JobCreatedListener(this), classOf[IMoleExecution.JobCreated])
        EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.ExceptionRaised])
        EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.JobFailed])
        EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.HookExceptionRaised])
        EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.SourceExceptionRaised])
        EventDispatcher.listen(mExecution: IMoleExecution, new ExecutionExceptionListener(this), classOf[IMoleExecution.ProfilerExceptionRaised])
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

        //FIXME Displays several environments
        if (environments.size > 0) {
          envBarPanel.peer.add(new PluginPanel("wrap", "[center]", "") {
            contents += new TitleLabel("Environment: " + environments.toList(0)._2._1)
            contents += envBarPlotter.panel
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

  def buildMoleExecution = MoleFactory.buildMoleExecution(mole,
    manager,
    capsuleMapping)

  def incrementEnvironmentState(environment: Environment,
                                state: ExecutionState.ExecutionState) = synchronized {
    states = States.factory(states, state, environments(environment)._2(state).incrementAndGet)
  }

  def decrementEnvironmentState(environment: Environment,
                                state: ExecutionState.ExecutionState) = synchronized {
    states = States.factory(states, state, environments(environment)._2(state).decrementAndGet)
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
    environments += env -> (name, m)

    moleExecution match {
      case Some(mE: IMoleExecution) ⇒
        EventDispatcher.listen(env, new JobStateChangedOnEnvironmentListener(this, mE, env), classOf[Environment.JobStateChanged])
      case _ ⇒
    }
  }

  def initPieChart = synchronized {
    status.keys.foreach(k ⇒ status(k) = new AtomicInteger)
    environments.values.foreach(env ⇒ env._2.keys.foreach(k ⇒ env._2(k) = new AtomicInteger))
  }

  def displayFileTransfer = atomic { implicit ctx ⇒
    executionContainer.updateFileTransferLabels(downloads()._1 + " / " + downloads()._2,
      uploads()._1 + " / " + uploads()._2)
  }

}
