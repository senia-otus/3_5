package ru.otus.sc.user.service

import java.util.UUID

import ru.otus.sc.user.dao.UserDao
import ru.otus.sc.user.dao.UserDao.{Env, UserDao}
import ru.otus.sc.user.model.UpdateUserResponse.Updated
import ru.otus.sc.user.model.{CreateUserRequest, Role, UpdateUserRequest, UpdateUserResponse, User}
import zio._
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.test._
import zio.test.mock._
import zio.test.Assertion._
import zio.test.mock.Expectation._

@mockable[UserDao.Service]
object UserDaoMock
//object UserDaoMock extends Mock[UserDao] {
//  object CreateUser     extends Effect[User, Nothing, User]
//  object GetUser        extends Effect[UUID, Nothing, Option[User]]
//  object UpdateUser     extends Effect[User, Nothing, Option[User]]
//  object DeleteUser     extends Effect[UUID, Nothing, Option[User]]
//  object FindByLastName extends Effect[String, Nothing, Seq[User]]
//  object FindAll        extends Effect[Unit, Nothing, Seq[User]]
//
//  override val compose: URLayer[Has[Proxy], UserDao] =
//    ZLayer.fromService { proxy =>
//      new UserDao.Service {
//        def createUser(user: User): URIO[Env, User]           = proxy(CreateUser, user)
//        def getUser(userId: UUID): URIO[Env, Option[User]]    = proxy(GetUser, userId)
//        def updateUser(user: User): URIO[Env, Option[User]]   = proxy(UpdateUser, user)
//        def deleteUser(userId: UUID): URIO[Env, Option[User]] = proxy(DeleteUser, userId)
//        def findByLastName(lastName: String): URIO[Env, Seq[User]] =
//          proxy(FindByLastName, lastName)
//        def findAll(): URIO[Env, Seq[User]] = proxy(FindAll)
//        private[user] def deleteAll()       = ???
//      }
//    }
//}

object UserServiceSpec extends DefaultRunnableSpec {
  val user1 = User(Some(UUID.randomUUID()), "SomeName1", "SomeLastName1", 1, Set(Role.Manager))
  val user2 = User(Some(UUID.randomUUID()), "SomeName2", "SomeLastName2", 2, Set(Role.Admin))

  type TestEnv = Environment with Logging

  def spec =
    suite("UserServiceSpec")(
      testM("createUser") {
        val env    = UserDaoMock.CreateUser(equalTo(user1), value(user2))
        val action = UserService.createUser(CreateUserRequest(user1))
        assertM(
          action.provideSomeLayer[TestEnv](
            env >>> UserService.live
          )
        )(hasField("user", _.user, equalTo(user2)))
      },
      testM("updateUser") {
        val user1  = User(None, "SomeName1", "SomeLastName1", 1, Set(Role.Manager))
        val env    = UserDaoMock.UpdateUser(equalTo(user1), value(Some(user2))).atMost(0)
        val action = UserService.updateUser(UpdateUserRequest(user1))
        assertM(
          action.provideSomeLayer[TestEnv](
            env >>> UserService.live
          )
        )(anything)
      }
    ).provideSomeLayerShared[Environment](Slf4jLogger.makeWithAnnotationsAsMdc(Nil))
}
