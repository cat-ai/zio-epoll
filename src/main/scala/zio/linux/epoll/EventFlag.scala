package zio.linux.epoll

sealed trait EventFlag {
  def value: Int
}

/**
 * Available for read operations.
 */
object EPOLLIN extends EventFlag {
  def value: Int = 0x0001
}

/**
 * Urgent data for read.
 */
object EPOLLPRI extends EventFlag {
  def value: Int = 0x0002
}

/**
 * Available for write operations.
 */
object EPOLLOUT extends EventFlag {
  def value: Int = 0x0004
}

/**
 * Priority data can be read.
 */
object EPOLLRDBAND extends EventFlag {
  def value: Int = 0x0080
}

/**
 * Priority data can be written.
 */
object EPOLLWRBAND extends EventFlag {
  def value: Int = 0x0200
}

/**
 * Error condition.
 * <p>
 * Always monitored even if not set explicitly.
 */
object EPOLLERR extends EventFlag {
  def value: Int = 0x0008
}

/**
 * Hang up, peer closed the channel.
 * <p>
 * Always monitored even if not set explicitly.
 */
object EPOLLHUP extends EventFlag {
  def value: Int = 0x0010
}

/**
 * Read hang up, peer closed its side of the channel for reading but can still receive data.
 */
object EPOLLRDHUP extends EventFlag {
  def value: Int = 0x2000
}

/**
 * Prevents hibernation, only to be used when really knowing what it does.
 */
object EPOLLWAKEUP extends EventFlag {
  def value: Int = 1 << 29
}

/**
 * One-shot behavior, meaning that after an event happens, it will not be monitored anymore.
 * file descriptor.
 */
object EPOLLONESHOT extends EventFlag {
  def value: Int = 1 << 30
}

/**
 * Edge Triggered behavior, only to be used when really knowing what it does.
 */
object EPOLLT extends EventFlag {
  def value: Int = 1 << 31
}