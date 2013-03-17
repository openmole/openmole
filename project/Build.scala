import sbt._
import Keys._

object HelloBuild extends libraries with Web {
    lazy val root = Project(id = "root",
                            base = file(".")) aggregate(libraries, web)

}
