/*
 *
 *  Copyright (c) 2008, Cemagref
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
package org.openmole.core.structuregenerator.internal;

import org.osgi.framework.Bundle;
import org.openmole.commons.exception.InternalProcessingError;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.openmole.core.structuregenerator.IStructureGenerator;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.PrototypeNode;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.structuregenerator.SequenceNode;
import org.openmole.core.structuregenerator.StructureNode;
import org.openmole.commons.tools.io.FileUtil;

public class StructureGenerator implements IStructureGenerator {

    private String classPrefix = "FromStructure";

    @Override
    public Class generateClass(ComplexNode structures) throws InternalProcessingError {
        File jar;

        GroovyProject gp;
        String className;
        try {
            gp = new GroovyProject();
            gp.initProject();

            className = buildJavaCode(structures,  gp.getPackDir(), gp.getNameSpace());
            //structures = packProcessorList(application, gp.getPackDir(), gp.getNameSpace());
            gp.compile();

            jar = Activator.getWorkspace().newTmpFile("structure", ".jar");
            mkJar(gp.getBinDir(), jar, gp.getManifest());
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }

        Activator.getPluginManager().load(jar);

        Bundle b = Activator.getPluginManager().getBundle(jar);
        //loadClassesInProcessorList(application, gp, b);
        gp.clean();
        try {
            Class ret = b.loadClass(gp.getNameSpace() + '.' + className);
            //System.out.println(ret.getName());
            return ret;
        } catch (ClassNotFoundException ex) {
            throw new InternalProcessingError(ex);
        }
    }

    public GroovyProject compile(Collection<ComplexNode> structures) throws IOException, InternalProcessingError {
        GroovyProject gp = new GroovyProject();
        gp.initProject();
        packStructures(structures, gp.getPackDir(), gp.getNameSpace());
        gp.compile();
        return gp;
    }

    /*  public void mkjar(GroovyProject gp, JarOutputStream jar) throws InternalProcessingError {
    try {
    mkJar(gp.getBinDir(), gp.getManifest(), jar);
    } catch (IOException e) {
    throw new InternalProcessingError(e);
    }
    }
     */
    private void packStructures(Collection<ComplexNode> structures, File packDir, String nameSpace) throws IOException {

        Set<NodePath<ComplexNode>> builded = new TreeSet<NodePath<ComplexNode>>();

        for (ComplexNode struct : structures) {
            buildJavaCode(struct, packDir, nameSpace, builded);
        }
    }
