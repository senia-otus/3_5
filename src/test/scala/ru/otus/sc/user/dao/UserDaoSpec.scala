package ru.otus.sc.user.dao

import java.util.UUID

import org.testcontainers.containers.PostgreSQLContainer

//import com.dimafeng.testcontainers.PostgreSQLContainer
import ru.otus.sc.db.{DbConfig, SlickContext}
import ru.otus.sc.user.model.User
import zio._
import zio.blocking.{Blocking, effectBlocking}
import zio.clock.Clock
import zio.logging.slf4j.Slf4jLogger
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.magnolia._

object UserDaoSpec extends DefaultRunnableSpec {

  val container =
    ZLayer.fromAcquireRelease(effectBlocking {
      val res = new PostgreSQLContainer()
      res.start()
      res
    }.orDie)(cont => effectBlocking(cont.stop()).orDie)

  val config: URLayer[Has[PostgreSQLContainer[Nothing]], Has[DbConfig]] =
    ZLayer.fromService(container =>
      DbConfig(
        dbUrl = container.getJdbcUrl,
        dbUserName = container.getUsername,
        dbPassword = container.getPassword
      )
    )

  val loggingLayer = Slf4jLogger.makeWithAnnotationsAsMdc(Nil)

  val env = {
    (ZLayer.requires[Blocking with Clock] ++ loggingLayer) >+>
      (container >>> config) >+>
      SlickContext.live >>>
      (UserDao.live ++ loggingLayer)
  }

  val pgString = Gen.string(Gen.oneOf(Gen.char('\u0001', '\uD7FF'), Gen.char('\uE000', '\uFFFD')))

  implicit val deriveGenString: DeriveGen[String] = DeriveGen.instance(pgString)

  val genUser = DeriveGen[User]

