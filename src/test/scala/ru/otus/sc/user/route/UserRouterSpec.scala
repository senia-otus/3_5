package ru.otus.sc.user.route

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import ru.otus.sc.route.ZDirectives
import ru.otus.sc.user.json.UserJsonProtocol._
import ru.otus.sc.user.model.{GetUserRequest, GetUserResponse, Role, User}
import ru.otus.sc.user.service.UserService
import zio.logging.Logging
import zio.logging.slf4j.Slf4jLogger
import zio.test.Assertion._
import zio.test._
import zio.test.akkahttp._
import zio.test.mock.Expectation._
import zio.test.mock.mockable

@mockable[UserService.Service]
object UserServiceMock

object UserRouterSpec extends DefaultAkkaRunnableSpec {
  val user1 = User(Some(UUID.randomUUID()), "SomeName1", "SomeLastName1", 1, Set(Role.Manager))
  val user2 = User(Some(UUID.randomUUID()), "SomeName2", "SomeLastName2", 2, Set(Role.Admin))

  type TestEnv = Environment with Logging

  def spec =
    suite("UserRouterSpec")(
      testM("get user") {
        val id = UUID.randomUUID()
        val env = UserServiceMock
          .GetUser(equalTo(GetUserRequest(id)), value(GetUserResponse.Found(user1)))

        val assertionRes =
          for {
            route <- UserRouter.route
            response = Get(s"/user/$id") ~> route
            res <- assertM(response)(
              handled(
                status(equalTo(StatusCodes.OK)) &&
                  entityAs[User](isRight(equalTo(user1)))
              )
            )
          } yield res

        assertionRes.provideSomeLayer[TestEnv]((ZDirectives.live ++ env) >>> UserRouter.live)
      }
    ).provideSomeLayerShared[Environment](Slf4jLogger.makeWithAnnotationsAsMdc(Nil))
}