/*
    private void loadClassesInProcessorList(IMole workflow, final GroovyProject gp, final Bundle b) throws InternalProcessingError {

        workflow.visit(new IVisitor<IGenericTaskCapsule>() {

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError {
                try {
                    if (IStructureGenerationTask.class.isAssignableFrom(visited.getAssignedTask().getClass())) {
                        IStructureGenerationTask loop = (IStructureGenerationTask) visited.getAssignedTask();


                        if (loop.getInputClassName() != null) {
                            Class<?> in = b.loadClass(gp.getNameSpace() + '.' + loop.getInputClassName());
                            loop.setInputDataStructureClass(in);
                        }

                        if (loop.getOutputClassName() != null) {
                            Class<?> out = b.loadClass(gp.getNameSpace() + '.' + loop.getOutputClassName());
                            loop.setOutputDataStructureClass(out);
                        }


                    }
                } catch (ClassNotFoundException e) {
                    throw new InternalProcessingError(e);
                }
            }
        });
    }*/

    /*private List<ComplexNode> packProcessorList(final IMole application, final File pack, final String nameSpace) throws InternalProcessingError {

        final List<ComplexNode> structures = new LinkedList<ComplexNode>();

        application.visit(new IVisitor<IGenericTaskCapsule>() {

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError {
                try {
                    if (IStructureGenerationTask.class.isAssignableFrom(visited.getAssignedTask().getClass())) {
                        IStructureGenerationTask loop = (IStructureGenerationTask) visited.getAssignedTask();
                        Set<NodePath<ComplexNode>> builded = new TreeSet<NodePath<ComplexNode>>();

                        if (loop.getInputDataStructureClass() == null) {
                            ComplexNode in = loop.getInputDataStructure();
                            if (in != null) {
                                structures.add(in);
                                String inClassName = buildJavaCode(in, pack, nameSpace, builded);
                                loop.setInputClassName(inClassName);
                            }
                        }

                        if (loop.getOutputDataStructureClass() == null) {
                            ComplexNode out = loop.getOutputDataStructure();
                            if (out != null) {
                                structures.add(out);
                                String outClassName = buildJavaCode(out, pack, nameSpace, builded);
                                loop.setOutputClassName(outClassName);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new InternalProcessingError(e);
                }
            }
        });

        return structures;
    }*/

    /*  void mkJar(File binDir, Manifest man, JarOutputStream zos, String entry) throws FileNotFoundException, IOException, InternalProcessingError {
    zipDir(binDir, zos);

    JarEntry anEntry = new JarEntry(entry);
    zos.putNextEntry(anEntry);

    }*/
    void mkJar(File binDir, File jar, Manifest man) throws FileNotFoundException, IOException, InternalProcessingError {
        JarOutputStream zos = new JarOutputStream(new FileOutputStream(jar), man);
        try {
            zipDir(binDir, zos);
        } finally {
            zos.close();
        }
    }

    private void zipDir(File dir2zip, JarOutputStream zos, String relative) throws IOException, InternalProcessingError {
        File zipDir = dir2zip;
        String[] dirList = zipDir.list();


        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                //String filePath = f.getPath();
                zipDir(f, zos, relative + f.getName() + "/");
                continue;
            }

            FileInputStream fis = new FileInputStream(f);
            try {
                JarEntry anEntry = new JarEntry(relative + f.getName());
                anEntry.setTime(f.lastModified());
                zos.putNextEntry(anEntry);

                FileUtil.copy(fis, zos);

            } finally {
                fis.close();
            }

        }
    }

    private void zipDir(File dir2zip, JarOutputStream zos) throws IOException, InternalProcessingError {
        zipDir(dir2zip, zos, "");
    }


    private String buildJavaCode(ComplexNode rootNode, File pack, String nameSpace) throws IOException {
        return buildJavaCode(rootNode, pack, nameSpace, new TreeSet<NodePath<ComplexNode>>());
    }


    /**
     *
     * @param node
     * @return a list containing : <ul>
     * <li>The code declaring the object of the upper type</li>
     * <li>The code declaring the variables and fields</li>
     * <li>A list of the structures types definitions</li>
     * </ul>
     * @throws IOException
     */
    private String buildJavaCode(ComplexNode rootNode, File pack, String nameSpace, Set<NodePath<ComplexNode>> builded) throws IOException {

        Stack<NodePath<ComplexNode>> toBuild = new Stack<NodePath<ComplexNode>>();

        NodePath<ComplexNode> root = new NodePath<ComplexNode>("", rootNode);
        toBuild.add(root);

        while (!toBuild.isEmpty()) {
            NodePath<ComplexNode> complexNodePath = toBuild.pop();
            if (!builded.contains(complexNodePath)) {
                String code = buildComplexNode(complexNodePath, nameSpace, toBuild);
                File out = new File(pack, complexNodePath.getClassName() + ".groovy");

                BufferedWriter writter = new BufferedWriter(new FileWriter(out));
                try {
                    writter.append(code);
                } finally {
                    writter.close();
                }
                builded.add(complexNodePath);
            }
        }

        return root.getClassName();
    }

    private String buildComplexNode(NodePath<ComplexNode> node, String nameSpace, Stack<NodePath<ComplexNode>> toBuild) {
        // builded.add(node);

        StringBuilder typeDeclaration = new StringBuilder("package " + nameSpace + ";\n\n");

        typeDeclaration.append("public class ");
        typeDeclaration.append(node.getClassName());
        // /!\ This should be done by introspection on the generated classes not by implementing interfaces creating useless dependancies
        // typeDeclaration.append(node.getClassName()).append(" implements DataContainer");
       /* if (node.getNode().isModifiable()) {
        typeDeclaration.append(", DataModifier");
        }*/
        if (node.getNode().getParent() != null) {
            NodePath<ComplexNode> parentNodePath = new NodePath<ComplexNode>(node.getClassName(), node.getNode().getParent());
            toBuild.push(parentNodePath);
            typeDeclaration.append(" extends ");
            typeDeclaration.append(parentNodePath.getClassName());
        }
        typeDeclaration.append(" {\n");

        for (StructureNode child : node.getNode()) {
            String n = null;

            if (child instanceof Prototype) {
                Prototype<?> p = (Prototype<?>) child;
                n = buildPrototypeAttribute(p);
            } else if (child instanceof SequenceNode) {
                SequenceNode<?> s = (SequenceNode<?>) child;
                n = buildSequenceNodeAttribute(new NodePath<SequenceNode<?>>(node.getClassName(), s), toBuild);
            } else if (child instanceof ComplexNode) {
                ComplexNode s = (ComplexNode) child;
                toBuild.add(new NodePath<ComplexNode>(node.getClassName(), s));
                n = buildComplexNodeAttribute(new NodePath<ComplexNode>(node.getClassName(), s));
            }

            typeDeclaration.append("  public " + n + " " + child.getName() + ";\n");

        }

        typeDeclaration.append("  @Override\n  public Object getValue(String name) {\n    return this[name];\n  }\n");
        //if (node.getNode().isModifiable()) {
        typeDeclaration.append("  @Override\n  public void setValue(String name, Object value) {\n    this[name] = value;\n  }\n");
        //}
        typeDeclaration.append("}\n\n");

        return typeDeclaration.toString();
    }

    private String buildComplexNodeAttribute(NodePath<ComplexNode> node) {
        return node.getClassName();
    }

    private String buildSequenceNodeAttribute(NodePath<SequenceNode<?>> nodePath, Stack<NodePath<ComplexNode>> toBuild) {


        StringBuilder res = new StringBuilder();
        SequenceNode<?> node = nodePath.getNode();

        res.append("List<");
        if (node.getInnerNode() instanceof PrototypeNode) {
            res.append(((PrototypeNode<?>) node.getInnerNode()).getPrototype().getType().getCanonicalName());
        } else if (node.getInnerNode() instanceof SequenceNode) {
            SequenceNode<?> inner = (SequenceNode<?>) node.getInnerNode();
            String n = buildSequenceNodeAttribute(new NodePath<SequenceNode<?>>(nodePath.getPath(), inner), toBuild);
            res.append(n);
        } else if (node.getInnerNode() instanceof ComplexNode) {
            ComplexNode inner = (ComplexNode) node.getInnerNode();
            toBuild.add(new NodePath<ComplexNode>(nodePath.getPath(), inner));
            String n = buildComplexNodeAttribute(new NodePath<ComplexNode>(nodePath.getPath(), inner));
            res.append(n);
        }

        res.append(">");


        return res.toString();
    }

    private String buildPrototypeAttribute(Prototype<?> node) {
        return node.getType().getCanonicalName();
    }

    class NodePath<T extends StructureNode> implements Comparable<NodePath> {

        String path;
        T node;

        public NodePath(String path, T node) {
            this.node = node;
            this.path = path;
        }

        String getPath() {
            return path;
        }

        T getNode() {
            return node;
        }

        String getClassName() {
            return path + classPrefix + node.getName();
        }

        @Override
        public int compareTo(NodePath o) {
            return getClassName().compareTo(o.getClassName());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NodePath<T> other = (NodePath<T>) obj;
            return getClassName().equals(other.getClassName());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.getClassName() != null ? this.getClassName().hashCode() : 0);
            return hash;
        }
    }
}
