package ru.otus.sc.user.dao

import java.util.UUID

import ru.otus.sc.db.SlickContext.SlickContext
import ru.otus.sc.user.model.{Role, User}
import ru.otus.sc.utils.LoggingUtils.localTimed
import slick.jdbc.PostgresProfile.api._
import zio.clock.Clock
import zio.logging.Logging
import zio.macros.accessible
import zio.{Tag => _, _}

@accessible
object UserDao {
  type UserDao = Has[Service]

  type Env = Logging with Clock
  trait Service {
    def createUser(user: User): URIO[Env, User]
    def getUser(userId: UUID): URIO[Env, Option[User]]
    def updateUser(user: User): URIO[Env, Option[User]]
    def deleteUser(userId: UUID): URIO[Env, Option[User]]
    def findByLastName(lastName: String): URIO[Env, Seq[User]]
    def findAll(): URIO[Env, Seq[User]]
    private[user] def deleteAll(): URIO[Env, Unit]
  }

  val live: URLayer[SlickContext, UserDao] = ZLayer.fromService { slickContext =>
    new Service {
      import UserDaoSlickImpl._

      def getUser(userId: UUID): URIO[Env, Option[User]] =
        localTimed("UserDao", "getUser") {
          slickContext.run { implicit ec =>
            for {
              user <- users.filter(user => user.id === userId).result.headOption
              roles <-
                usersToRoles.filter(_.usersId === userId).map(_.rolesCode).result.map(_.toSet)
            } yield user.map(_.toUser(roles))
          }
        }

      def createUser(user: User): URIO[Env, User] =
        localTimed("UserDao", "getUser") {
          slickContext.run { implicit ec =>
            val userRaw = UserRow.fromUser(user).copy(id = None)

            val res = for {
              newId <- (users returning users.map(_.id)) += userRaw
              _     <- usersToRoles ++= user.roles.map(newId -> _)
            } yield user.copy(id = Some(newId))

            res.transactionally
          }
        }

      def updateUser(user: User): URIO[Env, Option[User]] =
        localTimed("UserDao", "getUser") {
          slickContext.run { implicit ec =>
            user.id match {
              case Some(userId) =>
                val updateUser =
                  users
                    .filter(_.id === userId)
                    .map(u => (u.firstName, u.lastName, u.age))
                    .update((user.firstName, user.lastName, user.age))

                val deleteRoles = usersToRoles.filter(_.usersId === userId).delete
                val insertRoles = usersToRoles ++= user.roles.map(userId -> _)

                val action =
                  for {
                    u <- users.filter(_.id === userId).forUpdate.result.headOption
                    _ <- u match {
                      case None    => DBIO.successful(())
                      case Some(_) => updateUser >> deleteRoles >> insertRoles
                    }
                  } yield u.map(_ => user)

                action.transactionally

              case None => DBIO.successful(None)
            }
          }
        }

      def deleteUser(userId: UUID): URIO[Env, Option[User]] =
        localTimed("UserDao", "getUser") {
          slickContext.run { implicit ec =>
            val action =
              for {
                u <- users.filter(_.id === userId).forUpdate.result.headOption
                res <- u match {
                  case None => DBIO.successful(None)
                  case Some(userRow) =>
                    val rolesQuery = usersToRoles.filter(_.usersId === userId)
                    for {
                      roles <- rolesQuery.map(_.rolesCode).result
                      _     <- rolesQuery.delete
                      _     <- users.filter(_.id === userId).delete
                    } yield Some(userRow.toUser(roles.toSet))
                }
              } yield res

            action.transactionally
          }
        }

      private def findByCondition(condition: Users => Rep[Boolean]): URIO[Env, Vector[User]] =
        localTimed("UserDao", "getUser") {
          slickContext.run { implicit ec =>
            users
              .filter(condition)
              .joinLeft(usersToRoles)
              .on(_.id === _.usersId)
              .result
              .map(_.groupMap(_._1)(_._2).view.map {
                case (user, roles) => user.toUser(roles.flatMap(_.map(_._2)).toSet)
              }.toVector)
          }
        }

      def findByLastName(lastName: String): URIO[Env, Seq[User]] =
        localTimed("UserDao", "getUser") {
          findByCondition(_.lastName === lastName)
        }

      def findAll(): URIO[Env, Seq[User]] =
        localTimed("UserDao", "getUser") { findByCondition(_ => true) }

      private[user] def deleteAll(): URIO[Env, Unit] =
        localTimed("UserDao", "getUser") {
          slickContext.run(_ => usersToRoles.delete >> users.delete).unit
        }
    }
  }

  private object UserDaoSlickImpl {
    implicit val rolesType: BaseColumnType[Role] =
      MappedColumnType.base[Role, String](roleToCode, roleFromCode)

    def roleToCode(role: Role): String =
      role match {
        case Role.Reader  => "reader"
        case Role.Manager => "manager"
        case Role.Admin   => "admin"
      }

    def roleFromCode(code: String): Role =
      Option(code)
        .collect {
          case "reader"  => Role.Reader
          case "manager" => Role.Manager
          case "admin"   => Role.Admin
        }
        .getOrElse(throw new RuntimeException(s"Unsupported role code $code"))

    case class UserRow(
        id: Option[UUID],
        firstName: String,
        lastName: String,
        age: Int
    ) {
      def toUser(roles: Set[Role]): User = User(id, firstName, lastName, age, roles)
    }

    object UserRow extends ((Option[UUID], String, String, Int) => UserRow) {
      def fromUser(user: User): UserRow = UserRow(user.id, user.firstName, user.lastName, user.age)
    }

    class Users(tag: Tag) extends Table[UserRow](tag, "users") {
      val id        = column[UUID]("id", O.PrimaryKey, O.AutoInc)
      val firstName = column[String]("first_name")
      val lastName  = column[String]("last_name")
      val age       = column[Int]("age")

      val * = (id.?, firstName, lastName, age).mapTo[UserRow]
    }

    val users = TableQuery[Users]

    class UsersToRoles(tag: Tag) extends Table[(UUID, Role)](tag, "users_to_roles") {
      val usersId   = column[UUID]("users_id")
      val rolesCode = column[Role]("roles_code")

      val * = (usersId, rolesCode)
    }

    val usersToRoles = TableQuery[UsersToRoles]
  }

}
