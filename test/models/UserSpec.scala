/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.play.test.UnitSpec

class UserSpec extends UnitSpec {

  "Creating a User with only a VRN" should {

    val user = User("123456789")

    "have a VRN value the same as the constructor VRN" in {
      user.vrn shouldBe "123456789"
    }

    "be have an active status" in {
      user.active shouldBe true
    }
  }

  "Creating a User with a VRN and inactive status" should {

    val user = User("123456789", active = false)

    "have a VRN value the same as the constructor VRN" in {
      user.vrn shouldBe "123456789"
    }

    "be have an active status" in {
      user.active shouldBe false
    }
  }

  "containsNonMtdVAT" when {

    "a user with valid VATDEC and VATVAR enrolments" should {

      val enrolments = Set(
        Enrolment(
          "HMCE-VATDEC-ORG",
          Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
          "Activated"
        ),
        Enrolment(
          "HMCE-VATVAR-ORG",
          Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
          "Activated"
        )
      )

      "return true" in {
        User.containsNonMtdVat(enrolments) shouldBe true
      }

    }

    "a user with only one 'old vat' enrolment" should {
      val enrolments = Set(
        Enrolment(
          "HMCE-VATDEC-ORG",
          Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
          "Activated"
        )
      )

      "return true" in {
        User.containsNonMtdVat(enrolments) shouldBe true
      }
    }

    "a user with only a valid MTD-VAT enrolment" should {

      val enrolments = Set(
        Enrolment(
          "HMRC-MTD-VAT",
          Seq(EnrolmentIdentifier("VRN", "123456789")),
          "Activated"
        )
      )

      "return false" in {
        User.containsNonMtdVat(enrolments) shouldBe false
      }
    }

  }

  "extractVatEnrolments" should {

    val enrolments = Enrolments(
      Set(
        Enrolment(
          "HMRC-MTD-VAT",
          Seq(EnrolmentIdentifier("VRN", "123456789")),
          "Activated"
        ),
        Enrolment(
          "HMCE-VATDEC-ORG",
          Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
          "Activated"
        ),
        Enrolment(
          "HMCE-VATVAR-ORG",
          Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
          "Activated"
        ),
        Enrolment(
          "HMRC-MTD-IT",
          Seq(EnrolmentIdentifier("SAUTR", "123456789")),
          "Activated"
        )
      )
    )

    val expectedEnrolments = Set(
      Enrolment(
        "HMRC-MTD-VAT",
        Seq(EnrolmentIdentifier("VRN", "123456789")),
        "Activated"
      ),
      Enrolment(
        "HMCE-VATDEC-ORG",
        Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
        "Activated"
      ),
      Enrolment(
        "HMCE-VATVAR-ORG",
        Seq(EnrolmentIdentifier("VATRegNo", "123456789")),
        "Activated"
      )
    )

    "only extract VAT enrolments" in {
      User.extractVatEnrolments(enrolments) shouldBe expectedEnrolments
    }

  }
}