  def spec =
    suite("UserRouterSpec")(
      testM("createUser - create any number of users") {
        checkM(Gen.vectorOf(genUser), genUser) { (users, user) =>
          for {
            _           <- ZIO.foreach_(users)(UserDao.createUser)
            createdUser <- UserDao.createUser(user)
          } yield assert(createdUser)(
            hasField[User, Option[UUID]]("id", _.id, isSome(anything) && not(equalTo(user.id))) &&
              equalTo(user.copy(id = createdUser.id))
          )
        }
      },
      suite("getUser")(
        testM("get unknown user") {
          checkM(Gen.vectorOf(genUser), Gen.anyUUID) { (users, userId) =>
            ZIO.foreach_(users)(UserDao.createUser) *>
              assertM(UserDao.getUser(userId))(isNone)
          }
        },
        testM("get known user") {
          checkM(Gen.vectorOf(genUser), genUser, Gen.vectorOf(genUser)) { (users1, user, users2) =>
            for {
              _           <- ZIO.foreach_(users1)(UserDao.createUser)
              createdUser <- UserDao.createUser(user)
              _           <- ZIO.foreach_(users2)(UserDao.createUser)
              res         <- assertM(UserDao.getUser(createdUser.id.get))(isSome(equalTo(createdUser)))
            } yield res
          }
        }
      ),
      suite("updateUser")(
        testM("update unknown user - keep all users the same") {
          checkM(Gen.vectorOf(genUser), genUser) { (users, user) =>
            for {
              createdUsers <- ZIO.foreach(users)(UserDao.createUser)
              updateResult <- assertM(UserDao.updateUser(user))(isNone ?? "Update unknown")
              otherResults <- ZIO.foreach(createdUsers) { u =>
                assertM(UserDao.getUser(u.id.get))(isSome(equalTo(u)) ?? "Get known")
              }
            } yield otherResults.foldLeft(updateResult)(_ && _)
          }
        },
        testM("update known user - keep other users the same") {
          checkM(Gen.vectorOf(genUser), genUser, genUser, Gen.vectorOf(genUser)) {
            (users1, user1, user2, users2) =>
              for {
                createdUsers1 <- ZIO.foreach(users1)(UserDao.createUser)
                createdUser   <- UserDao.createUser(user1)
                createdUsers2 <- ZIO.foreach(users2)(UserDao.createUser)
                toUpdate = user2.copy(id = createdUser.id)
                updateResult <- assertM(UserDao.updateUser(toUpdate))(
                  isSome(equalTo(toUpdate)) ?? "Update known"
                )
                getResult <- assertM(UserDao.getUser(toUpdate.id.get))(
                  isSome(equalTo(toUpdate)) ?? "Get updated"
                )
                otherResults1 <- ZIO.foreach(createdUsers1) { u =>
                  assertM(UserDao.getUser(u.id.get))(isSome(equalTo(u)) ?? "Get known 1")
                }
                otherResults2 <- ZIO.foreach(createdUsers2) { u =>
                  assertM(UserDao.getUser(u.id.get))(isSome(equalTo(u)) ?? "Get known 2")
                }
              } yield (otherResults1 ++ otherResults2).foldLeft(updateResult && getResult)(_ && _)
          }
        }
      ),
      suite("deleteUser")(
        testM("delete unknown user - keep all users the same") {
          checkM(Gen.vectorOf(genUser), Gen.anyUUID) { (users, userId) =>
            for {
              createdUsers <- ZIO.foreach(users)(UserDao.createUser)
              deleteResult <- assertM(UserDao.deleteUser(userId))(isNone ?? "Delete unknown")
              otherResults <- ZIO.foreach(createdUsers) { u =>
                assertM(UserDao.getUser(u.id.get))(isSome(equalTo(u)) ?? "Get known")
              }
            } yield otherResults.foldLeft(deleteResult)(_ && _)
          }
        },
        testM("update known user - keep other users the same") {
          checkM(Gen.vectorOf(genUser), genUser, Gen.vectorOf(genUser)) { (users1, user, users2) =>
            for {
              createdUsers1 <- ZIO.foreach(users1)(UserDao.createUser)
              createdUser   <- UserDao.createUser(user)
              createdUsers2 <- ZIO.foreach(users2)(UserDao.createUser)
              getResult1 <- assertM(UserDao.getUser(createdUser.id.get))(
                isSome(equalTo(createdUser)) ?? "Get created"
              )
              deleteResult <- assertM(UserDao.deleteUser(createdUser.id.get))(
                isSome(equalTo(createdUser)) ?? "Delete known"
              )
              getResult2 <- assertM(UserDao.getUser(createdUser.id.get))(
                isNone ?? "Get deleted"
              )
              otherResults1 <- ZIO.foreach(createdUsers1) { u =>
                assertM(UserDao.getUser(u.id.get))(isSome(equalTo(u)) ?? "Get known 1")
              }
              otherResults2 <- ZIO.foreach(createdUsers2) { u =>
                assertM(UserDao.getUser(u.id.get))(isSome(equalTo(u)) ?? "Get known 2")
              }
            } yield (otherResults1 ++ otherResults2).foldLeft(
              getResult1 && deleteResult && getResult2
            )(_ && _)
          }
        }
      ),
      testM("findByLastName") {
        checkM(Gen.vectorOf(genUser), pgString, Gen.vectorOf(genUser)) {
          (users1, lastName, users2) =>
            val withOtherLastName = users1.filterNot(_.lastName == lastName)
            val withLastName      = users2.map(_.copy(lastName = lastName))

            for {
              _                  <- ZIO.foreach_(withOtherLastName)(UserDao.createUser)
              createdWithLasName <- ZIO.foreach(withLastName)(UserDao.createUser)
              res                <- assertM(UserDao.findByLastName(lastName))(hasSameElements(createdWithLasName))
            } yield res
        }
      },
      testM("findAll") {
        checkM(Gen.vectorOf(genUser)) { users =>
          ZIO.foreach(users)(UserDao.createUser).flatMap { createdUsers =>
            assertM(UserDao.findAll())(hasSameElements(createdUsers))
          }
        }
      }
    )
      .@@(after(UserDao.deleteAll()))
      .provideSomeLayerShared[Environment](env)
}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//val env = {
//  SlickContext.live >>>
//  UserDao.live
//}
//
//  val container: URLayer[Blocking, Has[PostgreSQLContainer]] = ZLayer.fromAcquireRelease(
//  effectBlocking(PostgreSQLContainer()).orDie
//  )(cont => effectBlocking(cont.close()).orDie)
//
//  val containerConfig: URLayer[Has[PostgreSQLContainer], Has[DbConfig]] =
//  ZLayer.fromService { container =>
//  DbConfig(container.jdbcUrl, container.username, container.password)
//}
//
