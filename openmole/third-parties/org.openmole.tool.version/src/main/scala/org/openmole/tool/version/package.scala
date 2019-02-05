/*
 * Copyright (C) 2019 Mathieu Leclaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.tool

import java.util.Date

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib._
import org.eclipse.jgit.revwalk._
import org.eclipse.jgit.treewalk._
import org.eclipse.jgit.errors.{ ConfigInvalidException, MissingObjectException }

import collection.JavaConverters._
import scala.annotation.tailrec
import scala.util.control.Exception._

package object version {

  //API
  private def git(repositoryPath: java.io.File) = new Git(Git.open(repositoryPath).getRepository())

  def content(repositoryPath: java.io.File, filePath: java.io.File, branch: String = "master") = {

    val fileName = filePath.getName
    val relativeFile = new java.io.File(filePath.getAbsolutePath.diff(repositoryPath.getAbsolutePath).tail)
    val files = getFileList(git(repositoryPath), branch, relativeFile.getParent)

    files.find { file ⇒
      !file.isDirectory && file.name == fileName
    }.map { f ⇒
      convertFromByteArray(
        getContentFromId(git(repositoryPath), f.id, true).get
      )
    }.getOrElse("")
  }

  def using[A <: { def close(): Unit }, B](resource: A)(f: A ⇒ B): B =
    try f(resource)
    finally {
      if (resource != null) {
        ignoring(classOf[Throwable]) {
          resource.close()
        }
      }
    }

  def isLarge(size: Long): Boolean = (size > 1024 * 1000)

  /**
   * Get object content of the given object id as byte array from the Git repository.
   *
   * @param git            the Git object
   * @param id             the object id
   * @param fetchLargeFile if false then returns None for the large file
   * @return the byte array of content or None if object does not exist
   */
  def getContentFromId(git: Git, id: ObjectId, fetchLargeFile: Boolean): Option[Array[Byte]] =
    try {
      using(git.getRepository.getObjectDatabase) { db ⇒
        val loader = db.open(id)
        if (loader.isLarge || (fetchLargeFile == false && isLarge(loader.getSize))) {
          None
        }
        else {
          Some(loader.getBytes)
        }
      }
    }
    catch {
      case e: MissingObjectException ⇒ None
    }

  /**
   * Get object content of the given path as byte array from the Git repository.
   *
   * @param git            the Git object
   * @param revTree        the rev tree
   * @param path           the path
   * @param fetchLargeFile if false then returns None for the large file
   * @return the byte array of content or None if object does not exist
   */
  def getContentFromPath(git: Git, revTree: RevTree, path: String, fetchLargeFile: Boolean): Option[Array[Byte]] = {
    @scala.annotation.tailrec
    def getPathObjectId(path: String, walk: TreeWalk): Option[ObjectId] = walk.next match {
      case true if (walk.getPathString == path) ⇒ Some(walk.getObjectId(0))
      case true                                 ⇒ getPathObjectId(path, walk)
      case false                                ⇒ None
    }

    using(new TreeWalk(git.getRepository)) { treeWalk ⇒
      treeWalk.addTree(revTree)
      treeWalk.setRecursive(true)
      getPathObjectId(path, treeWalk)
    } flatMap { objectId ⇒
      getContentFromId(git, objectId, fetchLargeFile)
    }
  }

  private val GitBucketUrlPattern = "^(https?://.+)/git/(.+?)/(.+?)\\.git$".r
  private val GitHubUrlPattern = "^https://(.+@)?github\\.com/(.+?)/(.+?)\\.git$".r
  private val BitBucketUrlPattern = "^https?://(.+@)?bitbucket\\.org/(.+?)/(.+?)\\.git$".r
  private val GitLabUrlPattern = "^https?://(.+@)?gitlab\\.com/(.+?)/(.+?)\\.git$".r

  def getRepositoryViewerUrl(gitRepositoryUrl: String, baseUrl: Option[String]): String = {
    def removeUserName(baseUrl: String): String = baseUrl.replaceFirst("(https?://).+@", "$1")

    gitRepositoryUrl match {
      case GitBucketUrlPattern(base, user, repository) if baseUrl.map(removeUserName(base).startsWith).getOrElse(false) ⇒
        s"${removeUserName(base)}/$user/$repository"
      case GitHubUrlPattern(_, user, repository)    ⇒ s"https://github.com/$user/$repository"
      case BitBucketUrlPattern(_, user, repository) ⇒ s"https://bitbucket.org/$user/$repository"
      case GitLabUrlPattern(_, user, repository)    ⇒ s"https://gitlab.com/$user/$repository"
      case _                                        ⇒ gitRepositoryUrl
    }
  }

  /**
   * Read submodule information from .gitmodules
   */
  def getSubmodules(git: Git, tree: RevTree, baseUrl: Option[String]): List[SubmoduleInfo] = {
    val repository = git.getRepository
    getContentFromPath(git, tree, ".gitmodules", true).map { bytes ⇒
      (try {
        val config = new BlobBasedConfig(repository.getConfig(), bytes)
        config.getSubsections("submodule").asScala.map { module ⇒
          val path = config.getString("submodule", module, "path")
          val url = config.getString("submodule", module, "url")
          SubmoduleInfo(module, path, url, getRepositoryViewerUrl(url, baseUrl))
        }
      }
      catch {
        case e: ConfigInvalidException ⇒ Nil

      }).toList
    } getOrElse Nil
  }

  def defining[A, B](value: A)(f: A ⇒ B): B = f(value)

  private def getSummaryMessage(fullMessage: String, shortMessage: String): String = {
    defining(fullMessage.trim.indexOf('\n')) { i ⇒
      defining(if (i >= 0) fullMessage.trim.substring(0, i).trim else fullMessage) { firstLine ⇒
        if (firstLine.length > shortMessage.length) shortMessage else firstLine
      }
    }
  }

  /**
   * Returns the file list of the specified path.
   *
   * @param git      the Git object
   * @param revision the branch name or commit id
   * @param path     the directory path (optional)
   * @param baseUrl  the base url of GitBucket instance. This parameter is used to generate links of submodules (optional)
   * @return HTML of the file list
   */
  def getFileList(git: Git, revision: String, path: String = ".", baseUrl: Option[String] = None): List[FileInfo] = {
    using(new RevWalk(git.getRepository)) { revWalk ⇒
      val objectId = git.getRepository.resolve(revision)
      if (objectId == null) return Nil
      val revCommit = revWalk.parseCommit(objectId)

      def useTreeWalk(rev: RevCommit)(f: TreeWalk ⇒ Any): Unit =
        if (path == ".") {
          val treeWalk = new TreeWalk(git.getRepository)
          treeWalk.addTree(rev.getTree)
          using(treeWalk)(f)
        }
        else {
          val treeWalk = TreeWalk.forPath(git.getRepository, path, rev.getTree)
          if (treeWalk != null) {
            treeWalk.enterSubtree
            using(treeWalk)(f)
          }
        }

      @tailrec
      def simplifyPath(
        tuple: (ObjectId, FileMode, String, String, Option[String], RevCommit)
      ): (ObjectId, FileMode, String, String, Option[String], RevCommit) = tuple match {
        case (oid, FileMode.TREE, name, path, _, commit) ⇒
          (using(new TreeWalk(git.getRepository)) { walk ⇒
            walk.addTree(oid)
            // single tree child, or None
            if (walk.next() && walk.getFileMode(0) == FileMode.TREE) {
              Some(
                (
                  walk.getObjectId(0),
                  walk.getFileMode(0),
                  name + "/" + walk.getNameString,
                  path + "/" + walk.getNameString,
                  None,
                  commit
                )
              ).filterNot(_ ⇒ walk.next())
            }
            else {
              None
            }
          }) match {
            case Some(child) ⇒ simplifyPath(child)
            case _           ⇒ tuple
          }
        case _ ⇒ tuple
      }

      def tupleAdd(tuple: (ObjectId, FileMode, String, String, Option[String]), rev: RevCommit) = tuple match {
        case (oid, fmode, name, path, opt) ⇒ (oid, fmode, name, path, opt, rev)
      }

      @tailrec
      def findLastCommits(
        result:      List[(ObjectId, FileMode, String, String, Option[String], RevCommit)],
        restList:    List[((ObjectId, FileMode, String, String, Option[String]), Map[RevCommit, RevCommit])],
        revIterator: java.util.Iterator[RevCommit]
      ): List[(ObjectId, FileMode, String, String, Option[String], RevCommit)] = {
        if (restList.isEmpty) {
          result
        }
        else if (!revIterator.hasNext) { // maybe, revCommit has only 1 log. other case, restList be empty
          result ++ restList.map { case (tuple, map) ⇒ tupleAdd(tuple, map.values.headOption.getOrElse(revCommit)) }
        }
        else {
          val newCommit = revIterator.next
          val (thisTimeChecks, skips) = restList.partition {
            case (tuple, parentsMap) ⇒ parentsMap.contains(newCommit)
          }
          if (thisTimeChecks.isEmpty) {
            findLastCommits(result, restList, revIterator)
          }
          else {
            var nextRest = skips
            var nextResult = result
            // Map[(name, oid), (tuple, parentsMap)]
            val rest = scala.collection.mutable.Map(thisTimeChecks.map { t ⇒
              (t._1._3 -> t._1._1) -> t
            }: _*)
            lazy val newParentsMap = newCommit.getParents.map(_ -> newCommit).toMap
            useTreeWalk(newCommit) { walk ⇒
              while (walk.next) {
                rest.remove(walk.getNameString -> walk.getObjectId(0)).map {
                  case (tuple, _) ⇒
                    if (newParentsMap.isEmpty) {
                      nextResult +:= tupleAdd(tuple, newCommit)
                    }
                    else {
                      nextRest +:= tuple -> newParentsMap
                    }
                }
              }
            }
            rest.values.map {
              case (tuple, parentsMap) ⇒
                val restParentsMap = parentsMap - newCommit
                if (restParentsMap.isEmpty) {
                  nextResult +:= tupleAdd(tuple, parentsMap(newCommit))
                }
                else {
                  nextRest +:= tuple -> restParentsMap
                }
            }
            findLastCommits(nextResult, nextRest, revIterator)
          }
        }
      }

      var fileList: List[(ObjectId, FileMode, String, String, Option[String])] = Nil
      useTreeWalk(revCommit) { treeWalk ⇒
        while (treeWalk.next()) {
          val linkUrl = if (treeWalk.getFileMode(0) == FileMode.GITLINK) {
            getSubmodules(git, revCommit.getTree, baseUrl).find(_.path == treeWalk.getPathString).map(_.viewerUrl)
          }
          else None
          fileList +:= (treeWalk.getObjectId(0), treeWalk.getFileMode(0), treeWalk.getNameString, treeWalk.getPathString, linkUrl)
        }
      }
      revWalk.markStart(revCommit)
      val it = revWalk.iterator
      val lastCommit = it.next
      val nextParentsMap = Option(lastCommit).map(_.getParents.map(_ -> lastCommit).toMap).getOrElse(Map())
      findLastCommits(List.empty, fileList.map(a ⇒ a -> nextParentsMap), it)
        .map(simplifyPath)
        .map {
          case (objectId, fileMode, name, path, linkUrl, commit) ⇒
            FileInfo(
              objectId,
              fileMode == FileMode.TREE || fileMode == FileMode.GITLINK,
              name,
              path,
              getSummaryMessage(commit.getFullMessage, commit.getShortMessage),
              commit.getName,
              commit.getAuthorIdent.getWhen,
              commit.getAuthorIdent.getName,
              commit.getAuthorIdent.getEmailAddress,
              linkUrl
            )
        }
        .sortWith { (file1, file2) ⇒
          (file1.isDirectory, file2.isDirectory) match {
            case (true, false) ⇒ true
            case (false, true) ⇒ false
            case _             ⇒ file1.name.compareTo(file2.name) < 0
          }
        }
    }
  }

  def convertFromByteArray(content: Array[Byte]): String = new String(content.map(_.toChar))

  case class FileInfo(
    id:          ObjectId,
    isDirectory: Boolean,
    name:        String,
    path:        String,
    message:     String,
    commitId:    String,
    time:        Date,
    author:      String,
    mailAddress: String,
    linkUrl:     Option[String]
  )

  /**
   * The submodule data
   *
   * @param name          the module name
   * @param path          the path in the repository
   * @param repositoryUrl the repository url of this module
   * @param viewerUrl     the repository viewer url of this module
   */
  case class SubmoduleInfo(name: String, path: String, repositoryUrl: String, viewerUrl: String)

}
