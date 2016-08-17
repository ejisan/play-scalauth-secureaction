// package ejisan.scalauth.play

// import scala.language.higherKinds
// import scala.concurrent.{ Future, ExecutionContext }
// import play.api.mvc._
// import play.api.http.HeaderNames.AUTHORIZATION
// import ejisan.scalauth.simple.Subject
// import ejisan.scalauth.simple.errors._
// import ejisan.scalauth.simple.util.BearerToken
// import ejisan.scalauth.simple.token.Token68

// trait BearerSecureAction[S <: Subject[_]]
//   extends SecureActionBuilder[S, ({type R[A] = SecureRequest[A, S]})#R] {

//   def realm: Future[String]

//   def authentication(token: Token68): Future[Option[S]]

//   def onUnauthenticated[A](request: Request[A]): PartialFunction[ScalauthError, Future[Result]]

//   def invokeBlock[A](
//     request: Request[A],
//     onAuthenticated: SecureRequest[A, S] => Future[Result],
//     onUnauthenticated: Request[A] => PartialFunction[ScalauthError, Future[Result]]): Future[Result]
//     = (request.headers.get(AUTHORIZATION) match {
//       case Some(BearerToken(token)) => Some(token)
//       case None =>
//         (try {
//           request.body.asInstanceOf[AnyContent].asFormUrlEncoded
//             .flatMap(_.get("access_token").map(_.headOption))
//             .getOrElse(request.queryString.get("access_token").flatMap(_.headOption))
//         } catch {
//           case e: Throwable => None
//         }).map(Token68.fromToken68String(_))
//       case _ => None
//     }) match {
//       case Some(token) =>
//         authentication(token).flatMap({
//           case subject if subject.isDefined => onAuthenticated(SecureRequest(request, subject))
//           case _ => errorHandler(request, onUnauthenticated, SubjectNotFoundError())
//         })(executionContext)
//       case None => errorHandler(request, onUnauthenticated, InvalidRequestError())
//       case _ => throw new RuntimeException("`Authorization` header in request is invalid.")
//     }
// }
