package org.kapunga.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import org.kapunga.spjall.web.ApiResponse.Meta
import org.kapunga.spjall.model.User
import org.kapunga.spjall.{ SlackId, botTokenParam }

import scala.concurrent.Future

object Users extends WebApi {

  def info(id: SlackId, includeLocale: Boolean = false)(implicit as: ActorSystem): Future[ApiResponse[User]] = {
    val params: Map[String, String] = Map(
      "user" -> id.id,
      "include_locale" -> s"$includeLocale") ++ botTokenParam
    val req = HttpRequest(method = HttpMethods.GET, uri = apiUri("users.info", params))

    performApiRequest[User](req, Some("user"))
  }

  def list(includeLocale: Boolean = false, limit: Int = 100)(implicit as: ActorSystem): Future[ApiResponse[Seq[User]]] = {
    def genReq(meta: Option[Meta]): HttpRequest = {
      val params: Map[String, String] = Map(
        "include_locale" -> s"$includeLocale",
        "limit" -> s"$limit") ++ botTokenParam ++ meta.map(_.param).getOrElse(Map())
      HttpRequest(method = HttpMethods.GET, uri = apiUri("users.list", params))
    }

    performPagedApiRequest[User](genReq, "members")
  }
}
