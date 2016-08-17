package ejisan.play.libs.scalauth

import play.api.mvc.{ Result, RequestHeader }
import play.api.{ Configuration, Logger }

class PlaySessionManager(configuration: Configuration) {
  val SESSION_COOKIE_NAME: String =
    configuration.getString("scalauth.play.secure_action.cookie_name").getOrElse("SESSION")
  val IMPERSONATING_SESSION_COOKIE_NAME: String =
    configuration.getString("scalauth.play.secure_action.impersonator_cookie_name").getOrElse("IMPERSONATOR")

  def start(id: String)(result: => Result)(implicit request: RequestHeader): Result = {
    Logger.debug(s"Start session for ID: `$id`.")
    result
      .removingFromSession(SESSION_COOKIE_NAME, IMPERSONATING_SESSION_COOKIE_NAME)
      .addingToSession(SESSION_COOKIE_NAME -> id)
  }

  def restart(result: => Result)(implicit request: RequestHeader): Result = {
    Logger.debug(s"Restart session.")
    result
      .removingFromSession(SESSION_COOKIE_NAME, IMPERSONATING_SESSION_COOKIE_NAME)
      .addingToSession(request.session.data.filterKeys(k => k == SESSION_COOKIE_NAME || k == IMPERSONATING_SESSION_COOKIE_NAME).toSeq:_*)
  }

  def impersonate(id: String)(result: => Result)(implicit request: RequestHeader): Result = {
    val impersonator = try {
      request.session(SESSION_COOKIE_NAME)
    } catch {
      case _: NoSuchElementException =>
        Logger.error("Impersonator must start session before impersonating.")
        throw new IllegalStateException("Impersonator must start session before impersonating.")
    }
    Logger.debug(s"Session `${request.session(SESSION_COOKIE_NAME)}` impersonates session `${request.session(IMPERSONATING_SESSION_COOKIE_NAME)}`.")
    result
      .removingFromSession(SESSION_COOKIE_NAME, IMPERSONATING_SESSION_COOKIE_NAME)
      .addingToSession(SESSION_COOKIE_NAME -> id, IMPERSONATING_SESSION_COOKIE_NAME -> impersonator)
  }

  def end(result: => Result)(implicit request: RequestHeader): Result = {
    if (request.session.data.keySet.exists(_ == IMPERSONATING_SESSION_COOKIE_NAME)) {
      Logger.debug(s"End session for IMPERSONATOR_ID: `${request.session(IMPERSONATING_SESSION_COOKIE_NAME)}`.")
      result.removingFromSession(IMPERSONATING_SESSION_COOKIE_NAME)
    } else if (request.session.data.keySet.exists(_ == SESSION_COOKIE_NAME)) {
      Logger.debug(s"End session for ID: `${request.session(SESSION_COOKIE_NAME)}`.")
      result.removingFromSession(SESSION_COOKIE_NAME)
    } else {
      Logger.debug(s"Nothing end session.")
      result
    }
  }

  def forceEnd(result: => Result)(implicit request: RequestHeader): Result = {
    Logger.debug(s"Force end sessions for ID: `${request.session.get(SESSION_COOKIE_NAME).getOrElse("N/A")}` and IMPERSONATOR_ID: `${request.session.get(IMPERSONATING_SESSION_COOKIE_NAME).getOrElse("N/A")}`.")
    result.removingFromSession(SESSION_COOKIE_NAME, IMPERSONATING_SESSION_COOKIE_NAME)
  }
}
