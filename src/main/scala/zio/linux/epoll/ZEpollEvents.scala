package zio.linux.epoll

import com.sun.jna.Memory

import zio.{ZEnv, ZIO}
import zio.linux._

import java.io.IOException

import scala.annotation.tailrec

case class ZEpollEvents(events: Array[ZEpollEvent],
                        memory: Memory) {

  def eventAtOpt(idx: Int): Option[ZEpollEvent] =
    if (idx > events.length || idx < 0)
      None
    else
      Option(events(idx))
}

object ZEpollEvents {

  private def fillArray(arr: Array[ZEpollEvent], mem: Memory): Array[ZEpollEvent] = {
    val size = arr.length

    @tailrec
    def fillRec(cnt: Int)(acc: Array[ZEpollEvent]): Array[ZEpollEvent] =
      cnt match {
        case i if i < 0 => acc
        case i          =>
          acc(i) = new ZEpollEvent(mem.share(i * size))
          fillRec(i - 1)(acc)
      }
    fillRec(size)(arr)
  }

  def createEvents(size: Int): ZIO[ZEnv, IOException, ZEpollEvents] = {
    if (size < 1)
      ZIO.fail(new IllegalArgumentException("The number of epoll events must be >= 1"))
    else
      for {
        mem    <- alloc(size).tap(mem => ZIO.effect(mem.clear()))
        events <- ZIO.effect(new Array[ZEpollEvent](size))
      } yield ZEpollEvents(fillArray(events, mem), mem)
  }.refineToOrDie[IOException]
}
