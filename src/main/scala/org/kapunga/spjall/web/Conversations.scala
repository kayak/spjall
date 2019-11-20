package org.kapunga.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ HttpMethods, HttpRequest }
import org.kapunga.spjall.model.ConvTypes.ConvTypes
import org.kapunga.spjall.model._
import org.kapunga.spjall._
import org.kapunga.spjall.web.ApiResponse.Meta

import scala.concurrent.{ ExecutionContext, Future }

object Conversations extends WebApi {
  def info(id: SlackId, includeLocale: Boolean = false, includeNumMembers: Boolean = false)(implicit as: ActorSystem): Future[ApiResponse[Conversation]] = {
    val params: Map[String, String] =
      Map(
        "channel" -> id.id,
        "include_locale" -> s"$includeLocale",
        "include_num_members" -> s"$includeNumMembers") ++ botTokenParam
    val req = HttpRequest(method = HttpMethods.GET, uri = apiUri("conversations.info", params))

    performApiRequest[Conversation](req, Some("channel"))
  }

  def list(ct: Set[ConvTypes] = Set(ConvTypes.CHANNEL), includeArchived: Boolean = false, limit: Int = 100)(implicit as: ActorSystem): Future[ApiResponse[Seq[Conversation]]] = {
    def genReq(meta: Option[Meta]): HttpRequest = {
      val l: Int = if (limit < 1) 1 else if (limit > 1000) 1000 else limit

      val params: Map[String, String] = Map(
        "types" -> ct.map(_.toString).mkString(","),
        "exclude_archived" -> s"${!includeArchived}",
        "limit" -> s"$l") ++ botTokenParam ++ meta.map(_.param).getOrElse(Map())
      HttpRequest(method = HttpMethods.GET, uri = apiUri("conversations.list", params))
    }

    performPagedApiRequest[Conversation](genReq, "channels")
  }

  def setTopic(channel: SlackId, topic: String)(implicit as: ActorSystem): Future[ApiResponse[Boolean]] = {
    implicit val ec: ExecutionContext = as.dispatcher

    val params: Map[String, String] = Map(
      "channel" -> channel.id,
      "topic" -> topic) ++ userTokenParam

    val req = HttpRequest(method = HttpMethods.POST, uri = apiUri("conversations.setTopic", params))

    performApiRequest[String](req, Some("topic")).map(_.map(_ == topic))
  }
}
