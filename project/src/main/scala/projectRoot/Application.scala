package projectRoot

import sbt._
import Keys._

import net.virtualvoid.sbt.graph._

trait Application extends Web with Libraries {
  private implicit val org = organization := "org.openmole.ui"
  private implicit val dir = file("application")
  lazy val application = Project("application", file("application")) aggregate(plugins, openmoleui,
    openmolePlugins, openmoleGuiPlugins, openmoleResources, openMoleDB)

  private lazy val pluginDependencies = libraryDependencies <++= version {
    v =>
      Seq("org.openmole.core" % "org.openmole.core.model" % v,
        "org.openmole.core" % "org.openmole.core.implementation" % v,
        "org.openmole.core" % "org.openmole.core.batch" % v,
        "org.openmole.core" % "org.openmole.misc.workspace" % v,
        "org.openmole.core" % "org.openmole.misc.replication" % v,
        "org.openmole.core" % "org.openmole.misc.exception" % v,
        "org.openmole.core" % "org.openmole.misc.tools" % v,
        "org.openmole.core" % "org.openmole.misc.eventdispatcher" % v,
        "org.openmole.core" % "org.openmole.misc.pluginmanager" % v,
        "org.openmole.core" % "org.openmole.misc.logging" % v,
        "org.openmole.core" % "org.openmole.misc.sftpserver" % v,
        "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841" intransitive(),
        "org.eclipse.core" % "org.eclipse.core.contenttype" % "3.4.200.v20120523-2004" intransitive(),
        "org.eclipse.core" % "org.eclipse.core.jobs" % "3.5.300.v20120912-155018" intransitive(),
        "org.eclipse.core" % "org.eclipse.core.runtime" % "3.8.0.v20120912-155025" intransitive(),
        "org.eclipse.core" % "org.eclipse.equinox.common" % "3.6.100.v20120522-1841" intransitive(),
        "org.eclipse.core" % "org.eclipse.equinox.launcher" % "1.3.0.v20120522-1813" intransitive(),
        "org.eclipse.core" % "org.eclipse.equinox.registry" % "3.5.200.v20120522-1841" intransitive(),
        "org.eclipse.core" % "org.eclipse.equinox.preferences" % "3.5.1.v20121031-182809" intransitive(),
        "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944" intransitive(),
        "org.openmole" % "org.apache.commons.logging" % v intransitive(),
        "org.openmole" % "net.sourceforge.jline" % v intransitive(),
        "org.openmole" % "org.apache.ant" % v intransitive(),
        "org.openmole" % "uk.com.robustit.cloning" % v intransitive(),
        "org.openmole" % "org.joda.time" % v intransitive(),
        "org.openmole" % "org.scala-lang.scala-library" % v intransitive(),
        "org.openmole" % "org.jasypt.encryption" % v intransitive(),
        "org.openmole" % "org.apache.commons.configuration" % v intransitive(),
        "org.openmole" % "org.objenesis" % v intransitive(),
        "org.openmole" % "com.github.scopt" % v intransitive(),
        "org.openmole.ide" % "org.openmole.ide.core.implementation" % v)
  }

  private lazy val openmolePluginDependencies = libraryDependencies <++= version {
    v => {
      def pluginTemplate(subId: String) = "org.openmole.core" % ("org.openmole.plugin." + subId) % v intransitive()
      Seq(pluginTemplate("tools.groovy"),
        pluginTemplate("environment.gridscale"),
        pluginTemplate("environment.glite"),
        pluginTemplate("environment.desktopgrid"),
        pluginTemplate("environment.ssh"),
        pluginTemplate("environment.pbs"),
        pluginTemplate("grouping.onvariable"),
        pluginTemplate("grouping.batch"),
        pluginTemplate("task.netlogo"),
        pluginTemplate("task.netlogo4"),
        pluginTemplate("task.netlogo5"),
        pluginTemplate("task.systemexec"),
        pluginTemplate("task.groovy"),
        pluginTemplate("task.scala"),
        pluginTemplate("task.code"),
        pluginTemplate("task.external"),
        pluginTemplate("task.template"),
        pluginTemplate("task.stat"),
        pluginTemplate("domain.modifier"),
        pluginTemplate("domain.file"),
        pluginTemplate("domain.collection"),
        pluginTemplate("sampling.csv"),
        pluginTemplate("sampling.lhs"),
        pluginTemplate("sampling.combine"),
        pluginTemplate("domain.range"),
        pluginTemplate("domain.bounded"),
        pluginTemplate("domain.relative"),
        pluginTemplate("domain.distribution"),
        pluginTemplate("sampling.filter"),
        pluginTemplate("profiler.csvprofiler"),
        pluginTemplate("hook.file"),
        pluginTemplate("hook.display"),
        pluginTemplate("source.file"),
        pluginTemplate("method.evolution"),
        pluginTemplate("method.sensitivity"),
        pluginTemplate("builder.base"),
        pluginTemplate("builder.evolution"),
        pluginTemplate("builder.stochastic"),
        "org.openmole" % "au.com.bytecode.opencsv" % v intransitive(),
        "org.openmole" % "ccl.northwestern.edu.netlogo5" % "5.0.3" intransitive(),
        "org.openmole" % "ccl.northwestern.edu.netlogo4" % "4.1.3" intransitive(),
        "org.openmole" % "fr.iscpif.mgo" % v intransitive()
      )
    }
  }

