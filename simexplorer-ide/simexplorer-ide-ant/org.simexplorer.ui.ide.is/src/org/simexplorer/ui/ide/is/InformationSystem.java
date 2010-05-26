/*
 *  Copyright (c) 2008, 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.ui.ide.is;

import com.healthmarketscience.rmiio.SerializableInputStream;
import fr.cemagref.simexplorer.is.entities.attachment.Attachment;
import fr.cemagref.simexplorer.is.entities.composite.Attachments;
import fr.cemagref.simexplorer.is.entities.composite.Codes;
import fr.cemagref.simexplorer.is.entities.composite.Components;
import fr.cemagref.simexplorer.is.entities.composite.ConstantValues;
import fr.cemagref.simexplorer.is.entities.composite.Constants;
import fr.cemagref.simexplorer.is.entities.composite.Descriptors;
import fr.cemagref.simexplorer.is.entities.composite.ExplorationDatas;
import fr.cemagref.simexplorer.is.entities.composite.Libraries;
import fr.cemagref.simexplorer.is.entities.composite.Structures;
import fr.cemagref.simexplorer.is.entities.data.Code;
import fr.cemagref.simexplorer.is.entities.data.Component;
import fr.cemagref.simexplorer.is.entities.data.Constant;
import fr.cemagref.simexplorer.is.entities.data.RuntimeType;
import fr.cemagref.simexplorer.is.entities.data.Descriptor;
import fr.cemagref.simexplorer.is.entities.data.ExplorationApplication;
import fr.cemagref.simexplorer.is.entities.data.ExplorationComponent;
import fr.cemagref.simexplorer.is.entities.data.ExplorationData;
import fr.cemagref.simexplorer.is.entities.data.LoggableElement;
import fr.cemagref.simexplorer.is.entities.data.Result;
import fr.cemagref.simexplorer.is.entities.data.Structure;
import fr.cemagref.simexplorer.is.entities.metadata.MetaData;
import fr.cemagref.simexplorer.is.exceptions.SimExplorerException;
import fr.cemagref.simexplorer.is.factories.LoggableElementFactory;
import fr.cemagref.simexplorer.is.service.StorageService;
import fr.cemagref.simexplorer.is.ui.swing.SimExplorer;
import fr.cemagref.simexplorer.is.ui.swing.actions.util.SimExplorerBaseAction;
import fr.cemagref.simexplorer.is.ui.swing.ui.JListTab;
import fr.cemagref.simexplorer.is.ui.swing.ui.SimExplorerMainUI;
import fr.cemagref.simexplorer.is.ui.swing.ui.SimExplorerTab;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTable;
import org.openmole.core.execution.structuregenerator.ComplexNode;
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.model.data.SequenceNode;
import org.openmole.core.workflow.model.data.StructureNode;
import org.openmole.core.workflow.implementation.task.Task;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.plan.ExperimentalDesign;
import org.openmole.core.workflow.implementation.plan.Factor;
import org.openmole.core.workflow.implementation.task.ExplorationTask;
import org.simexplorer.core.commons.tools.Instanciator;
import org.openmole.core.workflow.implementation.plan.Domain;
import org.openmole.core.workflow.implementation.task.GenericTask;
import org.openmole.core.workflow.model.metada.Metadata;
import org.openmole.core.workflow.model.plan.IFactor;
import org.openmole.core.workflow.model.plan.IPlan;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;
import org.simexplorer.ui.ide.workflow.model.TasksList;

/**
 * <p>This class serves as an interface between the SimExplorer Runtime objects
 *  and the information system.</p>
 *
 * <p>Rules for the transaction to store an Application from the IDE to an
 *  ExplorationApplication (EA) in the IS:<ul>
 *  <li>Root processor is skipped, and its children are directly added to the
 *   EA components.</li>
 *  <li>User variables are stored in a Structure container attached to the first
 *   component of the EA. This structure is named with the constant
 *   {@link InformationSystem#VARIABLES_NAME}</li>
 *  <li>For each component :</li><ul>
 *   <li>Codes are extracted from attributes and methods annoted with
 *    {@link org.openmole.core.workflow.model.task.annotations.Code}.</li>
 *   <li>Constants are extracted from attributes and methods annoted with
 *    {@link org.openmole.core.workflow.model.task.annotations.Constant}</li>
 *   <li>Methods that should be used as codes and constants should be designed
 *    as getters, so without any arguments.</li>
 *   <li>In case of an exploration task, an {@link ExplorationComponent} is
 *    created instead of a simple Component. The method is stored in an
 *    associated RuntimeType object. The constants and the codes of the method
 *    are stored in the ExplorationComponent itself.</li></ul>
 * </ul></p>
 */
