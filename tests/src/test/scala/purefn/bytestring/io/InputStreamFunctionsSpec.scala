package purefn.bytestring
package io

import ByteString._
import syntax._
import scalacheck._

import scalaz._
import std.option._
import std.stream._
import syntax.std.stream._

import org.specs2.matcher._
import org.scalacheck._
import Gen.{choose, listOf, oneOf}
import Arbitrary.arbitrary
import scalaz.scalacheck.ScalazProperties._

import java.io._

class InputStreamFunctionsSpec extends Spec {
  import ByteStringSpecFunctions._
  import InputStreamFunctionsSpecData._

  "sGetContents" ! check { (x: ByteString) ⇒
    ByteStringInputStream(x).getContents.map(_ must be_=== (x))
  }

  "sGetStr" ! check { (x: ByteString, y: Short) ⇒
    ByteStringInputStream(x).getStr(y).map(_ must be_=== (x.take(y)))
  }

  "sGetLine buffered" ! check { (x: Lines) ⇒
    sGetLine(x.is).run.map(_ must be_=== (x.head))
  }

  "sGetLine unbuffered" ! check { (x: Lines) ⇒
    val is = new InputStream {
      val is_ = x.is
      override def read = is_.read
    }
    sGetLine(is).run.map(_ must be_=== (x.head))
  }

  "sGetLines" ! check { (x: Lines) ⇒
    sGetLines(x.is).toStream.map(_ must be_=== (x.packed))
  }
}

object InputStreamFunctionsSpecData {
  case class Lines(val xss: Stream[Stream[Byte]], nl: Boolean) {
    def is = {
      def addNl(xs: Stream[Byte]) = (xs :+ '\n'.toByte).toStream
      def yss = 
        if (xss.isEmpty) xss
        else if (nl) xss.map(addNl).toStream
        else xss.init.map(addNl).toStream :+ xss.last.toStream
      new ByteArrayInputStream(yss.flatten.toArray)
    }

    def head = xss match {
      case Stream.Empty                  ⇒ None
      case Stream.Empty #:: Stream.Empty ⇒ if (nl) Some(empty) else None
      case xs #:: _                      ⇒ Some(packF(xs))
    }

    def packed = {
      def yss = 
        if (!nl && !xss.isEmpty && xss.last.isEmpty) xss.init
        else xss
      yss.map(packF[Stream])
    }
  }

  implicit def arbLines: Arbitrary[Lines] = Arbitrary {
    def genNonNlByte = oneOf(choose(Byte.MinValue, '\n'.toByte - 1), choose('\n'.toByte + 1, Byte.MaxValue)).map(_.toByte)
    for {
      xss ← listOf(listOf(genNonNlByte))
      nl  ← arbitrary[Boolean]
    } yield Lines(xss.toStream.map(_.toStream), nl)
  }
}
