package ru.otus.sc.di

import ru.otus.sc.config.{HttpConfig, RootConfig}
import ru.otus.sc.db.SlickContext
import ru.otus.sc.route.Router.Router
import ru.otus.sc.route.{Router, ZDirectives}
import ru.otus.sc.user.dao.UserDao
import ru.otus.sc.user.route.UserRouter
import ru.otus.sc.user.service.UserService
import zio.blocking.Blocking
import zio.clock.Clock
import zio.logging.Logging
import zio._

object DI {
  val live: URLayer[Blocking with Clock with Logging, Router with Has[HttpConfig]] =
    (RootConfig.allConfigs ++ ZLayer.requires[Blocking with Clock with Logging]) >+>
      SlickContext.live >+>
      UserDao.live >+>
      (UserService.live ++ ZDirectives.live) >>>
      UserRouter.live >>>
      (Router.live ++ RootConfig.allConfigs)

}

// DaoL
// ServiceL: ServiceL1(DaoL1 => Srv1) ++ ServiceL1(DaoL2 => Srv2)
// PresentationL

// URLayer:  InEnv => OutEnv
// ZDirectives with UserService => UserRouter
//
// ++: URLayer1(InEnv1 => OutEnv1) ++ URLayer2(InEnv2 => OutEnv2)
// URLayer1 ++ URLayer2: (InEnv1 with InEnv2) => (OutEnv1 with OutEnv2)

// L1: A => B
// L2: B => C
// L1 >>> L2: A => C

// L1: A => B
// L2: B => C
// L3: C => D
// L1 >>> L2 >>> L3: A => D

// L1: A => B  (B: B1 with B2)
// L2: B => C
// L3: C with B1 => D
// L1 >>> L2 >>> L3: A => D

// ZLayer.requires[B1]: URLayer[B1, B1]: B1 => B1
//L2 ++ ZLayer.requires[B1]: B => C with B1
// L1 >>> (L2 ++ ZLayer.requires[B1]) >>> L3: A => D

// L1: A => B  (B: B1 with B2)
// L2: B => C
// L1 >+> L2: A => B with C
// (L1 >>> L2) ++ L1
