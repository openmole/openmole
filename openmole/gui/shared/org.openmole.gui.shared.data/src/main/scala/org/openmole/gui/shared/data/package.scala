package org.openmole.gui.shared.data

/*
 * Copyright (C) 10/03/17 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import endpoints4s.algebra

val connectionRoute = "connection"
val shutdownRoute = "shutdown"
val restartRoute = "restart"

val appRoute = "app"

val downloadFileRoute = "downloadFile"
val uploadFilesRoute = "uploadFiles"
val resetPasswordRoute = "resetPassword"

def downloadFile(uri: String, fileType: ServerFileSystemContext, hash: Boolean = false) =
  s"${downloadFileRoute}?path=$uri&hash=$hash&fileType=${fileType.typeName}"

def hashHeader = "Content-Hash"

import java.io.{PrintWriter, StringWriter}
import scala.collection.immutable.ArraySeq
import scala.scalajs.js.annotation.JSExport

import org.openmole.gui.shared.data.SafePath._


object ServerFileSystemContext:
  def fromTypeName(s: String): Option[ServerFileSystemContext] = ServerFileSystemContext.values.find(_.typeName == s.toLowerCase)

enum ServerFileSystemContext:
  val typeName = this.productPrefix.toLowerCase
  case Absolute, Project, Authentication

object RelativePath:
  implicit def apply(s: Seq[String]): RelativePath = new RelativePath(s)
  extension(p: RelativePath)
    def name = p.value.lastOption.getOrElse("")
    def nameWithoutExtension = p.name.split('.').head
    def mkString = p.value.mkString("/")
    def parent = RelativePath(p.value.dropRight(1))

case class RelativePath(value: Seq[String]):
  def ::(s: String) = RelativePath(Seq(s) ++ value)

object SafePath:
  def leaf(name: String) = SafePath(name)
  def empty = leaf("")
  def naming = (sp: SafePath) ⇒ sp.name

  def apply(value: String*): SafePath = SafePath(ArraySeq.from(value))
  def apply(path: Iterable[String], context: ServerFileSystemContext = ServerFileSystemContext.Project): SafePath =
    new SafePath(ArraySeq.from(path.filter(!_.isEmpty)), context)

case class SafePath(path: RelativePath, context: ServerFileSystemContext):
  def ++(s: String) = copy(path = this.path.value :++ s.split('/'))
  def /(child: String): SafePath = copy(path = path.value :+ child)
  def /(child: RelativePath): SafePath = copy(path = path.value :++ child.value)

  def parent: SafePath = copy(path = path.value.dropRight(1))

  def name = path.name
  def isEmpty = path.value.isEmpty
  def nameWithoutExtension = name.split('.').head
  def normalizedPathString = path.name.tail.mkString("/")

  def startsWith(safePath: SafePath) =
    safePath.context == context &&
      path.value.take(safePath.path.value.size) == safePath.path.value

object PluginState:
  def empty = PluginState(false, false)

case class PluginState(isPlugin: Boolean, isPlugged: Boolean)

object TreeNodeData:
  case class Directory(isEmpty: Boolean)

case class TreeNodeData(
  name: String,
  size: Long,
  time: Long,
  directory: Option[TreeNodeData.Directory] = None,
  pluginState: PluginState = PluginState.empty)

object ErrorData:
  def empty = MessageErrorData("", None)

  def toStackTrace(t: Throwable) =
    val sw = new StringWriter()
    t.printStackTrace(new PrintWriter(sw))
    sw.toString

  def apply(errors: Seq[ErrorWithLocation], t: Throwable) = CompilationErrorData(errors, toStackTrace(t))

  def apply(t: Throwable): MessageErrorData = MessageErrorData(t.getMessage, Some(toStackTrace(t)))

  def apply(message: String) = MessageErrorData(message, None)

  def stackTrace(e: ErrorData) =
    e match
      case MessageErrorData(msg, stackTrace) => msg + stackTrace.map("\n" + _).getOrElse("")
      case CompilationErrorData(_, stackTrace) => stackTrace


sealed trait ErrorData
case class MessageErrorData(message: String, stackTrace: Option[String]) extends ErrorData
case class CompilationErrorData(errors: Seq[ErrorWithLocation], stackTrace: String) extends ErrorData

case class Token(token: String, duration: Long)

object ExecutionId:
  def apply() =
    val id = randomId
    new ExecutionId(id)

case class ExecutionId(id: String)

case class EnvironmentId(id: String = randomId)

enum ErrorStateLevel(val name: String):
  case Debug extends ErrorStateLevel("Debug")
  case Error extends ErrorStateLevel("Error")

case class EnvironmentError(
  environmentId: EnvironmentId,
  errorMessage: String,
  stack: ErrorData,
  date: Long,
  level: ErrorStateLevel) extends Ordered[EnvironmentError]:
  def compare(that: EnvironmentError) = date compare that.date

case class NetworkActivity(
  downloadingFiles: Int = 0,
  downloadedSize: Long = 0L,
  readableDownloadedSize: String = "",
  uploadingFiles: Int = 0,
  uploadedSize: Long = 0L,
  readableUploadedSize: String = "")

case class ExecutionActivity(executionTime: Long = 0)

case class EnvironmentErrorGroup(error: EnvironmentError, oldestDate: Long, number: Int)

case class ExecutionData(
  id: ExecutionId,
  path: SafePath,
  script: String,
  startDate: Long,
  state: ExecutionState,
  output: String,
  executionTime: Long):
  def duration = state.duration

case class EnvironmentState(
  envId: EnvironmentId,
  taskName: String,
  running: Long,
  done: Long,
  submitted: Long,
  failed: Long,
  networkActivity: NetworkActivity,
  executionActivity: ExecutionActivity,
  numberOfErrors: Int)

sealed trait ExecutionState(val state: String):
  def duration: Long
  def capsules: Seq[ExecutionState.CapsuleExecution]
  def ready: Long = capsules.map(_.statuses.ready).sum
  def running: Long = capsules.map(_.statuses.running).sum
  def completed: Long = capsules.map(_.statuses.completed).sum
  def environmentStates: Seq[EnvironmentState]


object ExecutionState:
  case class CapsuleExecution(name: String, scope: String, statuses: ExecutionState.JobStatuses, user: Boolean, userCardinality: Int)
  case class JobStatuses(ready: Long, running: Long, completed: Long)

  case class Failed(
    capsules: Seq[ExecutionState.CapsuleExecution],
    error: ErrorData,
    environmentStates: Seq[EnvironmentState],
    duration: Long = 0L,
    clean: Boolean = true) extends ExecutionState("failed")

  case class Running(
    capsules: Seq[ExecutionState.CapsuleExecution],
    duration: Long,
    environmentStates: Seq[EnvironmentState]) extends ExecutionState("running")

  case class Finished(
    capsules: Seq[ExecutionState.CapsuleExecution],
    duration: Long = 0L,
    environmentStates: Seq[EnvironmentState],
    clean: Boolean) extends ExecutionState("finished")

  case class Canceled(
    capsules: Seq[ExecutionState.CapsuleExecution],
    environmentStates: Seq[EnvironmentState],
    duration: Long = 0L,
    clean: Boolean) extends ExecutionState("canceled")

//    case class Compiling() extends ExecutionInfo("compiling"):
//      def duration: Long = 0L
//      def capsules = Vector.empty
//      def environmentStates: Seq[EnvironmentState] = Seq()

  case class Preparing() extends ExecutionState("preparing"):
    def duration: Long = 0L
    def capsules = Vector.empty
    def environmentStates: Seq[EnvironmentState] = Seq()


case class PasswordState(chosen: Boolean, hasBeenSet: Boolean)

// projectSafePath is the plugin path in the project tree.
// The plugin is copied in the plugin directory with the same name.
case class Plugin(projectSafePath: SafePath, time: String, plugged: Boolean)

//Processes
sealed trait ProcessState {
  def ratio: Int = 0
  def display: String = ""
}

case class Processing(override val ratio: Int = 0) extends ProcessState {
  override def display: String = "Transferring... " + ratio + " %"
}

case class Finalizing(
  override val ratio: Int = 100,
  override val display: String = "Finalizing...") extends ProcessState

case class Processed(override val ratio: Int = 100) extends ProcessState


object Resources {
  def empty = Resources(Seq(), Seq(), 0)
}

case class Resource(safePath: SafePath, size: Long)

case class Resources(all: Seq[Resource], implicits: Seq[Resource], number: Int):
  def withNoImplicit = copy(implicits = Seq())
  def size = all.size + implicits.size

enum FirstLast:
  case First, Last

enum ListSorting:
  case AlphaSorting, SizeSorting, TimeSorting

object FileSorting:
  def toOrdering(filter: FileSorting): Ordering[TreeNodeData] =
    val fs = filter.fileSorting
    def fileSizeOrdering: Ordering[TreeNodeData] = (tnd1, tnd2) => tnd1.size compare tnd2.size

    def alphaOrdering = new Ordering[TreeNodeData]:
      def isDirectory(tnd: TreeNodeData) =
        tnd.directory match
          case None ⇒ false
          case _ ⇒ true

      def compare(tn1: TreeNodeData, tn2: TreeNodeData) =
        if isDirectory(tn1)
        then
          if isDirectory(tn2)
          then tn1.name compare tn2.name
          else -1
        else if isDirectory(tn2)
        then 1
        else tn1.name compare tn2.name


    def timeOrdering: Ordering[TreeNodeData] = (tnd1, tnd2) => tnd1.time compare tnd2.time

    def ordering =
      fs match
        case ListSorting.AlphaSorting ⇒ alphaOrdering
        case ListSorting.SizeSorting ⇒ fileSizeOrdering
        case ListSorting.TimeSorting ⇒ timeOrdering

    filter.firstLast match
      case FirstLast.First => ordering
      case FirstLast.Last => ordering.reverse


case class FileSorting(firstLast: FirstLast = FirstLast.First, fileSorting: ListSorting = ListSorting.AlphaSorting, size: Option[Int] = None)


object FileListData:
  def empty = Seq()

case class FileListData(data: Seq[TreeNodeData] = Seq(), listed: Int = 0, total: Int = 0)

case class OMSettings(workspace: SafePath, version: String, versionName: String, buildTime: String, isDevelopment: Boolean)

sealed trait PluginExtensionType

object AuthenticationExtension extends PluginExtensionType


object PluginExtensionData:
  def empty = PluginExtensionData(Seq.empty, Seq.empty, Seq.empty)

//TODO: add other extension points
case class PluginExtensionData(
  authentications: Seq[GUIPluginAsJS],
  wizards: Seq[GUIPluginAsJS],
  analysis: Seq[(String, GUIPluginAsJS)])

type GUIPluginAsJS = String

trait AuthenticationData:
  def name: String


trait WizardData

sealed trait Test:
  def passed: Boolean
  def message: String
  def error: Option[ErrorData]

//  case class PendingTest() extends Test:
//    def passed = false
//    def message = "pending"
//    def error = None

case class FailedTest(message: String, errorValue: ErrorData) extends Test:
  def error = Some(errorValue)
  def passed = false

case class PassedTest(message: String) extends Test:
  def passed = true
  def error = None


object Test:
  def passed(message: String = "OK") = PassedTest(message)
//    def pending = PendingTest()
  def error(msg: String, err: ErrorData) = FailedTest(msg, err)


case class JVMInfos(javaVersion: String, jvmImplementation: String, processorAvailable: Int, allocatedMemory: Long, totalMemory: Long)

type SequenceHeader = Seq[String]

object SequenceData:
  def empty = SequenceData()

case class SequenceData(header: SequenceHeader = Seq(), content: Seq[Seq[String]] = Seq()):
  def withRowIndexes =
    val lineIndexes = (1 to content.length).map {
      _.toString
    }
    copy(header = header :+ "Row index", content = content.zip(lineIndexes).map { case (l, i) ⇒ l :+ i })



case class ErrorWithLocation(stackTrace: String = "", line: Option[Int] = None, start: Option[Int] = None, end: Option[Int] = None)
case class ErrorFromCompiler(errorWithLocation: ErrorWithLocation = ErrorWithLocation(), lineContent: String = "")
case class EditorErrors(errorsFromCompiler: Seq[ErrorFromCompiler] = Seq(), errorsInEditor: Seq[Int] = Seq())


object NotificationEvent:
  def time(event: NotificationEvent) =
    event match
      case e: MoleExecutionFinished => e.time

  def id(event: NotificationEvent) =
    event match
      case e: MoleExecutionFinished => e.id

  case class MoleExecutionFinished(executionId: ExecutionId, script: SafePath, error: Option[ErrorData], date: String, time: Long, id: Long) extends NotificationEvent


sealed trait NotificationEvent

def randomId = scala.util.Random.alphanumeric.take(10).mkString
