package zio.linux.epoll

import com.sun.jna.{Pointer, Structure}
import scala.jdk.CollectionConverters._

import java.util.{List => JList}
import java.io.IOException

import zio.{ZEnv, ZIO}

object ZNativeEpollEvent {

  protected[this] class EpollEventStruct(pointer: Pointer = Pointer.NULL) extends Structure(pointer) {

    val offsetEvents: Int   = super.fieldOffset( "events" )
    val offsetUserData: Int = super.fieldOffset("userData")
    val _size: Int          = super.size

    override protected def getFieldOrder: JList[String] = List("events", "userData").asJava
  }

  private val eventStruct: Option[Pointer] => EpollEventStruct = {
    case Some(pointer) => new EpollEventStruct(pointer)
    case None          => new EpollEventStruct()
  }

  def offsetEvents(ptr: Option[Pointer] = Some(Pointer.NULL)): Int = eventStruct(ptr).offsetEvents

  def offsetUserData(ptr: Option[Pointer] = Some(Pointer.NULL)): Int = eventStruct(ptr).offsetUserData

  def size(ptr: Option[Pointer] = Some(Pointer.NULL)): Int = eventStruct(ptr)._size

  def offsetEventsZIO(ptr: Option[Pointer] = Some(Pointer.NULL)): ZIO[ZEnv, IOException, Int] =
    ZIO.effect(offsetEvents(ptr)).refineToOrDie[IOException]

  def offsetUserDataZIO(ptr: Option[Pointer] = Some(Pointer.NULL)): ZIO[ZEnv, IOException, Int] =
    ZIO.effect(offsetEvents(ptr)).refineToOrDie[IOException]

  def sizeZIO(ptr: Option[Pointer] = Some(Pointer.NULL)) :ZIO[ZEnv, IOException, Int] =
    ZIO.effect(size(ptr)).refineToOrDie[IOException]
}
