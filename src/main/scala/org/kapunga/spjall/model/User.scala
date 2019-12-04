package org.kapunga.spjall.model

import org.kapunga.spjall._
import cats.syntax.either._
import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._

case class User(
  id: SlackId,
  team: SlackId,
  name: String,
  realName: Option[String],
  tz: Option[User.Tz],
  profile: Profile,
  props: User.Props,
  color: Option[String],
  updated: Long,
  locale: Option[String]) extends SlackObject {

  override val identifiers: List[String] = name :: profile.contact.email.toList
}

object User {
  case class Tz(name: Option[String], label: String, offset: Int)

  case class Props(
    deleted: Boolean,
    isAdmin: Boolean,
    isOwner: Boolean,
    isPrimaryOwner: Boolean,
    isRestricted: Boolean,
    isUltraRestricted: Boolean,
    isBot: Boolean,
    isStranger: Boolean,
    isAppUser: Boolean,
    has2fa: Boolean)

  def buildTz(name: Option[String], label: Option[String], offset: Option[Int]): Option[Tz] =
    for {
      l <- label
      o <- offset
    } yield { Tz(name, l, o) }

  implicit val userDecoder: Decoder[User] = new Decoder[User] {
    override def apply(c: HCursor): Result[User] =
      for {
        id <- c.downField("id").as[SlackId]
        team <- c.downField("team_id").as[SlackId]
        name <- c.downField("name").as[String]
        deleted <- c.downField("deleted").as[Option[Boolean]]
        color <- c.downField("color").as[Option[String]]
        realName <- c.downField("real_name").as[Option[String]]
        tz <- c.downField("tz").as[Option[String]]
        tzLabel <- c.downField("tz_label").as[Option[String]]
        tzOffset <- c.downField("tz_offset").as[Option[Int]]
        profile <- c.downField("profile").as[Profile]
        isAdmin <- c.downField("is_admin").as[Option[Boolean]]
        isOwner <- c.downField("is_owner").as[Option[Boolean]]
        isPrimaryOwner <- c.downField("is_primary_owner").as[Option[Boolean]]
        isRestricted <- c.downField("is_restricted").as[Option[Boolean]]
        isUltraRestricted <- c.downField("is_ultra_restricted").as[Option[Boolean]]
        isBot <- c.downField("is_bot").as[Boolean]
        isStranger <- c.downField("is_stranger").as[Option[Boolean]]
        updated <- c.downField("updated").as[Long]
        isAppUser <- c.downField("is_app_user").as[Boolean]
        has2fa <- c.downField("has_2fa").as[Option[Boolean]]
        locale <- c.downField("locale").as[Option[String]]
      } yield {
        User(id, team, name, realName, buildTz(tz, tzLabel, tzOffset), profile,
          Props(
            deleted.getOrElse(false),
            isAdmin.getOrElse(false),
            isOwner.getOrElse(false),
            isPrimaryOwner.getOrElse(false),
            isRestricted.getOrElse(false),
            isUltraRestricted.getOrElse(false),
            isBot,
            isStranger.getOrElse(false),
            isAppUser,
            has2fa.getOrElse(false)),
          color,
          updated,
          locale)
      }
  }

  implicit val userEncoder: Encoder[User] = new Encoder[User] {
    override def apply(a: User): Json = Json.obj(
      ("id", a.id.asJson),
      ("team_id", a.team.asJson),
      ("name", a.name.asJson),
      ("real_name", a.realName.asJson),
      ("tz", a.tz.map(_.name).asJson),
      ("tz_label", a.tz.map(_.label).asJson),
      ("tz_offset", a.tz.map(_.offset).asJson),
      ("profile", a.profile.asJson),
      ("deleted", a.props.deleted.asJson),
      ("is_admin", a.props.isAdmin.asJson),
      ("is_owner", a.props.isOwner.asJson),
      ("is_primary_owner", a.props.isPrimaryOwner.asJson),
      ("is_restricted", a.props.isRestricted.asJson),
      ("is_ultra_restricted", a.props.isUltraRestricted.asJson),
      ("is_bot", a.props.isBot.asJson),
      ("is_stranger", a.props.isStranger.asJson),
      ("is_app_user", a.props.isAdmin.asJson),
      ("has_2fa", a.props.has2fa.asJson),
      ("color", a.color.asJson),
      ("updated", a.updated.asJson),
      ("locale", a.locale.asJson))
  }
}

case class Profile(
  names: Profile.Names,
  contact: Profile.Contact,
  status: Option[Status],
  title: Option[String],
  avatarHash: String,
  images: Profile.Images,
  team: SlackId,
  alwaysActive: Boolean,
  guestChannels: Option[String],
  invitedBy: Option[SlackId])

