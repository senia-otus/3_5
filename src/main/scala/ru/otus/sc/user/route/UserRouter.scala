package ru.otus.sc.user.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import ru.otus.sc.route.{BaseRouter, ZDirectives}
import ru.otus.sc.user.model.{
  CreateUserRequest,
  CreateUserResponse,
  GetUserRequest,
  GetUserResponse,
  User
}
import ru.otus.sc.user.service.UserService
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import ru.otus.sc.route.ZDirectives.ZDirectives
import ru.otus.sc.user.json.UserJsonProtocol._
import ru.otus.sc.user.service.UserService.UserService
import zio._
import zio.clock.Clock
import zio.logging.Logging
import zio.macros.accessible

import scala.concurrent.Future

@accessible
object UserRouter {
  type UserRouter = Has[Service]

  trait Service extends BaseRouter {
    def route: Route
  }

  val live: URLayer[ZDirectives with UserService, UserRouter] =
    ZLayer.fromServices[ZDirectives.Service, UserService.Service, Service] {
      (directives, service) =>
        import directives._
        new Service {
          def route: Route =
            pathPrefix("user") {
              getUser ~
                createUser
            }

          private val UserIdRequest = JavaUUID.map(GetUserRequest)

          private def getUser: Route =
            (get & path(UserIdRequest)) { userIdRequest =>
              onSuccessZio(service.getUser(userIdRequest)) {
                case GetUserResponse.Found(user) =>
                  complete(user)
                case GetUserResponse.NotFound(_) =>
                  complete(StatusCodes.NotFound)
              }
            }

          private def createUser: Route =
            (post & entity(as[User])) { user =>
              completeZio(service.createUser(CreateUserRequest(user)).map(_.user))
            }
        }
    }

}
