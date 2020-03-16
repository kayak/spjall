package com.r9.spjall.model

import cats.syntax.either._
import io.circe.Decoder.Result
import io.circe._

import scala.util.matching.Regex

/**
 * A `trait` used by a number of subclasses that represent a Slack id.
 *
 * @author Paul Thordarson - thor@kayak.com
 */
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
case class SubGroupId(id: String) extends SlackId {
  override val mention: Option[String] = Some(s"<@$id>")
}
case class TeamId(id: String) extends SlackId
case class UserId(id: String) extends SlackId {
  override val mention: Option[String] = Some(s"<@$id>")
}
case class UnknownId(id: String) extends SlackId

object SlackId {
  implicit val slackIdDecoder: Decoder[SlackId] = new Decoder[SlackId] {
    override def apply(c: HCursor): Result[SlackId] =
      for {
        sid <- c.as[String]
      } yield { SlackId.apply(sid) }
  }

  implicit val slackIdEncoder: Encoder[SlackId] = new Encoder[SlackId] {
    override def apply(a: SlackId): Json = Json.fromString(a.id)
  }

  val idFormat: String = "[A-Z][A-Z0-9]{8}"
  val mentionRx: Regex = s"<[@#]($idFormat)>".r

  def isSlackId(s: String): Boolean = s.matches(idFormat)

  /**
   * Auxiliary constructor for `SlackId`. Generates a `SlackId` from a String
   * @param id A String to convert into a `SlackId`
   * @return A `SlackId` representation of a Slack Id
   * @throws Exception if `id` is not in the format of a SlackId
   */
  def apply(id: String): SlackId = {
    if (!id.matches(idFormat)) throw new Exception(s"'$id' does not match the format for a Slack Id.")
    id.headOption match {
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
  }

  /**
   * Extract a `Seq[SlackId]` containing a `SlackId` for every mention in the String.
   * Used to check chat messages for users, channel, and subgroup mentions.
   * @param msg A message to check for mentions
   * @return A `Seq[SlackId]` containing a `SlackId` for each mention in the input string.
   */
  def mentions(msg: String): Seq[SlackId] =
    mentionRx.findAllMatchIn(msg)
      .map(_.group(1)).toSeq
      .map(SlackId(_))
}

trait IdMatcher[I] {
  def matches(id: SlackId): Boolean
}

object IdMatcher {
  implicit val channelIdMatcher: IdMatcher[Channel] = new IdMatcher[Channel] {
    override def matches(id: SlackId): Boolean = id.isInstanceOf[ChannelId]
  }

  implicit val groupIdMatcher: IdMatcher[Group] = new IdMatcher[Group] {
    override def matches(id: SlackId): Boolean = id.isInstanceOf[GroupId]
  }

  implicit val mpimIdMatcher: IdMatcher[Mpim] = new IdMatcher[Mpim] {
    override def matches(id: SlackId): Boolean = id.isInstanceOf[GroupId]
  }

  implicit val imIdMatcher: IdMatcher[Im] = new IdMatcher[Im] {
    override def matches(id: SlackId): Boolean = id.isInstanceOf[DmId]
  }

  implicit val convIdMatcher: IdMatcher[Conversation] = new IdMatcher[Conversation] {
    override def matches(id: SlackId): Boolean =
      Seq(channelIdMatcher, groupIdMatcher, mpimIdMatcher, imIdMatcher).exists(_.matches(id))
  }

  implicit val subGroupIdMatcher: IdMatcher[UserGroup] = new IdMatcher[UserGroup] {
    override def matches(id: SlackId): Boolean = id.isInstanceOf[SubGroupId]
  }

  implicit val userIdMatcher: IdMatcher[User] = new IdMatcher[User] {
    override def matches(id: SlackId): Boolean = id.isInstanceOf[UserId]
  }
}
