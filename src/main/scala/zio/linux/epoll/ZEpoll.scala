package zio.linux.epoll

import com.sun.jna._

import java.io.IOException

import zio._
import zio.linux._

import Epoll.FdOperation._

import ZEpoll._

final case class Epoll protected[epoll](fd: Int,
                                        closedRef: UIO[Ref[Boolean]] = Ref.make(false)) { self =>

  @inline
  val failGenericOpZIO: Int => ZIO[ZEnv with LinuxIO, IOException, Unit] = {
    case Error.EBADF  => ZIO.fail(new IOException("Given file descriptor is invalid"))
    case Error.ENOMEM => ZIO.fail(new IOException("Kernel has insufficient memory for acting on file descriptor"))
    case _            => ZIO.unit
  }

  @inline
  val failOpZIO: Int => ZIO[ZEnv with LinuxIO, IOException, Unit] = {
    case Error.EEXIST => ZIO.fail(new IOException("Given file descriptor has already been added"))
    case Error.ENOSPC => ZIO.fail(new IOException("Limit of maximum user epoll watches reached"))
    case Error.EPERM  => ZIO.fail(new IOException("Given file descriptor does not support epoll"))
    case otherError   => ZIO.fail(new IOException("Native error while adding file descriptor: " + otherError))
  }

  @inline
  val handleLastError: Int => ZIO[ZEnv with LinuxIO, IOException, Unit] = errNum =>
    for {
      linuxErr <- lastError
      err      <- failGenericOpZIO(linuxErr) *> {
                    if (linuxErr == errNum)
                      ZIO.fail(new IOException("Unable to remove file descriptor which has not been added"))
                    else
                      ZIO.fail(new IOException("Native error while removing file descriptor: " + linuxErr))
                  }
    } yield err

  private lazy val close: ZIO[ZEnv with LinuxIO, IOException, Unit] =
    for {
      ref      <- closedRef
      closed   <- ref.set(true)
    } yield closed

  def isEpollClosed: ZIO[ZEnv with LinuxIO, IOException, Boolean] =
    for {
      ref      <- closedRef
      isClosed <- ref.get
    } yield isClosed

  private def failClosedZIO: ZIO[ZEnv with LinuxIO, IOException, Unit] =
    ZIO.fail(new IOException("Cannot perform operation on a closed epoll instance"))

  def add(fd: Int, event: ZEpollEvent): ZIO[ZEnv with LinuxIO, IOException, Epoll] =
    isEpollClosed >>= {
      case false =>
        if (fd == self.fd)
          ZIO.fail(new IOException("Given file descriptor cannot be the same as the file descriptor of this epoll instance")).refineToOrDie[IOException]
        else
          epollCtlZIO(self.fd, EPOLL_CTL_ADD, fd, event.pointer) >>= {
            case op if op < 0 => close *> failGenericOpZIO(op) *> failOpZIO(op).as(self)
            case _            => ZIO.effect(self.copy()).refineToOrDie[IOException]
          }

      case true  => failClosedZIO.as(self)
    }

  def delete(fd: Int): ZIO[ZEnv with LinuxIO, IOException, Epoll] =
    isEpollClosed >>= {
      case false =>
        epollCtlZIO(self.fd, EPOLL_CTL_DEL, fd, Pointer.NULL) >>= {
          case op if op < 0 => close *> handleLastError(Error.ENOENT).as(self)
          case _            => ZIO.effect(self.copy()).refineToOrDie[IOException]
        }

      case true  => failClosedZIO.as(self)
    }

  def mod(fd: Int, event: ZEpollEvent): ZIO[ZEnv with LinuxIO, IOException, Epoll] = {
    isEpollClosed >>= {
      case false =>
        epollCtlZIO(self.fd, EPOLL_CTL_MOD, fd, event.pointer) >>= {
          case op if op < 0 =>  close *> handleLastError(Error.ENOENT).as(self)
          case _            =>  ZIO.effect(self.copy()).refineToOrDie[IOException]
        }
      case true  => failClosedZIO.as(self)
    }
  }

  def wait(event: ZEpollEvent): ZIO[ZEnv with LinuxIO, IOException, Unit] =
    wait(event, -1) map(_ => ())

  def wait(events: ZEpollEvent, timeout: Int): ZIO[ZEnv with LinuxIO, IOException, Boolean] =
    wait(events.pointer, 1, timeout) map(_ < 0)

  def wait(events: ZEpollEvents): ZIO[ZEnv with LinuxIO, IOException, Int] =
    wait(events, -1)

  def wait(events: ZEpollEvents, timeout: Int): ZIO[ZEnv with LinuxIO, IOException, Int] =
    wait(events.memory, events.events.length, timeout)

  def wait(events: Pointer,
           maxEvents: Int,
           timeout: Int): ZIO[ZEnv with LinuxIO, IOException, Int] =
    (isEpollClosed >>= {
      case false =>
        epollWaitZIO(self.fd, events, maxEvents, timeout) >>= {
          case _      < 0 => close *> lastError >>= { errNum => ZIO.fail(new IOException("Native error while waiting for an epoll event: " + errNum)) }
          case _     == 0 =>
            close *> {
              if (timeout < 0)
                ZIO.fail(new IOException("Epoll unexpectedly unblocked before an event occured"))
              else
                ZIO.fail(new IOException("Native error while waiting for an epoll event"))
            }
          case result     => ZIO.effect(result)
        }

      case true  => failClosedZIO
    }).refineToOrDie[IOException]
}