object Profile {
  case class Names(
    realName: String,
    realNameNormalized: String,
    displayName: String,
    displayNameNormalized: String,
    firstName: Option[String],
    lastName: Option[String])

  case class Contact(phone: Option[String], skype: Option[String], email: Option[String])

  case class Images(
    original: Option[String],
    isCustom: Boolean,
    img24: String,
    img32: String,
    img48: String,
    img72: String,
    img192: String,
    img512: String,
    img1024: Option[String])

  implicit val profileDecoder: Decoder[Profile] = new Decoder[Profile] {
    override def apply(c: HCursor): Result[Profile] =
      for {
        title <- c.downField("title").as[Option[String]]
        phone <- c.downField("phone").as[Option[String]]
        skype <- c.downField("skype").as[Option[String]]
        avatarHash <- c.downField("avatar_hash").as[String]
        statusText <- c.downField("status_text").as[Option[String]]
        statusTextCanonical <- c.downField("status_text_canonical").as[Option[String]]
        statusEmoji <- c.downField("status_emoji").as[Option[String]]
        statusExpiration <- c.downField("status_expiration").as[Option[Long]]
        realName <- c.downField("real_name").as[String]
        displayName <- c.downField("display_name").as[String]
        realNameNorm <- c.downField("real_name_normalized").as[String]
        displayNameNorm <- c.downField("display_name_normalized").as[String]
        firstName <- c.downField("first_name").as[Option[String]]
        lastName <- c.downField("last_name").as[Option[String]]
        alwaysActive <- c.downField("always_active").as[Option[Boolean]]
        email <- c.downField("email").as[Option[String]]
        guestChannels <- c.downField("guest_channels").as[Option[String]]
        invitedBy <- c.downField("guest_invited_by").as[Option[String]]
        isCustomImage <- c.downField("is_custom_image").as[Option[Boolean]]
        imgOrig <- c.downField("image_original").as[Option[String]]
        img24 <- c.downField("image_24").as[String]
        img32 <- c.downField("image_32").as[String]
        img48 <- c.downField("image_48").as[String]
        img72 <- c.downField("image_72").as[String]
        img192 <- c.downField("image_192").as[String]
        img512 <- c.downField("image_512").as[String]
        img1024 <- c.downField("image_1024").as[Option[String]]
        team <- c.downField("team").as[SlackId]
      } yield {
        Profile(
          Names(realName, realNameNorm, displayName, displayNameNorm, firstName, lastName),
          Contact(phone.flatMap(emptyStringOption), skype.flatMap(emptyStringOption), email),
          for {
            text <- statusText
            textCanonical <- statusTextCanonical
            emoji <- statusEmoji
            expire <- statusExpiration
          } yield { Status(text, textCanonical, emoji, expire) },
          title,
          avatarHash,
          Images(imgOrig, isCustomImage.getOrElse(false), img24, img32, img48, img72, img192, img512, img1024),
          team,
          alwaysActive.getOrElse(false),
          guestChannels,
          invitedBy.flatMap(emptyStringOption).map(SlackId.apply))
      }
  }

  implicit val profileEncoder: Encoder[Profile] = new Encoder[Profile] {
    override def apply(a: Profile): Json = Json.obj(
      ("real_name", a.names.realName.asJson),
      ("real_name_normalized", a.names.realNameNormalized.asJson),
      ("display_name", a.names.displayName.asJson),
      ("display_name_normalized", a.names.displayNameNormalized.asJson),
      ("first_name", a.names.firstName.asJson),
      ("last_name", a.names.lastName.asJson),
      ("phone", a.contact.phone.getOrElse("").asJson),
      ("skype", a.contact.skype.getOrElse("").asJson),
      ("email", a.contact.email.asJson),
      ("status_text", a.status.map(_.text).asJson),
      ("status_text_canonical", a.status.map(_.canonical).asJson),
      ("status_emoji", a.status.map(_.emoji).asJson),
      ("status_expiration", a.status.map(_.expiration).asJson),
      ("title", a.title.asJson),
      ("avatar_hash", a.avatarHash.asJson),
      ("image_original", a.images.original.asJson),
      ("is_custom_image", a.images.isCustom.asJson),
      ("image_24", a.images.img24.asJson),
      ("image_32", a.images.img32.asJson),
      ("image_48", a.images.img48.asJson),
      ("image_72", a.images.img72.asJson),
      ("image_192", a.images.img192.asJson),
      ("image_512", a.images.img512.asJson),
      ("team", a.team.asJson),
      ("always_active", a.alwaysActive.asJson),
      ("guest_channels", a.guestChannels.asJson),
      ("guest_invited_by", a.invitedBy.asJson))
  }
}
