package ru.otus.sc.db

import ru.otus.sc.db.Migrations.WithMigrations
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.PostgresProfile.api._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import ru.otus.sc.utils.LoggingUtils.localTimed
import zio.logging.Logging

import scala.concurrent.ExecutionContext

object SlickContext {
  type SlickContext = Has[Service]

  type Env = Logging with Clock
  trait Service {
    def run[R](make: ExecutionContext => DBIOAction[R, NoStream, Nothing]): URIO[Env, R]
  }

  val fromDatabase: URLayer[Has[Database], SlickContext] = ZLayer.fromService { db =>
    new Service {
      def run[R](make: ExecutionContext => DBIOAction[R, NoStream, Nothing]): URIO[Env, R] =
        localTimed("SlickContext", "run") {
          ZIO.fromFuture(ec => db.run(make(ec))).orDie
        }
    }
  }

  val dbFromConfig: URLayer[Has[DbConfig] with WithMigrations, Has[Database]] =
    ZManaged
      .fromAutoCloseable(ZIO.service[DbConfig].map { config =>
        Database.forURL(config.dbUrl, config.dbUserName, config.dbPassword)
      })
      .toLayer

  type Dependencies = Has[DbConfig] with Blocking with Clock with Logging

  val live: URLayer[Dependencies, SlickContext] =
    (ZLayer.requires[Dependencies] ++ Migrations.live) >+>
      Migrations.afterMigrations >+>
      dbFromConfig >>>
      fromDatabase
}