object Epoll {

  object FdOperation {
    val EPOLL_CTL_ADD = 1
    val EPOLL_CTL_DEL = 2
    val EPOLL_CTL_MOD = 3
  }

  object EventTypes {
    val EPOLLIN       = 0x001.toInt
    val EPOLLPRI      = 0x002.toInt
    val EPOLLOUT      = 0x004.toInt
    val EPOLLRDNORM   = 0x040.toInt
    val EPOLLRDBAND   = 0x080.toInt
    val EPOLLWRNORM   = 0x100.toInt
    val EPOLLWRBAND   = 0x200.toInt
    val EPOLLMSG      = 0x400.toInt
    val EPOLLERR      = 0x008.toInt
    val EPOLLHUP      = 0x010.toInt
    val EPOLLRDHUP    = 0x2000.toInt
    val EPOLLWAKEUP   = (1 << 29).toInt
    val EPOLLONESHOT  = (1 << 30).toInt
    val EPOLLET       = (1 << 31).toInt
  }
}

protected[epoll] object ZEpoll {

  protected lazy val load = Native.register("c")

  @native
  private def epoll_create(size: Int): Int

  @native
  private def epoll_ctl(epFd: Int, op: Int, fd: Int, event: Pointer): Int

  @native
  private def epoll_wait(epFd: Int, event: Pointer, maxEvents: Int, timeout: Int): Int

  def epollCreateZIO(size: Int): ZIO[ZEnv with LinuxIO, IOException, Epoll] =
    (for {
      fd    <- ZIO.effect(epoll_create(size))
      epoll <- if (fd < 0)
                 lastError >>= {
                   case Error.EMFILE =>
                     ZIO.fail(new IOException("Per-user limit on the number of epoll instances or per-process limit on the number of file descriptor has been reached"))
                   case Error.ENFILE =>
                     ZIO.fail(new IOException("System-wide limit on the number of file descriptors has been reached"))
                   case other        =>
                     ZIO.fail(new IOException("Native error while create epoll instance: " + other))
                 }
               else
                 ZIO.succeed(new Epoll(fd))
    } yield epoll).refineToOrDie[IOException]

  def epollCtlZIO(epFd: Int, op: Int, fd: Int, event: Pointer): ZIO[ZEnv with LinuxIO, IOException, Int] =
    ZIO.effect(epoll_ctl(epFd, op, fd, event)).refineToOrDie[IOException]

  def epollWaitZIO(epFd: Int, event: Pointer, maxEvents: Int, timeout: Int): ZIO[ZEnv with LinuxIO, IOException, Int] =
    ZIO.effect(epoll_wait(epFd, event, maxEvents, timeout)).refineToOrDie[IOException]
}