public class InformationSystem implements ExplorationApplicationLoader, ExplorationApplicationSaver {

    public static final String VARIABLES_NAME = "variables";
    private static final Logger LOGGER = Logger.getLogger(InformationSystem.class.getName());
    private static final String SEQUENCE_STRUCTURE_SUFFIX = " []";
    private static final String INPUT_DATA_STRUCTURE_IS_TYPE_NAME = "input";
    private static final String OUTPUT_DATA_STRUCTURE_IS_TYPE_NAME = "output";
    private static final String ROLE_KEYS = "role";
    private static final String FACTOR_TYPE_KEY = "type";
    private StorageService storageService;
    private String token;
    private Map<org.simexplorer.ui.ide.workflow.model.ExplorationApplication, MetaData> applicationsMetaDataMap;
    private static InformationSystem instance;

    public InformationSystem() {
        SimExplorer.init();
        storageService = SimExplorer.getContext().getStorageService(false);
        token = SimExplorer.getContext().getToken();
        applicationsMetaDataMap = new HashMap<org.simexplorer.ui.ide.workflow.model.ExplorationApplication, MetaData>();
    }

    public static InformationSystem getInstance() {
        if (instance == null) {
            instance = new InformationSystem();
        }
        return instance;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public String getToken() {
        return token;
    }

    /**
     * Init the attributes of a component to prevent NullPointerExceptions in the database manager
     * @param component the component to init
     * @return the component
     */
    public static Component initComponent(Component component) {
        initLoggableElement(component);
        component.setCodes(new Codes());
        component.setConstants(new Constants());
        component.setStructures(new Structures());
        component.setLibraries(new Libraries());
        component.setSubComponents(new Components());
        return component;
    }

    /**
     * Init the attributes of a loggable element to prevent NullPointerExceptions in the database manager
     * @param <T> the type of loggable element
     * @param element the element to init
     * @return the element
     */
    public static <T extends LoggableElement> T initLoggableElement(T element) {
        element.setDescription("");
        element.setAttachments(new Attachments());
        element.setDescriptors(new Descriptors());
        return element;
    }

    /**
     * Return a list of all fields annoted and their instance value
     * @param object The object to process
     * @param annotation The annotation use to select field
     * @return
     * @throws org.simexplorer.exception.InternalProcessingError
     */
    public static List<Method> getAllTaggedMethod(Object object, Class<? extends Annotation> annotation) throws InternalProcessingError {
        List<Method> result = new ArrayList<Method>();
        Class cur = object.getClass();
        while (cur != null) {
            for (Method m : cur.getDeclaredMethods()) {
                if (m.isAnnotationPresent(annotation)) {
                    result.add(m);
                }
            }
            cur = cur.getSuperclass();
        }
        return result;
    }

    /**
     * Build a dialog box that list all the application stored in the IS
     * @param remote true if it is the application stored on the remote IS that are concerned, false for the local IS.
     * @return the dialog box
     */
    public static OpenFromISDialog getOpenDialog(boolean remote) {
        getInstance();
        // getting the IS local tab
        SimExplorerMainUI mainUI = SimExplorerMainUI.getUI();
        mainUI.getToggleTab_local().doClick();
        ((SimExplorerBaseAction) mainUI.getConnectAction(false)).updateUI();
        JListTab jListTab = SimExplorerTab.getListUI(remote);
        JTable jTable = jListTab.getTable();
        // and putting the list in the dialog box
        return new OpenFromISDialog(null, jTable, InformationSystem.getInstance());
    }

    public static JFrame getSyncDialog() {
        getInstance();
        SimExplorerMainUI mainUI = SimExplorerMainUI.getUI();
        // show local tab
        mainUI.getToggleTab_local().doClick();
        return mainUI;
    }

    /**
     * Save an exploration application in the information system.
     * @param explorationApplication
     * @return the metadata associated to the application stored in the information system
     * @throws org.simexplorer.exception.InternalProcessingError
     */
    @Override
    public MetaData saveExplorationApplication(org.simexplorer.ui.ide.workflow.model.ExplorationApplication explorationApplication) throws InternalProcessingError {
        MetaData result = null;
        ExplorationApplication loggedApplication = initLoggableElement(new ExplorationApplication());
        // processors : we skip the root processors list, and copy only its metadata
        syncProcessorToISLoggableElement(explorationApplication.getTreeRoot(), loggedApplication);
        loggedApplication.setName(explorationApplication.getName());
        loggedApplication.setComponents(new Components());
        ExplorationData explorationData = new ExplorationData();
        explorationData.setName("Default");
        initLoggableElement(explorationData);
        explorationData.setResult(new Result());
        explorationData.setConstantValues(new ConstantValues());
        loggedApplication.setExplorations(new ExplorationDatas());
        loggedApplication.getExplorations().add(explorationData);
        // Copy processors
        for (GenericTask subProcessor : (explorationApplication.getTreeRoot()).getChildren()) {
            loggedApplication.getComponents().add(syncProcessorToISComponent(explorationApplication, subProcessor, explorationData));
        }
        // Variables
        Structure variables = new Structure();
        variables.setName(VARIABLES_NAME);
        for (Prototype var : explorationApplication.getVariablesContracts()) {
            if (!org.simexplorer.ui.ide.workflow.model.ExplorationApplication.isVariableSystem(var)) {
                Structure isVar = new Structure();
                isVar.setName(var.getName());
                isVar.setRuntimeType(new RuntimeType(var.getType()));
                variables.getSubStructures().add(isVar);
                isVar.setDescriptors(toDescriptors(var.getMetadata()));
            }
        }
        loggedApplication.getComponents().get(0).getStructures().add(variables);
        // TODO libraries
        // Save
        try {
            SerializableInputStream serializableInputStream = new SerializableInputStream(LoggableElementFactory.getStream(loggedApplication));
            result = storageService.saveElement(token, serializableInputStream, new HashMap<Attachment, SerializableInputStream>()).getMetaData();
        } catch (IOException ex) {
            throw new InternalProcessingError(ex);
        } catch (SimExplorerException ex) {
            throw new InternalProcessingError(ex);
        }
        applicationsMetaDataMap.put(explorationApplication, result);
        return result;
    }

    private void syncProcessorToISLoggableElement(GenericTask processor, LoggableElement loggableElement) {
        loggableElement.setName(processor.getName());
        if (MetadataProxy.getDescription(processor) != null) {
            loggableElement.setDescription(MetadataProxy.getDescription(processor));
        }
        loggableElement.setDescriptors(toDescriptors(MetadataProxy.getMetadata(processor)));
    }

    private Descriptors toDescriptors(Metadata metadata) {
        Descriptors descriptors = new Descriptors();
        for (Entry<String, String> md : metadata.entrySet()) {
            descriptors.add(new Descriptor(md.getKey(), md.getValue()));
        }
        return descriptors;
    }

    private void syncCodesToComponent(Object codesCarrier, final Component component) throws InternalProcessingError {
        // searching codes in fields
        ReflectUtils.processAllTaggedField(codesCarrier, org.openmole.core.workflow.model.task.annotations.Code.class, new ReflectUtils.FieldGetter<org.openmole.core.workflow.model.task.annotations.Code>() {

            @Override
            public void process(Field f, Object object, org.openmole.core.workflow.model.task.annotations.Code annotation, Object value) throws InternalProcessingError {
                Code isCode = new Code();
                isCode.setLanguage(annotation.langage());
                isCode.setCode(value.toString());
                component.getCodes().add(isCode);
            }
        });
        // searching codes in methods
        ReflectUtils.processAllTaggedMethod(codesCarrier, org.openmole.core.workflow.model.task.annotations.Code.class, new ReflectUtils.MethodGetter<org.openmole.core.workflow.model.task.annotations.Code>() {

            @Override
            public void process(Method m, Object object, org.openmole.core.workflow.model.task.annotations.Code annotation, Object value) throws InternalProcessingError {
                Code isCode = new Code();
                isCode.setLanguage(annotation.langage());
                isCode.setCode(value.toString());
                component.getCodes().add(isCode);
            }
        });
    }

    private void syncConstantsToComponent(Object constantsCarrier, final Component component, final ExplorationData explorationData) throws InternalProcessingError {
        // searching constants in fields
        ReflectUtils.processAllTaggedField(constantsCarrier, org.openmole.core.workflow.model.task.annotations.Constant.class, new ReflectUtils.FieldGetter<org.openmole.core.workflow.model.task.annotations.Constant>() {

            @Override
            public void process(Field f, Object object, org.openmole.core.workflow.model.task.annotations.Constant annotation, Object value) throws InternalProcessingError {
                syncConstant(component, explorationData, f.getName(), f.getType(), value, annotation);
            }
        });
        // searching constants in methods
        ReflectUtils.processAllTaggedMethod(constantsCarrier, org.openmole.core.workflow.model.task.annotations.Constant.class, new ReflectUtils.MethodGetter<org.openmole.core.workflow.model.task.annotations.Constant>() {

            @Override
            public void process(Method m, Object object, org.openmole.core.workflow.model.task.annotations.Constant annotation, Object value) throws InternalProcessingError {
                syncConstant(component, explorationData, m.getName(), m.getReturnType(), value, annotation);
            }
        });
    }

    private void syncConstant(Component component, ExplorationData explorationData, String declaredName, Class declaredType, Object constantValue, org.openmole.core.workflow.model.task.annotations.Constant annotation) {
        Constant isConstant = new Constant();
        // if the name in the annotation is empty, we use the field name
        isConstant.setName(annotation.name().length() == 0 ? declaredName : annotation.name());
        isConstant.setType(declaredType);
        component.getConstants().add(isConstant);
        // puts value in ED
        explorationData.setConstantValue(isConstant, constantValue);
        LOGGER.finer("Constant saving in "
                + component.getName() + ", constant : "
                + declaredName + " - "
                + constantValue);
    }

    private Component syncProcessorToISComponent(org.simexplorer.ui.ide.workflow.model.ExplorationApplication application, GenericTask processor, ExplorationData explorationData) throws InternalProcessingError {
        Component component;
        // Experimental Design processing
        if (processor instanceof ExplorationTask) {
            ExplorationTask exploration = (ExplorationTask) processor;
            ExplorationComponent explorationComponent = new ExplorationComponent();
            initComponent(explorationComponent);
            // Factors
            explorationComponent.setFactors(new Components());
            for (IFactor factor : exploration.getDesign().getMethod().getFactors()) {
                Component factorComponent = new Component();
                initComponent(factorComponent);
                factorComponent.setName(factor.getName());
                factorComponent.setDescriptors(toDescriptors(factor.getMetadata()));
                factorComponent.getDescriptors().add(new Descriptor(FACTOR_TYPE_KEY, factor.getType().getCanonicalName()));
                // Domain
                assert factor.getDomain() != null;
                factorComponent.setType(new RuntimeType(factor.getDomain().getClass()));
                syncCodesToComponent(factor.getDomain(), factorComponent);
                syncConstantsToComponent(factor.getDomain(), factorComponent, explorationData);
                explorationComponent.getFactors().add(factorComponent);
            }
            // Method
            if (exploration.getDesign().getMethod() != null) {
                explorationComponent.setMethodType(new RuntimeType(exploration.getDesign().getMethod().getClass()));
                syncCodesToComponent(exploration.getDesign().getMethod(), explorationComponent);
                syncConstantsToComponent(exploration.getDesign().getMethod(), explorationComponent, explorationData);
            } else {
                explorationComponent.setMethodType(new RuntimeType(""));
            }
            // Structures
            // TODO
            /*explorationComponent.getStructures().add(syncStructureNodetoISStructure(
            ((ExplorationTask) processor).getInputDataStructure(),
            INPUT_DATA_STRUCTURE_IS_TYPE_NAME));
            explorationComponent.getStructures().add(syncStructureNodetoISStructure(
            ((ExplorationTask) processor).getOutputDataStructure(),
            OUTPUT_DATA_STRUCTURE_IS_TYPE_NAME));*/
            component = explorationComponent;
        } else {
            component = initComponent(new Component());
        }
        syncProcessorToISLoggableElement(processor, component);
        component.setType(new RuntimeType(processor.getClass()));
        // Codes
        syncCodesToComponent(processor, component);
        // Constants
        syncConstantsToComponent(processor, component, explorationData);
        // Subcomponents
        if (processor instanceof TasksList) {
            for (GenericTask child : (TasksList) processor) {
                component.getSubComponents().add(syncProcessorToISComponent(application, child, explorationData));
            }
        }
        return component;
    }

    private String getNameOfStructureNode(StructureNode node) {
        if (node instanceof SequenceNode) {
            return getNameOfStructureNode(((SequenceNode) node).getInnerNode());
        } else if (node instanceof Prototype) {
            return ((Prototype) node).getName();
        } else {
            assert (node instanceof ComplexNode) : node;
            return ((ComplexNode) node).getName();
        }
    }

    private Structure syncStructureNodetoISStructure(StructureNode root, String role) {
        Structure structure = null;
        if (root instanceof SequenceNode) {
            structure = syncStructureNodetoISStructure(((SequenceNode) root).getInnerNode(), role);
            structure.setName(structure.getName() + SEQUENCE_STRUCTURE_SUFFIX);
        } else {
            structure = new Structure();
            structure.setName(getNameOfStructureNode(root));
            if (role != null) {
                structure.getDescriptors().add(new Descriptor(ROLE_KEYS, role));
            }
            if (root instanceof ComplexNode) {
                for (StructureNode child : (ComplexNode) root) {
                    structure.getSubStructures().add(syncStructureNodetoISStructure(child, null));
                }
            } else if (root instanceof Prototype) {
                structure.setRuntimeType(new RuntimeType(((Prototype) root).getType()));
            }
        }
        return structure;
    }

    @Override
    public org.simexplorer.ui.ide.workflow.model.ExplorationApplication loadExplorationApplication(MetaData metaData) throws InternalProcessingError, UserBadDataError {
        return loadExplorationApplication(metaData, null);
    }

    @Override
    public org.simexplorer.ui.ide.workflow.model.ExplorationApplication loadExplorationApplication(MetaData metaData, ExplorationData explorationData) throws InternalProcessingError, UserBadDataError {
        LoggableElement loggableElement = SimExplorer.getContext().getLoggableElement(false, metaData.getUuid(), metaData.getVersion());
        ExplorationApplication loggedApplication = (ExplorationApplication) loggableElement;
        // TODO for instance, only the last exploration is automatically taken.
        if (explorationData == null) {
            explorationData = loggedApplication.getExplorations().get(loggedApplication.getExplorations().size()
                    - 1);
        }
        org.simexplorer.ui.ide.workflow.model.ExplorationApplication explorationApplication = new org.simexplorer.ui.ide.workflow.model.ExplorationApplication(loggedApplication.getName());
        explorationApplication.initSystemVariables();
        syncISLoggableElementToProcessor(loggableElement, explorationApplication.getTreeRoot());
        for (Component component : loggedApplication.getComponents()) {
            explorationApplication.getTreeRoot().add(syncISComponentToProcessor(explorationApplication, component, explorationData));
        }
        // variables
        assert loggedApplication.getComponents().size() > 0;
        for (Structure structure : loggedApplication.getComponents().get(0).getStructures()) {
            if (structure.getName().equals(VARIABLES_NAME)) {
                for (Structure varIS : structure.getSubStructures()) {
                    try {
                        explorationApplication.putContract(new Prototype(varIS.getName(), Class.forName(varIS.getRuntimeType().getType())));
                    } catch (ClassNotFoundException ex) {
                        throw new InternalProcessingError(ex, "Error when trying to load class "
                                + varIS.getRuntimeType().getType());
                    }
                }
            }
        }
        return explorationApplication;
    }

    private void syncISLoggableElementToProcessor(LoggableElement loggableElement, GenericTask processor) {
        processor.setName(loggableElement.getName());
        if (loggableElement.getDescription().length() > 0) {
            MetadataProxy.setDescription(processor, loggableElement.getDescription());
        }
        for (Descriptor descriptor : loggableElement.getDescriptors()) {
            if ((!descriptor.getName().equals("name"))
                    && (!descriptor.getName().equals("description"))) {
                MetadataProxy.setMetadata(processor, descriptor.getName(), descriptor.getValue());
            }
        }
    }

    private void syncCodesFromComponent(final Component component, Object codesCarrier) throws InternalProcessingError {
        if (!component.getCodes().isEmpty()) {
            // syncing codes to attributes
            ReflectUtils.processAllTaggedField(codesCarrier, org.openmole.core.workflow.model.task.annotations.Code.class, new ReflectUtils.FieldSetter<org.openmole.core.workflow.model.task.annotations.Code>() {

                @Override
                public Object getValueToSet(Field f, Object object, org.openmole.core.workflow.model.task.annotations.Code annotation, int counter) throws InternalProcessingError {
                    return component.getCodes().get(counter).getCode();
                }
            });
            // syncing codes to methods
            ReflectUtils.processAllTaggedMethod(codesCarrier, org.openmole.core.workflow.model.task.annotations.Code.class, new ReflectUtils.MethodSetter<org.openmole.core.workflow.model.task.annotations.Code>() {

                @Override
                public Object getValueToSet(Method m, Object object, org.openmole.core.workflow.model.task.annotations.Code annotation, int counter) throws InternalProcessingError {
                    return component.getCodes().get(counter).getCode();
                }
            });
        }
    }

    private void syncConstantsFromComponent(final Component component, Object constantsCarrier, final ExplorationData explorationData) throws InternalProcessingError {
        if (!component.getConstants().isEmpty()) {
            // syncing constants to fields
            ReflectUtils.processAllTaggedField(constantsCarrier, org.openmole.core.workflow.model.task.annotations.Constant.class, new ReflectUtils.FieldSetter<org.openmole.core.workflow.model.task.annotations.Constant>() {

                @Override
                public Object getValueToSet(Field f, Object object, org.openmole.core.workflow.model.task.annotations.Constant annotation, int counter) throws InternalProcessingError {
                    return explorationData.getConstantValue(component.getConstants().get(counter));
                }
            });
            // syncing constants to methods
            ReflectUtils.processAllTaggedMethod(constantsCarrier, org.openmole.core.workflow.model.task.annotations.Constant.class, new ReflectUtils.MethodSetter<org.openmole.core.workflow.model.task.annotations.Constant>() {

                @Override
                public Object getValueToSet(Method m, Object object, org.openmole.core.workflow.model.task.annotations.Constant annotation, int counter) throws InternalProcessingError {
                    return explorationData.getConstantValue(component.getConstants().get(counter));
                }
            });
        }
    }

    private GenericTask syncISComponentToProcessor(org.simexplorer.ui.ide.workflow.model.ExplorationApplication application, Component component, ExplorationData explorationData) throws InternalProcessingError {
        GenericTask processor = null;
        try {
            Class<?> forName = Class.forName(component.getType().getType());
            Constructor<?> constructor = forName.getConstructor(String.class);
            processor = (Task) constructor.newInstance(component.getName());
        } catch (Exception ex) {
            throw new InternalProcessingError(ex);
        }
        syncISLoggableElementToProcessor(component, processor);
        // Codes
        syncCodesFromComponent(component, processor);
        // Nothing to do with the constants declared by annotations :
        // name and type are in the class, and value will be stored in the exploration
        syncConstantsFromComponent(component, processor, explorationData);
        // Subcomponents
        if (!component.getSubComponents().isEmpty()) {
            TasksList processorsList = (TasksList) processor;
            for (Component child : component.getSubComponents().getInnerList()) {
                processorsList.add(syncISComponentToProcessor(application, child, explorationData));
            }
        }
        // Experimental Design
        if (component instanceof ExplorationComponent) {
            ExplorationComponent explorationComponent = (ExplorationComponent) component;
            ExplorationTask explorationLoop = (ExplorationTask) processor;
            explorationLoop.setDesign(new ExperimentalDesign());
            // Factors
            for (Component factorComponent : explorationComponent.getFactors()) {
                Domain domain;
                try {
                    domain = (Domain) Instanciator.instanciate(Class.forName(factorComponent.getType().getType()));
                } catch (Exception ex) {
                    throw new InternalProcessingError(ex);
                }
                syncCodesFromComponent(explorationComponent, domain);
                syncConstantsFromComponent(explorationComponent, domain, explorationData);
                Class<?> factorType;
                try {
                    factorType = Class.forName(findDescriptor(factorComponent.getDescriptors(), FACTOR_TYPE_KEY));
                } catch (ClassNotFoundException ex) {
                    throw new InternalProcessingError(ex);
                }
                IFactor factor = new Factor(factorComponent.getName(), factorType, domain);
                ((IPlan<IFactor<?, ?>>) explorationLoop.getDesign().getMethod()).addFactor(factor);
            }
            // Method
            IPlan method;
            if (explorationComponent.getMethodType().getType().length() > 0) {
                try {
                    method = (IPlan) Instanciator.instanciate(Class.forName(explorationComponent.getMethodType().getType()));
                } catch (Exception ex) {
                    throw new InternalProcessingError(ex);
                }
                syncCodesFromComponent(explorationComponent, method);
                syncConstantsFromComponent(explorationComponent, method, explorationData);
                explorationLoop.getDesign().setMethod(method);
            }
            // Structures
            // TODO
            /*for (Structure structure : component.getStructures().getInnerList()) {
            if (findDescriptor(structure.getDescriptors(), ROLE_KEYS).equals(INPUT_DATA_STRUCTURE_IS_TYPE_NAME)) {
            try {
            explorationLoop.setInputDataStructure((ComplexNode) syncISStructureToStructureNode(structure));
            } catch (ClassNotFoundException ex) {
            throw new InternalProcessingError(ex);
            }
            } else if (findDescriptor(structure.getDescriptors(), ROLE_KEYS).equals(OUTPUT_DATA_STRUCTURE_IS_TYPE_NAME)) {
            try {
            explorationLoop.setOutputDataStructure((ComplexNode) syncISStructureToStructureNode(structure));
            } catch (ClassNotFoundException ex) {
            throw new InternalProcessingError(ex);
            }
            }
            }*/
        }
        return processor;
    }

    private String findDescriptor(Descriptors descriptors, String key) {
        String result = "";
        for (Descriptor descriptor : descriptors) {
            if (descriptor.getName().equals(key)) {
                result = descriptor.getValue();
                break;
            }
        }
        return result;
    }

    private StructureNode syncISStructureToStructureNode(Structure structure) throws ClassNotFoundException {
        StructureNode root = null;
        if (structure.getName().endsWith(SEQUENCE_STRUCTURE_SUFFIX)) {
            structure.setName(structure.getName().substring(0, structure.getName().length()
                    - SEQUENCE_STRUCTURE_SUFFIX.length()));
            root = new SequenceNode(syncISStructureToStructureNode(structure));
        } else {
            if (structure.getRuntimeType() != null) {
                root = new Prototype(structure.getName(), Class.forName(structure.getRuntimeType().getType()));
            } else {
                root = new ComplexNode(structure.getName());
                for (Structure child : structure.getSubStructures()) {
                    ((ComplexNode) root).add(syncISStructureToStructureNode(child));
                }
            }
        }
        return root;
    }
}
