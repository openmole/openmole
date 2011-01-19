package org.openmole.core.structuregenerator.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;
import java.util.jar.Manifest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;

import org.codehaus.groovy.ant.Groovyc;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;

public class GroovyProject {

    File projDir;
    File binDir;
    File srcDir;
    File packDir;
    String nameSpaceSuffix;

    Manifest manifest;

    void initProject() throws IOException, InternalProcessingError, UserBadDataError {
        nameSpaceSuffix = UUID.randomUUID().toString();
        nameSpaceSuffix = "generatedpackage" + nameSpaceSuffix.replace("-", "");

        projDir = Activator.getWorkspace().newDir("groovy");

        srcDir = new File(projDir, "src");
        srcDir.mkdir();      

        File orgDir = new File(srcDir,"org");
        orgDir.mkdir();

        File simexplorerDir = new File(orgDir,"simexplorer");
        simexplorerDir.mkdir();

        File userDir = new File(simexplorerDir,"user");
        userDir.mkdir();

        File dataDir = new File(userDir,"data");
        dataDir.mkdir();

        packDir = new File(dataDir, nameSpaceSuffix);
        packDir.mkdir();

        binDir = new File(projDir, "bin");
        binDir.mkdir();

        File manifestFile = new File(projDir, "Manifest.mf");
        
        PrintStream manifestS = new PrintStream(new FileOutputStream(manifestFile));
        try {
            manifestS.println("Manifest-Version: 1.0");
            manifestS.println("Bundle-ManifestVersion: 2");
            manifestS.println("Bundle-Name: " + getNameSpace());
            manifestS.println("Bundle-SymbolicName: " + getNameSpace());
            manifestS.println("Require-Bundle: org.codehaus.groovy, org.openmole.core.model");
            manifestS.println("Export-Package: " + getNameSpace());
            manifestS.println("");
        } finally {
            manifestS.close();
        }

        FileInputStream fman = new FileInputStream(manifestFile);
        try {
            manifest = new Manifest(fman);
        } finally {
            fman.close();
        }

    }

    @Override
    protected void finalize() throws Throwable {
        clean();
    }


    void clean() {
        deleteDir(projDir);
    }

    void compile() {
        Project project = new Project();
        Groovyc compiler = new Groovyc();
        compiler.setProject(project);
        compiler.setSrcdir(new Path(project, srcDir.toString()));
        compiler.setDestdir(project.resolveFile(binDir.toString()));
        compiler.setListfiles(false);
        compiler.execute();
    }

    public File getPackDir() {
        return packDir;
    }

    public File getProjDir() {
        return projDir;
    }

    public File getBinDir() {
        return binDir;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    public String getNameSpace() {
        return "org.openmole.user.data." + nameSpaceSuffix;
    }
}
