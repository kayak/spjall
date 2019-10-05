package org.kapunga.spjall

import spray.json.{ DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat }

import scala.util.matching.Regex

sealed trait SlackId {
  val id: String
  val mention: Option[String] = None
}

case class ChannelId(id: String) extends SlackId {
  override val mention: Option[String] = Some(s"<#$id>")
}
case class DmId(id: String) extends SlackId
case class EventId(id: String) extends SlackId
case class FileId(id: String) extends SlackId
case class GroupId(id: String) extends SlackId
case class SubGroupId(id: String) extends SlackId
case class TeamId(id: String) extends SlackId
case class UserId(id: String) extends SlackId {
  override val mention: Option[String] = Some(s"<@$id>")
}
case class UnknownId(id: String) extends SlackId

object SlackId {
  val mentionRx: Regex = "<[@#]([a-zA-Z0-9]+)>".r

  def apply(id: String): SlackId = id.headOption match {
    case Some('C') => ChannelId(id)
    case Some('D') => DmId(id)
    case Some('E') => EventId(id)
    case Some('F') => FileId(id)
    case Some('G') => GroupId(id)
    case Some('S') => SubGroupId(id)
    case Some('T') => TeamId(id)
    case Some('U') => UserId(id)
    case Some('W') => UserId(id)
    case _ => UnknownId(id)
  }

  def mentions(msg: String): Seq[SlackId] =
    mentionRx.findAllMatchIn(msg)
      .map(_.group(1)).toSeq
      .map(SlackId(_))
}

object SlackIdJsonProtocol extends DefaultJsonProtocol {
  implicit val slackIdFormat: RootJsonFormat[SlackId] = new RootJsonFormat[SlackId] {
    override def read(json: JsValue): SlackId = json match {
      case JsString(id) => SlackId(id)
      case _ => throw DeserializationException("Expected String for Type `SlackId`...")
    }

    override def write(obj: SlackId): JsValue = JsString(obj.id)
  }
}
