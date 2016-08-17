package ejisan.play.libs.scalauth

import play.api.mvc.{ Request, WrappedRequest }

trait SecureRequest[A] extends WrappedRequest[A] {
  def isSuccess: Boolean
  def isFailure: Boolean = ! isSuccess
  override def toString: String = s"SecureRequest(${SecureRequest.unapply(this).get.toString})"
}

object SecureRequest {
  def apply[A](request: Request[A]): UnauthorizedRequest[A] =
    new UnauthorizedRequest(request)

  def apply[A, S](request: Request[A], optSubject: Option[S]): AuthenticatedRequest[A, S] =
    new AuthenticatedRequest(request, optSubject)

  def apply[A, S](request: Request[A], subject: S): AuthenticatedRequest[A, S] =
    new AuthenticatedRequest(request, Some(subject))

  def anonymous[A](request: Request[A]): AuthenticatedRequest[A, _] = apply(request, None)

  def unapply[A](request: SecureRequest[A]): Option[(Request[A])]
    = Some((request.map(r => r)))
}

class AuthenticatedRequest[A, S](request: Request[A], subject: Option[S])
  extends WrappedRequest[A](request) with SecureRequest[A] {
  def isSuccess: Boolean = true
  def isAnonymous: Boolean = subject.isEmpty
  def get: S = subject.get
  override def toString: String = s"AuthenticatedRequest(${request.toString}, ${subject.map(_.toString).getOrElse("Anonymous")})"
}

class UnauthorizedRequest[A](request: Request[A]) extends WrappedRequest[A](request) with SecureRequest[A] {
  def isSuccess: Boolean = false
  override def toString: String = s"UnauthorizedRequest(${request.toString})"
}
