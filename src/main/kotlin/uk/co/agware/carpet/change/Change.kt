package uk.co.agware.carpet.change;

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import uk.co.agware.carpet.change.tasks.Task


/**
 * // TODO More detail, format comment better
 * Implement a change set
 * @constructor create the change with tasks
 * @property version the version of the change
 * @property tasks List of tasks for the change
 * Created by Simon on 29/12/2016.
 */
// TODO Too long again, split the constructor into multiple lines
class Change @JsonCreator constructor(@JsonProperty("version") val version: String, @JsonProperty("tasks") val tasks: List<Task>? ) : Comparable<Change> {

    /**
     * Build the version from a String
     * @param version
     */
    // TODO Format the code better
   fun buildVersionValue(version: String): Double{
        return version.split(".").map(String::toDouble)
                .reduceIndexed { i, acc, next -> acc + next / Math.pow(10.0, i.toDouble()) }
    }

    // TODO When it isn't immediately obvious, explain how the ordering is done
    @Override
    override fun compareTo(other: Change): Int {
        val thisVersionValue = buildVersionValue(this.version)
        val oVersionValue = buildVersionValue(other.version)
        return thisVersionValue.compareTo(oVersionValue)
    }

    @Override
    override fun equals(other: Any?): Boolean {
        if (other !is Change) return false
        return this.version == other.version
    }

    @Override
    override fun hashCode(): Int {
        return this.version.hashCode()
    }

  // TODO swap this to using string concat, the compiler and JVM will figure out the best way to implement it
    @Override
    override fun toString(): String {
        val  sb = StringBuilder("Change{")
        sb.append("version='").append(this.version).append('\'')
        sb.append(", tasks=").append(this.tasks)
        sb.append('}')
        return sb.toString()
    }
}
