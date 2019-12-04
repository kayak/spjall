package org.kapunga.spjall.model

import cats.syntax.either._
import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._
import org.kapunga.spjall.model.UserGroup.Prefs

case class UserGroup(
  id: SlackId,
  team: SlackId,
  name: String,
  handle: String,
  description: String,
  isUserGroup: Boolean,
  isSubTeam: Boolean,
  subTeamId: String,
  isExternal: Boolean,
  dateCreate: Long,
  dateUpdate: Long,
  dateDelete: Long,
  autoType: Option[String],
  autoProvision: Boolean,
  createdBy: SlackId,
  updatedBy: SlackId,
  deletedBy: Option[SlackId],
  prefs: Prefs,
  users: List[SlackId]) extends SlackObject {

  override val identifiers: List[String] = handle :: Nil
}

object UserGroup {
  case class Prefs(channels: List[SlackId], groups: List[SlackId])

  implicit val userGroupDecoder: Decoder[UserGroup] = new Decoder[UserGroup] {
    override def apply(c: HCursor): Result[UserGroup] =
      for {
        id <- c.downField("id").as[SlackId]
        teamId <- c.downField("team_id").as[SlackId]
        isUserGroup <- c.downField("is_usergroup").as[Boolean]
        isSubteam <- c.downField("is_subteam").as[Boolean]
        subteamId <- c.downField("enterprise_subteam_id").as[String]
        name <- c.downField("name").as[String]
        description <- c.downField("description").as[String]
        handle <- c.downField("handle").as[String]
        isExternal <- c.downField("is_external").as[Boolean]
        dateCreate <- c.downField("date_create").as[Long]
        dateUpdate <- c.downField("date_update").as[Long]
        dateDelete <- c.downField("date_delete").as[Long]
        autoType <- c.downField("auto_type").as[Option[String]]
        autoProvision <- c.downField("auto_provision").as[Boolean]
        createdBy <- c.downField("created_by").as[SlackId]
        updatedBy <- c.downField("updated_by").as[SlackId]
        deletedBy <- c.downField("deleted_by").as[Option[SlackId]]
        channelPrefs <- c.downField("prefs").downField("channels").as[List[SlackId]]
        groupPrefs <- c.downField("prefs").downField("groups").as[List[SlackId]]
        users <- c.downField("users").as[Option[List[SlackId]]]
      } yield {
        UserGroup(
          id,
          teamId,
          name,
          handle,
          description,
          isUserGroup,
          isSubteam,
          subteamId,
          isExternal,
          dateCreate,
          dateUpdate,
          dateDelete,
          autoType,
          autoProvision,
          createdBy,
          updatedBy,
          deletedBy,
          Prefs(channelPrefs, groupPrefs),
          users.getOrElse(Nil))
      }
  }

  implicit val userGroupEncoder: Encoder[UserGroup] = new Encoder[UserGroup] {
    override def apply(a: UserGroup): Json = Json.obj(
      ("id", a.id.asJson),
      ("team_id", a.team.asJson),
      ("name", a.name.asJson),
      ("handle", a.handle.asJson),
      ("description", a.description.asJson),
      ("is_usergroup", a.isUserGroup.asJson),
      ("is_subteam", a.isSubTeam.asJson),
      ("enterprise_subteam_id", a.subTeamId.asJson),
      ("is_external", a.isExternal.asJson),
      ("date_create", a.dateCreate.asJson),
      ("date_update", a.dateUpdate.asJson),
      ("date_delete", a.dateDelete.asJson),
      ("auto_type", a.autoType.asJson),
      ("auto_provision", a.autoType.asJson),
      ("created_by", a.createdBy.asJson),
      ("updated_by", a.updatedBy.asJson),
      ("deleted_by", a.deletedBy.asJson),
      ("prefs", Json.obj(
        ("channels", a.prefs.channels.asJson),
        ("groups", a.prefs.groups.asJson))),
      ("users", a.users.asJson))
  }
}
