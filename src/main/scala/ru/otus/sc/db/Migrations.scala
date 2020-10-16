package ru.otus.sc.db

import org.flywaydb.core.Flyway
import zio.blocking.{Blocking, effectBlocking}
import zio.{Has, URIO, URLayer, ZIO, ZLayer}
import zio.logging._
import ru.otus.sc.utils.LoggingUtils.localTimed
import zio.clock.Clock

object Migrations {
  type Migrations = Has[Service]

  type Env = Blocking with Logging with Clock
  trait Service {
    def applyMigrations(): URIO[Env, Unit]
  }

  sealed trait AfterMigrations
  type WithMigrations = Has[AfterMigrations]

  val live: URLayer[Has[DbConfig], Migrations] = ZLayer.fromService { config =>
    new Service {
      def applyMigrations(): URIO[Env, Unit] =
        localTimed("Migrations", "applyMigrations") {
          effectBlocking {
            Flyway
              .configure()
              .dataSource(config.dbUrl, config.dbUserName, config.dbPassword)
              .load()
              .migrate()
          }.orDie.unit
        }
    }
  }

  val afterMigrations: URLayer[Env with Migrations, WithMigrations] =
    ZIO.service[Service].flatMap(_.applyMigrations()).as(new AfterMigrations {}).toLayer
}