  private lazy val openmoleGuiPluginDependencies = libraryDependencies <++= version {
    v => {
      def pluginTemplate(subArtifact: String) = ("org.openmole.ide" % ("org.openmole.ide.plugin." + subArtifact) % v) intransitive()
      Seq(pluginTemplate("task.groovy"),
        pluginTemplate("task.exploration"),
        pluginTemplate("task.netlogo"),
        pluginTemplate("task.systemexec"),
        pluginTemplate("task.moletask"),
        pluginTemplate("task.stat"),
        pluginTemplate("domain.range"),
        pluginTemplate("domain.collection"),
        pluginTemplate("domain.modifier"),
        pluginTemplate("domain.file"),
        pluginTemplate("domain.distribution"),
        pluginTemplate("sampling.csv"),
        pluginTemplate("sampling.combine"),
        pluginTemplate("sampling.lhs"),
        pluginTemplate("environment.local"),
        pluginTemplate("environment.glite"),
        pluginTemplate("environment.pbs"),
        pluginTemplate("environment.desktopgrid"),
        pluginTemplate("environment.ssh"),
        pluginTemplate("method.sensitivity"),
        pluginTemplate("groupingstrategy"),
        pluginTemplate("misc.tools"),
        pluginTemplate("hook.display"),
        pluginTemplate("source.file"),
        pluginTemplate("builder.base"),
        pluginTemplate("hook.file"),
        "org.openmole.ide" % "org.openmole.ide.osgi.netlogo" % v intransitive(),
        "org.openmole.ide" % "org.openmole.ide.osgi.netlogo4" % v intransitive(),
        "org.openmole.ide" % "org.openmole.ide.osgi.netlogo5" % v intransitive()
      )
    }
  }

  lazy val openmoleui = OsgiProject("org.openmole.ui", singleton = true) settings (pluginDependencies) dependsOn (webCore)

  lazy val plugins = AssemblyProject("package", "assembly/plugins",
    Map("""org\.eclipse\.equinox\.launcher.*\.jar""".r -> {s => "org.eclipse.equinox.launcher.jar"},
      """org\.eclipse\.(core|equinox|osgi)""".r -> {s => s.replaceFirst("-","_")} )
  ) settings (pluginDependencies,
    libraryDependencies <++= (version) {v =>
      Seq("org.openmole.ui" %% "org.openmole.ui" % v exclude("org.eclipse.equinox","*"),
        "org.openmole.web" %% "org.openmole.web.core" % v)
    }, dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))


  lazy val openmolePlugins = AssemblyProject("package", "assembly/openmole-plugins") settings (openmolePluginDependencies,
      ignoreTransitive := true, dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openmoleGuiPlugins = AssemblyProject("package", "assembly/openmole-plugins-gui") settings (openmoleGuiPluginDependencies,
    dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openmoleResources = AssemblyProject("package", "assembly") settings
    (resourceDirectory := file("application/resources"), copyResTask, assemble <<= assemble dependsOn (resourceAssemble),
      dependencyFilter := DependencyFilter.fnToModuleFilter(_.name != "scala-library"))

  lazy val openMoleDB = AssemblyProject("package", "assembly/dbserver/lib") settings (libraryDependencies ++=
    Seq("org.openmole.core" % "org.openmole.runtime.dbserver" % "0.8.0-SNAPSHOT"),
    copyResTask, resourceDirectory := file("application/db-resources"), assemble <<= assemble dependsOn (resourceAssemble),
    resourceOutDir := Option("assembly/dbserver/bin"))
}