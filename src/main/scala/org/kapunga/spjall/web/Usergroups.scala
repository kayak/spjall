package org.kapunga.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import org.kapunga.spjall._
import org.kapunga.spjall.model.UserGroup

import scala.concurrent.Future

object Usergroups extends WebApi {
  def list(includeUsers: Boolean = true, includeDisabled: Boolean = false, includeCount: Boolean = false)(implicit as: ActorSystem): Future[ApiResponse[Seq[UserGroup]]] = {

    val params: Map[String, String] = Map(
      "include_users" -> s"$includeUsers",
      "include_count" -> s"$includeDisabled",
      "include_disabled" -> s"$includeCount") ++ userTokenParam
    val req = HttpRequest(method = HttpMethods.GET, uri = apiUri("usergroups.list", params))

    performApiRequest[Seq[UserGroup]](req, Some("usergroups"))
  }

  def usersUpdate(id: SlackId, users: Seq[SlackId], includeCount: Boolean = false)(implicit as: ActorSystem): Future[ApiResponse[UserGroup]] = {
    val params: Map[String, String] = Map(
      "usergroup" -> id.id,
      "users" -> users.map(_.id).mkString(","),
      "include_count" -> s"$includeCount") ++ userTokenParam
    val req = HttpRequest(method = HttpMethods.POST, uri = apiUri("usergroups.users.update", params))

    performApiRequest[UserGroup](req, Some("usergroup"))
  }
}
