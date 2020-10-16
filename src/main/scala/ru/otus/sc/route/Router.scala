package ru.otus.sc.route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.otus.sc.user.route.UserRouter
import ru.otus.sc.user.route.UserRouter.UserRouter
import zio.{Has, URLayer, ZLayer}

object Router {
  type Router = Has[Service]

  trait Service extends BaseRouter {
    def route: Route
  }

  val live: URLayer[UserRouter, Router] = ZLayer.fromFunction { env =>
    val userRouter = env.get[UserRouter.Service]

    new Service {
      def route: Route =
        pathPrefix("api" / "v1") {
          concat(
            userRouter.route
          )
        }
    }
  }
}
