import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences <<= ScalariformKeys.preferences (p =>
  p.setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(CompactControlReadability, true)
  .setPreference(PreserveDanglingCloseParenthesis, true))

libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.3"
