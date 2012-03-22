/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Timer
import org.openide.DialogDescriptor
import org.openide.DialogDisplayer
import org.openide.NotifyDescriptor
import org.openide.awt.StatusDisplayer
import org.openide.util.Lookup
import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.core.implementation.mole.MoleExecution
import org.openmole.core.model.execution.IEnvironment
import org.openmole.core.model.hook.IHook
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IGroupingStrategy
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.misc.visualization._
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.core.model.mole.IMole
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.implementation.serializer.MoleMaker
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.workflow.IMoleSceneManager
import scala.collection.mutable.HashMap
import scala.swing.Action
import scala.swing.Label
import scala.swing.Menu
import scala.swing.MenuBar
import scala.swing.MenuItem
import scala.swing.Orientation
import scala.swing.ScrollPane
import scala.swing.Separator
import scala.swing.SplitPane
import scala.swing.TabbedPane
import org.openmole.misc.eventdispatcher.EventDispatcher
import org.openmole.misc.workspace.Workspace
import scala.collection.JavaConversions._
import scala.swing.TextArea
import org.openmole.core.model.job.State
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.execution.ExecutionState
import org.openmole.ide.core.model.workflow.ICapsuleUI
import TextAreaOutputStream._


object ExecutionManager {
  implicit def executionStatesDecorator(s: scala.collection.mutable.Map[ExecutionState.ExecutionState,AtomicInteger]) = new {
    def states = new States(s(ExecutionState.READY).get, s(ExecutionState.SUBMITTED).get, s(ExecutionState.RUNNING).get)
  }
}

