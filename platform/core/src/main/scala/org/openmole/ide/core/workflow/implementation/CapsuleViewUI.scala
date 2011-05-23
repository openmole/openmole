/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.provider.DnDTaskIntoCapsuleProvider
import org.openmole.ide.core.workflow.action.TaskActions
import org.openmole.ide.core.provider.CapsuleMenuProvider
import org.openmole.ide.core.workflow.implementation.paint.ConnectableWidget
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.workflow.implementation.paint.SamplingWidget
import org.openmole.ide.core.workflow.model.ICapsuleModelUI
import org.openmole.ide.core.workflow.model.ICapsuleView

class CapsuleViewUI(val scene: MoleScene,val capsuleModel: ICapsuleModelUI) extends Widget(scene) with ICapsuleView{

  createActions(scene.MOVE).addAction (ActionFactory.createMoveAction)
  
  val connectableWidget= new ConnectableWidget(scene,this)
  // val dnDAddPrototypeProvider= new DnDAddPrototypeProvider(scene, this)
  val dndTaskIntoCapsuleProvider = new DnDTaskIntoCapsuleProvider(scene, this)
  val capsuleMenuProvider= new CapsuleMenuProvider(scene, this)
  
  addChild(connectableWidget)
        
  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
  getActions.addAction(ActionFactory.createAcceptAction(dndTaskIntoCapsuleProvider))
  // getActions.addAction(ActionFactory.createAcceptAction(dnDAddPrototypeProvider))


  
  def defineStartingCapsule(on: Boolean){
    capsuleModel.defineStartingCapsule(on)
    connectableWidget.clearInputSlots
    connectableWidget.addInputSlot(new ISlotWidget(scene,this,1,on))
  }
  
  override def encapsule(taskUI: TaskUI)= {
    //   capsuleModel.setTaskModel(UIFactory.createTaskModelInstance(Preferences.model(MoleConcepts.TASK_INSTANCE,taskUI.entityType).getClass.asInstanceOf[Class[GenericTaskModelUI]]))

    //capsuleModel.setTaskUI(UIFactory.createTaskModelInstance(Preferences.model(MoleConcepts.TASK_INSTANCE,taskUI.entityType)).asInstanceOf[GenericTaskModelUI])
    capsuleModel.setTaskUI(taskUI)
    
    // changeConnectableWidget
    //  dnDAddPrototypeProvider.encapsulated= true
    dndTaskIntoCapsuleProvider.encapsulated= true
    capsuleMenuProvider.addTaskMenus
    getActions.addAction(new TaskActions(capsuleModel.taskUI.get, this))
  }
  
//  def changeConnectableWidget= {
//    connectableWidget.objectView.backgroundColor= backgroundColor
//    connectableWidget.setBorderCol(getBorderColor);
//    connectableWidget.setBackgroundImaqe(getBackgroundImage)
//    connectableWidget.setTaskModel(capsuleModel.getTaskModel)
//  }
  
  def addInputSlot: ISlotWidget =  {
    capsuleModel.addInputSlot
    val im = new ISlotWidget(scene, this,capsuleModel.nbInputSlots,capsuleModel.startingCapsule)
    connectableWidget.addInputSlot(im)
    scene.validate
    scene.refresh
    im
  }

}
      
//
//class CapsuleViewUI extends ObjectViewUI implements ICapsuleView {
//    protected ConnectableWidget connectableWidget;
//    private ICapsuleModelUI capsuleModel;
//    private DnDAddPrototypeProvider dnDAddPrototypeProvider;
//   // private DnDAddSamplingProvider dnDAddSamplingProvider;
//    private CapsuleMenuProvider taskCapsuleMenuProvider;
//
//
//    public CapsuleViewUI(MoleScene scene,
//            ICapsuleModelUI tcm,
//            Properties properties) {
//
//        super(scene, properties);
//        capsuleModel = tcm;
//
//        connectableWidget = new ConnectableWidget(scene,
//                capsuleModel,
//                getBackgroundColor(),
//                getBorderColor(),
//                getBackgroundImage());
//        setLayout(LayoutFactory.createVerticalFlowLayout());
//        addChild(connectableWidget);
//
//        //Default output slot
//        connectableWidget.addOutputSlot(new OSlotWidget(scene,this));
//
//        dnDAddPrototypeProvider = new DnDAddPrototypeProvider(scene, this);
//      //  dnDAddSamplingProvider = new DnDAddSamplingProvider(scene);
//
//        taskCapsuleMenuProvider = new CapsuleMenuProvider(scene, this);
//        getActions().addAction(ActionFactory.createPopupMenuAction(taskCapsuleMenuProvider));
//        getActions().addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(scene, this)));
//        getActions().addAction(ActionFactory.createAcceptAction(dnDAddPrototypeProvider));
//      //  getActions().addAction(ActionFactory.createAcceptAction(dnDAddSamplingProvider));
//    }
//    
//    public void defineAsRegularCapsule(){
//        capsuleModel.defineAsRegularCapsule();
//        connectableWidget.clearInputSlots();
//        connectableWidget.addInputSlot(new ISlotWidget(scene,this,1,false));
//    }
//
//    public void defineAsStartingCapsule(){
//        capsuleModel.defineAsStartingCapsule();
//        connectableWidget.clearInputSlots();
//        connectableWidget.addInputSlot(new ISlotWidget(scene,this,1,true));
//    }
//
//    @Override
//    public void encapsule(TaskUI taskUI) throws UserBadDataError {
//       // capsuleModel.setTaskModel(UIFactory.createTaskModelInstance((Class<? extends IGenericTaskModelUI>) Preferences.getInstance().getModel(CategoryName.TASK_INSTANCE, taskUI.getType()), taskUI));
//capsuleModel.setTaskModel(UIFactory.createTaskModelInstance( Preferences.getModel(CategoryName.TASK_INSTANCE, taskUI.getType()), taskUI));
//
//
//        properties = Preferences.getProperties(CategoryName.TASK_INSTANCE, taskUI.getType());
//
//        changeConnectableWidget();
//
//        dnDAddPrototypeProvider.setEncapsulated(true);
//
//        MoleScenesManager.getInstance().incrementNodeName();
//        connectableWidget.addTitle(taskUI.getName());
//
//        taskCapsuleMenuProvider.addTaskMenus();
//        getActions().addAction(new TaskActions(capsuleModel.getTaskModel(), this));
//    }
//
//    @Override
//    public void changeConnectableWidget() {
//        connectableWidget.setBackgroundCol(getBackgroundColor());
//        connectableWidget.setBorderCol(getBorderColor());
//        connectableWidget.setBackgroundImaqe(getBackgroundImage());
//        connectableWidget.setTaskModel(capsuleModel.getTaskModel());
//    }
//
//    @Override
//    public ISlotWidget addInputSlot() {
//        capsuleModel.addInputSlot();
//        ISlotWidget im = new ISlotWidget(scene, this,getCapsuleModel().getNbInputslots(),capsuleModel.isStartingCapsule() ? true : false);
//        getConnectableWidget().addInputSlot(im);
//        scene.refresh();
//        return im;
//    }
//
//    @Override
//    public ConnectableWidget getConnectableWidget() {
//        return connectableWidget;
//    }
//
//    @Override
//    public ICapsuleModelUI getCapsuleModel() {
//        return capsuleModel;
//    }
//
//    @Override
//    public IObjectModelUI getModel() {
//        return (IObjectModelUI) capsuleModel;
//    }
//
//    @Override
//    public MyWidget getWidget() {
//        return connectableWidget;
//    }
//}
