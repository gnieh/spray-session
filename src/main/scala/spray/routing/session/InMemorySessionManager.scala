/*
* Copyright © 2014 spray-session
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package spray
package routing
package session

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{
  ActorSystem,
  Actor,
  Props,
  Cancellable,
  PoisonPill
}
import akka.pattern.ask
import akka.util.Timeout

import java.util.concurrent.TimeUnit

import http.{
  DateTime,
  HttpCookie
}

import com.typesafe.config.Config

/** Session manager that stores the session in memory
 *  If a finite session timeout is given, it defines the maximum amount of time a session
 *  is valid without having been accessed. If it is infinite, sessions stay active indefinitely.
 *  Be careful though with infinite timeout, because, no cleanup can be performed
 *  automatically. You must then make sure that you do it properly to avoid memory leaks.
 *
 *  @author Lucas Satabin
 */
class InMemorySessionManager[T](config: Config)(implicit system: ActorSystem, timeout: Timeout) extends SessionManager[T](config) {

  private val manager = system.actorOf(Props(new ManagerActor))

  private class ManagerActor extends Actor {

    def receive = running(Map())

    private var cancellable: Option[Cancellable] = None

    private def restamp =
      DateTime.now + sessionTimeout.toMillis

    override def preStart(): Unit = {
      super.preStart()
      // schedule cleanup if needed
      sessionTimeout match {
        case finite: FiniteDuration =>
          import system.dispatcher
          cancellable = Some(system.scheduler.schedule(finite, finite, self, Cleanup))
        case _ =>
          // do not schedule cleanup task
      }
    }

    override def postStop(): Unit = {
      super.postStop()
      cancellable.foreach(_.cancel())
      cancellable = None
    }

    def running(sessions: Map[String, Session]): Receive = {
      case Start =>
        val id = newSid
        val expires =
          if(sessionTimeout.isFinite)
            Some(restamp)
          else
            None
        context.become(running(sessions + (id -> Session(Map(), expires))))
        sender ! id

      case Get(id) =>
        sessions.get(id) match {
          case Some(sess @ Session(map, Some(expires))) if expires > DateTime.now =>
            // expiration is in the future, return the map
            context.become(running(sessions.updated(id, Session(map, Some(restamp)))))
            sender ! Some(map)

          case Some(Session(map, None)) =>
            // no expiration date
            sender ! Some(map)

          case None | Some(_) =>
            // unknown session or expired session
            sender ! None
        }

      case Cookify(id) =>
        sessions.get(id) match {
          case Some(sess @ Session(_, expires)) if expires.map(_ > DateTime.now).getOrElse(true) =>
            sender ! HttpCookie(name = cookieName, content = id, expires = expires)
          case None | Some(_) =>
            // unknown session or expired session
            sender ! HttpCookie(name = cookieName, content = "", maxAge = Some(-1))

        }

      case Update(id, map) =>
        sessions.get(id) match {
          case Some(Session(_, Some(expires))) if expires > DateTime.now =>
            // only update a session if it exists and is valid
            context.become(running(sessions.updated(id, Session(map, Some(restamp)))))
            sender ! ()

          case Some(Session(_, None)) =>
            // no expiration, always valid
            context.become(running(sessions.updated(id, Session(map, None))))
            sender ! ()

          case None | Some(_) =>
            // unknown or expired session
            sender ! akka.actor.Status.Failure(new NoSuchElementException(s"unknown or expired session $id"))

        }

      case Invalidate(id) =>
        // instantaneously invalidate the given session id
        context.become(running(sessions - id))
        sender ! ()

      case Cleanup =>
        // remove expired sessions
        val (valid, expired) = sessions.partition {
          case (_, Session(_, Some(expires))) =>
            expires > DateTime.now
          case (_, _) =>
            true
        }
        // continue with valid sessions only
        context.become(running(valid))
        // invalidate other ones
        for((id, _) <- expired)
          self ! Invalidate(id)

    }

  }

  private case object Start
  private case class Get(id: String)
  private case class Update(id: String, map: Map[String, T])
  private case class Invalidate(id: String)
  private case class Cookify(id: String)
  private case object Cleanup

  import system.dispatcher

  def start(): Future[String] =
    (manager ? Start).mapTo[String]

  def isValid(id: String): Future[Boolean] =
    get(id).map(_.isDefined)

  def get(id: String): Future[Option[Map[String, T]]] =
    (manager ? Get(id)).mapTo[Option[Map[String, T]]]

  def update(id: String, map: Map[String, T]): Future[Unit] =
    (manager ? Update(id, map)).mapTo[Unit]

  def invalidate(id: String): Future[Unit] =
    (manager ? Invalidate(id)).mapTo[Unit]

  def cookify(id: String): Future[HttpCookie] =
    (manager ? Cookify(id)).mapTo[HttpCookie]

  def shutdown(): Unit =
    manager ! PoisonPill

}
