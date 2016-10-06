package ejisan.play.libs.scalauth

import play.api.mvc.{ Result, RequestHeader }
import play.api.{ Configuration, Logger }

case class Session(id: String, impersonator: Option[String]) {
  def toSeq(SESSION_COOKIE_NAME: String, IMPERSONATING_SESSION_COOKIE_NAME: String): Seq[(String, String)] =
    Seq(SESSION_COOKIE_NAME -> Some(id), IMPERSONATING_SESSION_COOKIE_NAME -> impersonator).collect {
      case (key, Some(value)) => (key, value)
    }
}

class PlaySessionManager(configuration: Configuration) {
  val SESSION_COOKIE_NAME: String =
    configuration.getString("scalauth.play.secure_action.cookie_name").getOrElse("SESSION")
  val IMPERSONATING_SESSION_COOKIE_NAME: String =
    configuration.getString("scalauth.play.secure_action.impersonator_cookie_name").getOrElse("IMPERSONATOR")

  def set(session: Session)(result: => Result)(implicit request: RequestHeader): Result = {
    Logger.debug(s"Set session for ID: `${session.id}` and IMPERSONATOR_ID: `${session.impersonator.getOrElse("N/A")}`.")
    forceEnd(result).addingToSession(session.toSeq(SESSION_COOKIE_NAME, IMPERSONATING_SESSION_COOKIE_NAME):_*)
  }

  def get(implicit request: RequestHeader): Option[Session] =
    request.session.get(SESSION_COOKIE_NAME).map(Session(_, request.session.get(IMPERSONATING_SESSION_COOKIE_NAME)))

  def start(id: String)(result: => Result)(implicit request: RequestHeader): Result =
    set(Session(id, None))(result)

  def restart(result: => Result)(implicit request: RequestHeader): Result =
    get.map(set(_)(result)).getOrElse(result)

  def impersonate(id: String)(result: => Result)(implicit request: RequestHeader): Result = {
    val impersonator = try {
      request.session(SESSION_COOKIE_NAME)
    } catch {
      case _: NoSuchElementException =>
      val e = new IllegalStateException("Impersonator must start main session before impersonating session.")
        Logger.error("Main session not found", e)
        throw e
    }
    set(Session(id, Some(impersonator)))(result)
  }

  def end(result: => Result)(implicit request: RequestHeader): Result = {
    if (request.session.data.keySet.exists(_ == IMPERSONATING_SESSION_COOKIE_NAME)) {
      result.removingFromSession(IMPERSONATING_SESSION_COOKIE_NAME)
    } else if (request.session.data.keySet.exists(_ == SESSION_COOKIE_NAME)) {
      result.removingFromSession(SESSION_COOKIE_NAME)
    } else {
      result
    }
  }

  def forceEnd(result: => Result)(implicit request: RequestHeader): Result =
    result.removingFromSession(SESSION_COOKIE_NAME, IMPERSONATING_SESSION_COOKIE_NAME)
}
