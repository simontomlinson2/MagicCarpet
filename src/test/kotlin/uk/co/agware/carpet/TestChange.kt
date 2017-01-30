package uk.co.agware.carpet

import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import uk.co.agware.carpet.change.Change
import uk.co.agware.carpet.exception.MagicCarpetParseException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@RunWith(JUnitPlatform::class)
class TestChange: SubjectSpek<Change>({

    describe("A Change object") {

        subject { Change("1.0.0") }

        it("should equal a Change with the same version") {
            assertEquals(subject, Change("1.0.0"))
        }

        it("should not equal a Change with a different version") {
            assertNotEquals(subject, Change("2.0.0"))
        }

        it("should fail when a supplied version does not match SemVer") {
            assertFailsWith<MagicCarpetParseException> {
                Change("1")
            }
        }

        it("should fail when a supplied version contains invalid characters") {
            assertFailsWith<MagicCarpetParseException> {
                Change("1.d.0")
            }
        }

        // TODO Don't forget we're in Kotlin, "assertTrue { subject < Change("1.1.1") }" actually works
        // TODO when you implement compareTo and makes the tests nicer to read
        it("should order changes using the version number") {
            assertEquals(subject.compareTo(Change("1.1.1")), -1)
        }

        // TODO If you're going to be checking HashCode, you need to check that the Hash
        // TODO of two identical objects is also identical, checking that it creates a HashCode
        // TODO just shows a total lack of understanding of what you're actually testing
        it("should generate a hashcode for the change version") {
            assertEquals(subject.hashCode(), "1.0.0".hashCode())
        }
    }

 })
