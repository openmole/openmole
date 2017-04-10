package org.openmole.plugin.task.udocker

import java.io._
import java.net.URLEncoder

import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{ HttpClients, LaxRedirectStrategy }
import org.apache.http.impl.conn.BasicHttpClientConnectionManager
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import squants.time._

object Registry {

  def copy(is: InputStream, os: OutputStream) =
    Iterator.continually(is.read()).takeWhile(_ != -1).foreach { os.write }

  implicit def formats = org.json4s.DefaultFormats

  def content(response: HttpResponse) = scala.io.Source.fromInputStream(response.getEntity.getContent).mkString

  object HTTP {
    def client = HttpClients.custom().setConnectionManager(new BasicHttpClientConnectionManager()).setRedirectStrategy(new LaxRedirectStrategy).build()

    def execute[T](get: HttpGet)(f: HttpResponse ⇒ T) = {
      val response = client.execute(get)
      try f(response)
      finally response.close
    }
  }

  import HTTP._

  case class Layer(digest: String)
  case class Manifest(value: JValue, image: DockerImage)

  object Token {
    case class AuthenticationRequest(scheme: String, realm: String, service: String, scope: String)
    case class Token(scheme: String, token: String)

    def withToken(url: String, timeout: Time) = {
      val get = new HttpGet(url)
      get.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      val authenticationRequest = authentication(get)
      val t = token(authenticationRequest.get)
      val request = new HttpGet(url)
      request.setHeader("Authorization", s"${t.scheme} ${t.token}")
      request.setConfig(RequestConfig.custom().setConnectTimeout(timeout.millis.toInt).setConnectionRequestTimeout(timeout.millis.toInt).build())
      request
    }

    def authentication(get: HttpGet) = execute(get) { response ⇒
      Option(response.getFirstHeader("Www-Authenticate")).map(_.getValue).map {
        a ⇒
          val Array(scheme, rest) = a.split(" ")
          val map =
            rest.split(",").map {
              l ⇒
                val kv = l.trim.split("=")
                kv(0) → kv(1).stripPrefix("\"").stripSuffix("\"")
            }.toMap
          AuthenticationRequest(scheme, map("realm"), map("service"), map("scope"))
      }

    }

    def token(authenticationRequest: AuthenticationRequest) = {
      val end = URLEncoder.encode("", "UTF-8")
      val tokenRequest = s"${authenticationRequest.realm}?service=${authenticationRequest.service}&scope=${authenticationRequest.scope}"
      val get = new HttpGet(tokenRequest)
      execute(get) { response ⇒
        Token(authenticationRequest.scheme, (parse(content(response)) \ "token").extract[String])
      }
    }

  }

  def baseURL(image: DockerImage) = {
    val path = if (image.image.contains("/")) image.image else s"library/${image.image}"
    s"${image.registry}/v2/$path"
  }

  def manifest(image: DockerImage, timeout: Time): Manifest = {
    val url = s"${baseURL(image)}/manifests/${image.tag}"
    val m = parse(content(client.execute(Token.withToken(url, timeout))))
    Manifest(m, image)
  }

  def layers(manifest: Manifest) =
    (manifest.value \ "fsLayers" \\ "blobSum").children.map(_.extract[String]).distinct.map { l ⇒ Layer(l) }.reverse

  def blob(image: DockerImage, layer: Layer, file: File, timeout: Time) = {
    val url = s"""${baseURL(image)}/blobs/${layer.digest}"""
    execute(Token.withToken(url, timeout)) { response ⇒
      val os = new FileOutputStream(file)
      try copy(response.getEntity.getContent, os)
      finally os.close()
    }
  }

}
