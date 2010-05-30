/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ui.example;

import java.math.BigDecimal;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.capsule.TaskCapsule;
import org.openmole.core.workflow.methods.task.JavaTask;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.implementation.mole.Mole;
import org.openmole.core.implementation.transition.TransitionFactory;
import org.openmole.core.implementation.task.ExplorationTask;
import org.openmole.plugin.task.groovy.GroovyTask;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.model.transition.ITransition;
import org.openmole.core.model.transition.ITransitionSlot;

/**
 *
 * @author leclaire
 */
public class MoleExample {

    static public IMole buildMole() throws UserBadDataError, InternalProcessingError, InterruptedException, InstantiationException {
        JavaTask t1 = new JavaTask("t1");
        ExplorationTask t2 = new ExplorationTask("expl");
        JavaTask t3 = new JavaTask("t3");
        JavaTask t4 = new JavaTask("t4");
        JavaTask t5 = new JavaTask("t5");
        JavaTask t6 = new JavaTask("t6");
        JavaTask t7 = new JavaTask("t7");
        JavaTask t8 = new JavaTask("t8");
        JavaTask t9 = new JavaTask("t9");
        JavaTask t10 = new JavaTask("t10");
        JavaTask t11 = new JavaTask("t11");
        JavaTask t12 = new JavaTask("t12");
        JavaTask t13 = new JavaTask("t13");

        Prototype<BigDecimal> i = new Prototype<BigDecimal>("i", BigDecimal.class);
        t2.addInput(i, true);
        t2.addOutput(i, true);

        TaskCapsule t1Caps = new TaskCapsule(t1);
        ExplorationTaskCapsule t2Caps = new ExplorationTaskCapsule(t2);
        TaskCapsule t3Caps = new TaskCapsule(t3);
        TaskCapsule t4Caps = new TaskCapsule(t4);
        TaskCapsule t5Caps = new TaskCapsule(t5);
        TaskCapsule t6Caps = new TaskCapsule();
        TaskCapsule t7Caps = new TaskCapsule();
        TaskCapsule t8Caps = new TaskCapsule(t8);
        TaskCapsule t9Caps = new TaskCapsule(t9);
        TaskCapsule t10Caps = new TaskCapsule(t10);
        TaskCapsule t11Caps = new TaskCapsule(t11);
        TaskCapsule t12Caps = new TaskCapsule(t12);
        TaskCapsule t13Caps = new TaskCapsule(t13);

       /*     TransitionFactory.buildDiamond(TransitionFactory.buildChain(TransitionFactory.build(t1Caps),
        TransitionFactory.buildBranch(TransitionFactory.buildBranch(t9Caps,t11Caps),
        TransitionFactory.buildChain(t12Caps, t13Caps))),
        TransitionFactory.buildDiamond(t3Caps, t5Caps, t6Caps),
        TransitionFactory.build(t7Caps),
        TransitionFactory.buildChain(t4Caps, t8Caps),
        TransitionFactory.build(t10Caps));*/
/*
             TransitionFactory.buildDiamond(TransitionFactory.build(t1Caps),
        TransitionFactory.buildChain(t7Caps,t12Caps),
        TransitionFactory.buildChain(t3Caps,t8Caps),
        TransitionFactory.buildChain(t4Caps,t9Caps),
        TransitionFactory.buildChain(t5Caps,t10Caps),
        TransitionFactory.buildChain(t6Caps,t11Caps),
        TransitionFactory.build(t13Caps));*/

        /*   TransitionFactory.buildChain(TransitionFactory.build(t1Caps),
        TransitionFactory.buildExploration(t2Caps,
        TransitionFactory.buildChain(t3Caps,t4Caps),
        t5Caps));*/
       // TransitionFactory.buildDiamond(t1Caps,t3Caps,t4Caps,t6Caps,t5Caps);
          TransitionFactory.buildChain(t1Caps,t7Caps,t12Caps);
        IMole mole = new Mole(t1Caps);

       //   printWorkflow(mole);
        return mole;
    }

    static public IMole buildMole2() throws UserBadDataError, InternalProcessingError, InterruptedException, InstantiationException {
        JavaTask tt1 = new JavaTask("tt1");
        ExplorationTask tt2 = new ExplorationTask("texpl");
        GroovyTask tt3 = new GroovyTask("tt3");
        JavaTask tt4 = new JavaTask("ts4");
        JavaTask tt5 = new JavaTask("tt5");
        
        TaskCapsule tt1Caps = new TaskCapsule(tt1);
        ExplorationTaskCapsule tt2Caps = new ExplorationTaskCapsule(tt2);
        TaskCapsule tt5Caps = new TaskCapsule(tt5);

        TransitionFactory.buildExploration(tt2Caps,
                                           TransitionFactory.build(tt5Caps));
        IMole mole = new Mole(tt2Caps);

      //  printWorkflow(mole);
        return mole;
    }

    static void printWorkflow(IMole wf) throws InternalProcessingError, UserBadDataError {
        wf.visit(new IVisitor<IGenericTaskCapsule>() {

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError {
                System.out.println("TASK :" + visited.getTask().getName());
                Iterable<ITransitionSlot> trgrcoll = visited.getIntputTransitionsSlots();
                for (ITransitionSlot tgroup : trgrcoll) {
                    for (ITransition transition : tgroup.getTransitions()) {
                        System.out.println("Transition from " + transition.getStart().getTask().getName() + " to " + transition.getEnd().getCapsule().getTask().getName());
                    }
                }
                System.out.println();
            }
        });
    }
}
