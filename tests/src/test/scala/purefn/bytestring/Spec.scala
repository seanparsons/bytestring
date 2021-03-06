package purefn.bytestring

import scalaz._
import scalaz.effect.IO

import org.specs2.matcher._
import org.specs2.mutable.FragmentsBuilder
import org.specs2.specification.{Example, Fragments, BaseSpecification, SpecificationStructure}
import org.specs2.main.{ArgumentsShortcuts, ArgumentsArgs}
import org.scalacheck.{Gen, Arbitrary, Prop, Properties, Shrink}

/** A minimal version of the Specs2 mutable base class */
trait Spec
  extends BaseSpecification 
  with FragmentsBuilder
  with MustExpectations
  with MustThrownExpectations 
  with ShouldThrownExpectations 
  with ExceptionMatchers
  with ScalaCheckMatchers
  with MatchersImplicits 
  with StandardMatchResults
  with ArgumentsShortcuts 
  with ArgumentsArgs {

  addArguments(fullStackTrace)

  def is = fragments

  addArguments(fullStackTrace)

  def be_===[T: Show : Equal](expected: T): Matcher[T] = new Matcher[T] {
    def apply[S <: T](actual: Expectable[S]): MatchResult[S] = {
      val actualT = actual.value.asInstanceOf[T]
      def test = Equal[T].equal(expected, actualT)
      def koMessage = "%s !== %s".format(Show[T].shows(actualT), Show[T].shows(expected))
      def okMessage = "%s === %s".format(Show[T].shows(actualT), Show[T].shows(expected))
      Matcher.result(test, okMessage, koMessage, actual)
    }
  }

//   override implicit val defaultParameters = set((minTestsOk, 1000))
  override implicit val defaultParameters = display((minTestsOk, 1000))

  def checkAll(name: String, props: Properties)(implicit p: Parameters) {
    addFragments(name + " " + props.name,
      for ((name, prop) <- props.properties) yield { name in check(prop)(p)}
      , "must satisfy"
    )
    ()
  }

  def checkAll(props: Properties)(implicit p: Parameters) {
    addFragments(props.name,
      for ((name, prop) <- props.properties) yield { name in check(prop)(p)}
      , "must satisfy"
    )
    ()
  }

  implicit def ioResultToProp[A](io: => IO[MatchResult[A]]): Prop = io.unsafePerformIO

  implicit def enrichProperties(props: Properties) = new {
    def withProp(propName: String, prop: Prop) = new Properties(props.name) {
      for {(name, p) <- props.properties} property(name) = p
      property(propName) = prop
    }
  }

  /**
   * Most of our scalacheck tests use (Int => Int). This generator includes non-constant
   * functions (id, inc), to have a better chance at catching bugs.
   */
  implicit def Function1IntInt[A](implicit A: Arbitrary[Int]): Arbitrary[Int => Int] =
    Arbitrary(Gen.frequency[Int => Int](
      (1, Gen.value((x: Int) => x)),
      (1, Gen.value((x: Int) => x + 1)),
      (3, A.arbitrary.map(a => (_: Int) => a))
    ))
}
