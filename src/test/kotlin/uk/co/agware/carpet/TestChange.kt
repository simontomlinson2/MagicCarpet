package uk.co.agware.carpet

import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.it
import uk.co.agware.carpet.change.Change
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestChange:  SubjectSpek<Change>({
    subject { Change("1") }

    it("should equal a change with the same version") {
        assertTrue(subject.equals(Change("1")))
    }

    it("should not equal a change with a different version") {
        assertFalse(subject.equals(Change("2")))
    }

 })