class ExecutionManager(manager : IMoleSceneManager,
                       val mole: IMole,
                       val capsuleMapping: Map[ICapsuleUI, ICapsule],
                       val prototypeMapping: Map[IPrototypeDataProxyUI,IPrototype[_]]) extends TabbedPane with IExecutionManager {
  val logTextArea = new TextArea{columns = 20;rows = 10;editable = false}
  val executionJobExceptionTextArea = new TextArea{columns = 40;rows = 10;editable = false}
  val moleExecutionExceptionTextArea = new TextArea{columns = 40;rows = 10;editable = false}
  override val printStream = new PrintStream(new TextAreaOutputStream(logTextArea),true)
  // override val (mole, capsuleMapping, prototypeMapping) = MoleMaker.buildMole(manager)
  var moleExecution: IMoleExecution = new MoleExecution(mole)
  var gStrategyPanels= new HashMap[String,(IGroupingStrategyPanelUI,List[(IGroupingStrategy,ICapsule)])]
  val hookPanels= new HashMap[String, (IHookPanelUI, List[IHook])]
  var status = HashMap(State.READY-> new AtomicInteger,
                       State.RUNNING-> new AtomicInteger,
                       State.COMPLETED-> new AtomicInteger,
                       State.FAILED-> new AtomicInteger,
                       State.CANCELED-> new AtomicInteger)
  
  val wfPiePlotter = new PiePlotter("Workflow execution")
  val envBarPanel = new PluginPanel("","[][grow,fill]",""){
    peer.add(wfPiePlotter.panel)
    preferredSize = new Dimension(250,250)}
  val envBarPlotter = new XYPlotter("Environment",5000,120) {preferredSize = new Dimension(400,250)}
  
  var states = new States(0,0,0)
  val timer = new Timer(5000, new ActionListener {
      def actionPerformed(e: ActionEvent) = {
        envBarPlotter.update(states)
      }
    })
  var environments = new HashMap[IEnvironment,(String,HashMap[ExecutionState.ExecutionState,AtomicInteger])] 
  
  val hookMenu = new Menu("Hooks")
  val groupingMenu = new Menu("Grouping")
  Lookup.getDefault.lookupAll(classOf[IHookFactoryUI]).foreach{f=>hookMenu.contents+= new MenuItem(new AddHookRowAction(f))}
  Lookup.getDefault.lookupAll(classOf[IGroupingStrategyFactoryUI]).foreach{f=>groupingMenu.contents+= new MenuItem(new AddGroupingStrategyRowAction(f))}
  val menuBar = new MenuBar{contents.append(hookMenu,groupingMenu)}
  menuBar.minimumSize = new Dimension(menuBar.size.width,30)
  val hookPanel = new PluginPanel(""){contents+= (menuBar,"wrap")}
  
  val splitPane = new SplitPane(Orientation.Vertical) {
    leftComponent = new ScrollPane(envBarPanel)
    rightComponent = new ScrollPane(logTextArea)
    resizeWeight = 0.6
  }
  
  System.setOut(new PrintStream(logTextArea.toStream))
  System.setErr(new PrintStream(logTextArea.toStream))
  
  pages+= new TabbedPane.Page("Settings",hookPanel)
  pages+= new TabbedPane.Page("Execution progress", splitPane)
  pages+= new TabbedPane.Page("Execution errors", new ScrollPane(executionJobExceptionTextArea))
  pages+= new TabbedPane.Page("Environments errors", new ScrollPane(moleExecutionExceptionTextArea))
  
  def canBeRun = 
    if(Workspace.anotherIsRunningAt(Workspace.defaultLocation)) {
      val dd = new DialogDescriptor(new Label("A simulation is currently running.\nTwo simulations can not run concurrently, overwrite ?")
                                    {background = Color.white}.peer,
                                    "Execution warning")
      val result = DialogDisplayer.getDefault.notify(dd)
      if (result.equals(NotifyDescriptor.OK_OPTION)) {
        (new File(Workspace.defaultLocation.getAbsolutePath + "/.running")).delete
        true
      } else false
    } else true
  
  def start = synchronized {
    if (canBeRun){
      cancel
      initBarPlotter
      hookPanels.values.foreach(_._2.foreach(_.release))
      val (moleExecution, environments) = MoleMaker.buildMoleExecution(mole, 
                                                                       manager, 
                                                                       capsuleMapping,
                                                                       gStrategyPanels.values.map{v=>v._1.saveContent.map(_.coreObject)}.flatten.toList)

      this.moleExecution = moleExecution
      
      EventDispatcher.listen(moleExecution,new JobSatusListener(this),classOf[IMoleExecution.OneJobStatusChanged])
      EventDispatcher.listen(moleExecution,new JobCreatedListener(this),classOf[IMoleExecution.OneJobSubmitted])
      EventDispatcher.listen(moleExecution,new ExecutionExceptionListener(this),classOf[IMoleExecution.ExceptionRaised])
      
      environments.foreach {
        case(env, _) => EventDispatcher.listen(env, new EnvironmentExceptionListener(this),classOf[IEnvironment.ExceptionRaised])
      }

      environments.foreach(buildEmptyEnvPlotter)
      if(envBarPanel.peer.getComponentCount == 2) envBarPanel.peer.remove(1)
      
      //FIXME Displays several environments
      if (environments.size > 0) {
        envBarPlotter.title(environments.toList(0)._2)
        envBarPanel.peer.add(envBarPlotter.panel) 
      }
      initPieChart
      hookPanels.keys.foreach{commitHook}
      repaint 
      revalidate
    
      timer.start
      moleExecution.start
    }
  }
    
  def incrementEnvironmentState(environment: IEnvironment,
                                state: ExecutionState.ExecutionState) = synchronized {
    states = States.factory(states, state, environments(environment)._2(state).incrementAndGet)
  }
  
  def decrementEnvironmentState(environment: IEnvironment,
                                state: ExecutionState.ExecutionState) = synchronized {
    states = States.factory(states, state, environments(environment)._2(state).decrementAndGet)
  }
  
  def cancel = synchronized { 
    timer.stop
    moleExecution.cancel 
  }
  
  def initBarPlotter = synchronized{
    environments.clear
    buildEmptyEnvPlotter((LocalExecutionEnvironment.asInstanceOf[IEnvironment],"Local"))
  }

  def buildEmptyEnvPlotter(e: (IEnvironment,String)) = {
    val m = HashMap(ExecutionState.SUBMITTED-> new AtomicInteger,
                    ExecutionState.READY-> new AtomicInteger,
                    ExecutionState.RUNNING-> new AtomicInteger,
                    ExecutionState.DONE-> new AtomicInteger,
                    ExecutionState.FAILED->new AtomicInteger,
                    ExecutionState.KILLED-> new AtomicInteger)    
    environments+= e._1-> (e._2,m)
    EventDispatcher.listen(e._1,new JobStateChangedOnEnvironmentListener(this,moleExecution,e._1),classOf[IEnvironment.JobStateChanged])
  }
  
  
  override def commitHook(hookClassName: String) { 
    if (hookPanels.contains(hookClassName)) hookPanels(hookClassName)._2.foreach(_.release)
    hookPanels(hookClassName) = (hookPanels(hookClassName)._1,hookPanels(hookClassName)._1.saveContent.map(_.coreObject))
  }
  
  def initPieChart = synchronized{
    status.keys.foreach(k=>status(k)=new AtomicInteger)
    environments.values.foreach(env=>env._2.keys.foreach(k=> env._2(k) = new AtomicInteger))
  }
    
  class AddHookRowAction(fui: IHookFactoryUI) extends Action(fui.toString){
    def apply = {
      val cl = fui.coreClass.getCanonicalName
      if(hookPanels.contains(cl)) hookPanels(cl)._1.addHook
      else {
        val pui = fui.buildPanelUI(ExecutionManager.this)
        hookPanel.peer.add(pui.peer)
        hookPanel.peer.add((new Separator).peer)
        hookPanels+= cl -> (pui, List.empty)
      }
      hookPanels+= cl -> (hookPanels(cl)._1,hookPanels(cl)._1.saveContent.map(_.coreObject))
    }
  }
  
  class AddGroupingStrategyRowAction(fui: IGroupingStrategyFactoryUI) extends Action(fui.toString){
    def apply = {
      val cl = fui.coreClass.getCanonicalName
      if(gStrategyPanels.contains(cl)) 
        gStrategyPanels(cl)._1.addStrategy
      else {
        val pui = fui.buildPanelUI(ExecutionManager.this)
        hookPanel.peer.add(pui.peer)
        gStrategyPanels+= cl-> (pui,List.empty)
      }
      gStrategyPanels+= cl-> (gStrategyPanels(cl)._1,gStrategyPanels(cl)._1.saveContent.map(_.coreObject))
    }
  }
}
