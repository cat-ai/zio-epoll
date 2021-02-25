package zio.linux.epoll

import com.sun.jna.Pointer

import zio.linux.LinuxIO
import zio.{IO, Ref, UIO, ZEnv, ZIO}

import java.io.IOException

class ZEpollEvent protected[epoll](val pointer: Pointer) {

  private val FlagRef: UIO[Ref[TypesafeFlagContainer]] = Ref.make(TypesafeFlagContainer())

  def flag: ZIO[ZEnv with LinuxIO, IOException, TypesafeFlagContainer] =
    (for {
      ref            <- FlagRef
      container      <- ref.get
      offset         <- ZNativeEpollEvent.offsetEventsZIO()
      valuePointedTo <- IO.effect(pointer.getInt(offset))
      upd            =  container.copy(eventFlag = valuePointedTo)
      _              <- ref.update(_ => upd)
    } yield upd).refineToOrDie[IOException]

  def updateFlag(newFlag: EventFlag): ZIO[ZEnv with LinuxIO, IOException, Unit] =
    (for {
      ref            <- FlagRef
      offset         <- ZNativeEpollEvent.offsetEventsZIO()
      _              <- IO.effect(pointer.setInt(offset, newFlag.value))
      upd            <- ref.update(_.copy(eventFlag = newFlag.value))
    } yield upd).refineToOrDie[IOException]

  def userData: ZIO[ZEnv with LinuxIO, IOException, Long] =
    (for {
      offset   <- ZNativeEpollEvent.offsetEventsZIO()
      userData <- ZIO.effect(pointer.getLong(offset))
    } yield userData).refineToOrDie[IOException]

  def updateUserData(newValue: Long): ZIO[ZEnv with LinuxIO, IOException, Unit] =
    (for {
      offset      <- ZNativeEpollEvent.offsetEventsZIO()
      updUserData <- ZIO.effect(pointer.setLong(offset, newValue))
    } yield updUserData).refineToOrDie[IOException]

}