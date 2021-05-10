package org.openmole.gui.server.jscompile

import org.openmole.core.workspace._
import org.openmole.tool.file._

object App {
  def main(argvs: Array[String]) = {

        val sources = new java.io.File("/home/mathieu/Bureau/tmpWebpack/")
        val targetDir = new java.io.File("/tmp/webpackTest")
        val entry = sources / "openmole.js"

//    val sources = new java.io.File("/home/mathieu/work/cogit/scaladget/bootstrapDemo/target/scala-2.13/scalajs-bundler/main")
//    val targetDir = new java.io.File("/tmp/webpackTest")
//    val entry = sources / "bootstrapdemo-opt.js"

    val confiFileTemplate = /*sources / */ new java.io.File("/home/mathieu/Bureau/tmpWebpack/template.webpack.config.js")
    val depsOutput = targetDir / "deps.js"
    depsOutput.createNewFile()

    //1 - generate package.json
    // Stub: scaladget's one used

    //2- install js resources via npm (including webpack)
    //    println("APPP")
    Npm.install(sources)

    //3- build the js deps with webpack
    Webpack.run(
      entry,
      confiFileTemplate,
      sources,
      depsOutput
    )
  }
}