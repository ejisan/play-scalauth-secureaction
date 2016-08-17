package ejisan.play.libs.scalauth

import scala.language.higherKinds
import scala.concurrent.{ Future, ExecutionContext }
import play.api.mvc._

/**
 * A builder for generic SecureActions that generalizes over the type of requests.
 * An SecureActionFunction[S,R,PS,PF] may be chained onto an existing SecureActionBuilder[S,R]
 * to produce a new ActionBuilder[S,PS] or a new ActionBuilder[S,PF] using andThen.
 * The critical (abstract) function is `invokeEither` that invokes either success or failure block.
 * Most users will want to use ActionBuilder instead.
 *
 * @tparam S the type of the authentication subject
 * @tparam R the type of the request on which this is invoked (input)
 * @tparam PS the parameter type which blocks that succeeds in authentication executed by this builder take (output)
 * @tparam PF the parameter type which blocks that fails in authentication executed by this builder take (output)
 */
trait SecureActionFunction[-R[_], +PS[_] <: ({type R[A] = AuthenticatedRequest[A, _]})#R[_], +PF[_] <: UnauthorizedRequest[_]]
  extends ActionFunction[R, PS] { self =>

  final def invokeBlock[A](request: R[A], block: PS[A] => Future[Result]): Future[Result] =
    invokeEither(request, block, None)

  /**
   * Invoke the block. This is the main method that an SecureActionBuilder has to implement, at this stage it can wrap it in
   * any other actions, modify the request object or potentially use a different class to represent the request.
   *
   * @param request The request
   * @param success The block of code to invoke that succeeds in authentication
   * @param failure The block of code to invoke that fails in authentication
   * @return A future of the result
   */
  def invokeEither[A](
    request: R[A],
    success: PS[A] => Future[Result],
    failure: Option[PF[A] => Future[Result]]): Future[Result]

  /**
   * Compose this SecureActionFunction with another, with this one applied first.
   *
   * @param other SecureActionFunction with which to compose
   * @return The new SecureActionFunction
   */
  def andThen[P[_] <: SecureRequest[_], QS[_] <: AuthenticatedRequest[_, _], QF[_] <: UnauthorizedRequest[_]](
    other: SecureActionFunction[P, QS, QF]): SecureActionFunction[R, QS, QF] =
    new SecureActionFunction[R, QS, QF] {
      def invokeEither[A](
        request: R[A],
        success: QS[A] => Future[Result],
        failure: Option[QF[A] => Future[Result]]): Future[Result] = self.invokeEither[A](
          request,
          r => other.invokeEither[A](r.asInstanceOf[P[A]], success, failure),
          Some(r => other.invokeEither[A](r.asInstanceOf[P[A]], success, failure)))
    }

  /**
   * Compose this SecureActionFunction with others, with this one applied first.
   *
   * @param success SecureActionFunction with which to compose
   * @param failure SecureActionFunction with which to compose
   * @return The new SecureActionFunction
   */
  def andThen[QS[_] <: AuthenticatedRequest[_, _], QF[_] <: UnauthorizedRequest[_]](
    success: SecureActionFunction[PS, QS, QF],
    failure: SecureActionFunction[PF, QS, QF]): SecureActionFunction[R, QS, QF] =
    new SecureActionFunction[R, QS, QF] {
      def invokeEither[A](
        request: R[A],
        s: QS[A] => Future[Result],
        f: Option[QF[A] => Future[Result]]): Future[Result] =
        self.invokeEither[A](request, success.invokeEither[A](_, s, f), Some(failure.invokeEither[A](_, s, f)))
    }

  /**
   * Compose another ActionFunction with this one, with this one applied last.
   *
   * @param other ActionFunction with which to compose
   * @return The new ActionFunction
   */
  def compose[P[_] <: SecureRequest[_], QS[_] <: AuthenticatedRequest[_, _], QF[_] <: UnauthorizedRequest[_]](
    other: SecureActionFunction[P, QS, QF]): SecureActionFunction[P, PS, PF] = other.andThen(this)
}

/**
 * Provides helpers for creating `Action` values.
 */
trait SecureActionBuilder[RS[_] <: ({type R[A] = AuthenticatedRequest[A, _]})#R[_], RF[_] <: UnauthorizedRequest[_]]
  extends SecureActionFunction[Request, RS, RF] with ActionBuilder[RS] {

  /**
   * Constructs an `Action` with default content, and no request parameter for failure action.
   *
   * For example:
   * {{{
   * val secure = SecureAction(
   *   request => Ok("Got request [" + request + "]"),
   *   Unauthorized("You need authentication"))
   * }}}
   *
   * @param the success action code
   * @param the failure action code
   * @return an action
   */
  final def apply(
    success: RS[AnyContent] => Result,
    failure: => Result): Action[AnyContent] = apply(success, _ => failure)

  /**
   * Constructs an `Action` with default content.
   *
   * For example:
   * {{{
   * val secure = SecureAction(
   *   request => Ok("Got request [" + request + "]"),
   *   request => Unauthorized("You need authentication [" + request + "]"))
   * }}}
   *
   * @param the success action code
   * @param the failure action code
   * @return an action
   */
  final def apply(
    success: RS[AnyContent] => Result,
    failure: RF[AnyContent] => Result): Action[AnyContent] =
    apply(BodyParsers.parse.default)(success, failure)

  /**
   * Constructs an `Action`.
   *
   * For example:
   * {{{
   * val secure = SecureAction(parse.anyContent)(
   *   request => Ok("Got request [" + request + "]"),
   *   request => Unauthorized("You need authentication [" + request + "]"))
   * }}}
   *
   * @tparam A the type of the request body
   * @param bodyParser the `BodyParser` to use to parse the request body
   * @param the success action code
   * @param the failure action code
   * @return an action
   */
  final def apply[A](bodyParser: BodyParser[A])(
    success: RS[A] => Result,
    failure: RF[A] => Result): Action[A] =
    async(bodyParser)(r => Future.successful(success(r)), r => Future.successful(failure(r)))

  /**
   * Constructs an `Action` that returns a future of a result, with default content, and
   * no request parameter for failure action.
   *
   * For example:
   * {{{
   * val secure = SecureAction.async(
   *   request => WS.url("http://www.playframework.com").get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   },
   *   Future.successful(Unauthorized("You need authentication")))
   * }}}
   *
   * @param the success action code
   * @param the failure action code
   * @return an action
   */
  final def async(
    success: RS[AnyContent] => Future[Result],
    failure: => Future[Result]): Action[AnyContent] = async(success, _ => failure)

  /**
   * Constructs an `Action` that returns a future of a result, with default content.
   *
   * For example:
   * {{{
   * val secure = SecureAction.async(
   *   request => WS.url("http://www.playframework.com").get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   },
   *   request => Future.successful(Unauthorized("You need authentication [" + request + "]")))
   * }}}
   *
   * @param the success action code
   * @param the failure action code
   * @return an action
   */
  final def async(
    success: RS[AnyContent] => Future[Result],
    failure: RF[AnyContent] => Future[Result]): Action[AnyContent] =
    async(BodyParsers.parse.default)(success, failure)

  /**
   * Constructs an `Action` that returns a future of a result.
   *
   * For example:
   * {{{
   * val secure = SecureAction.async(parse.anyContent)(
   *   request => WS.url("http://www.playframework.com").get().map { r =>
   *     if (r.status == 200) Ok("The website is up") else NotFound("The website is down")
   *   },
   *   request => Future.successful(Unauthorized("You need authentication [" + request + "]")))
   * }}}
   *
   * @tparam A the type of the request body
   * @param bodyParser the `BodyParser` to use to parse the request body
   * @param the success action code
   * @param the failure action code
   * @return an action
   */
  final def async[A](bodyParser: BodyParser[A])(
    success: RS[A] => Future[Result],
    failure: RF[A] => Future[Result]): Action[A] = composeAction(new Action[A] {
    def parser = composeParser(bodyParser)
    def apply(request: Request[A]) = try {
      invokeEither(request, success, Some(failure))
    } catch {
      // NotImplementedError is not caught by NonFatal, wrap it
      case e: NotImplementedError => throw new RuntimeException(e)
      // LinkageError is similarly harmless in Play Framework, since automatic reloading could easily trigger it
      case e: LinkageError => throw new RuntimeException(e)
    }
    override def executionContext = SecureActionBuilder.this.executionContext
  })
}
