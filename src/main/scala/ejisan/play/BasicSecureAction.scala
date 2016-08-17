// package ejisan.scalauth.play

// import scala.language.higherKinds
// import scala.concurrent.{ Future, ExecutionContext }
// import play.api.mvc._
// import play.api.http.HeaderNames.{ AUTHORIZATION, WWW_AUTHENTICATE }
// import play.api.http.Status.UNAUTHORIZED
// import ejisan.scalauth.simple.Subject
// import ejisan.scalauth.simple.util.BasicAuth
// import ejisan.scalauth.simple.errors._

// trait BasicSecureAction[S <: Subject[_]]
//   extends SecureActionBuilder[S, ({type T[A] = SecureRequest[A, S]})#T] {

//   def realm: Future[String]

//   def authentication(username: String, password: String): Future[Option[S]]

//   def onUnauthenticated[A](request: Request[A]): PartialFunction[ScalauthError, Future[Result]]

//   def invokeBlock[A](
//     request: Request[A],
//     onAuthenticated: SecureRequest[A, S] => Future[Result],
//     onUnauthenticated: Request[A] => PartialFunction[ScalauthError, Future[Result]]): Future[Result]
//     = (request.headers.get(AUTHORIZATION) match {
//       case Some(BasicAuth(username, password)) =>
//         authentication(username, password).flatMap({
//           case subject if subject.isDefined => onAuthenticated(SecureRequest(request, subject))
//           case _ => errorHandler(request, onUnauthenticated, SubjectNotFoundError())
//         })(executionContext)
//       case None => errorHandler(request, onUnauthenticated, InvalidRequestError())
//       case _ => throw new RuntimeException("`Authorization` header in request is invalid.")
//     }).flatMap({ result =>
//       if (result.header.status == UNAUTHORIZED && result.header.headers.exists(_._1 == WWW_AUTHENTICATE)) {
//         realm.map(realm => result.withHeaders(BasicAuth.unauthorized(realm)))(executionContext)
//       } else Future.successful(result)
//     })(executionContext)
// }
