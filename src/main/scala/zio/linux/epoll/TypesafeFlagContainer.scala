package zio.linux.epoll

case class TypesafeFlagContainer(eventFlag: Int = 0) {

  def isEqualTo(flag: EventFlag): Boolean             = (eventFlag & flag.value) > 0

  def setTo(flag: EventFlag): TypesafeFlagContainer   = copy(eventFlag = eventFlag | flag.value)

  def unsetTo(flag: EventFlag): TypesafeFlagContainer = copy(eventFlag = eventFlag & ~flag.value)

  def clear: TypesafeFlagContainer                    = copy(eventFlag = 0)
}
