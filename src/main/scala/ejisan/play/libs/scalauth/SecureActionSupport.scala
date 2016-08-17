package ejisan.play.libs.scalauth

trait SecureActionSupport[S] {
  type SubjectAuthenticatedRequest[A] = AuthenticatedRequest[A, S]
  def SecureAction: SecureActionBuilder[SubjectAuthenticatedRequest, UnauthorizedRequest]
}
