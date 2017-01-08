package uk.co.agware.carpet.change;

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import uk.co.agware.carpet.change.tasks.Task


/**
 * Created by Simon on 29/12/2016.
 */
//TODO Ensure the version String follows the correct format of NUMBER.NUMBER.NUMBER...
//TODO Ensure the taskOrder string is either "" or an integer

class Change @JsonCreator constructor(@JsonProperty("version") val version: String, @JsonProperty("tasks") val tasks: List<Task>? ) : Comparable<Change> {

   fun buildVersionValue(version: String): Double{
        return version.split(".").map(String::toDouble)
                .reduceIndexed { i, acc, next -> acc + next / Math.pow(10.0, i.toDouble()) }
    }

    @Override
    override fun compareTo(other: Change): Int {
        val thisVersionValue = buildVersionValue(this.version)
        val oVersionValue = buildVersionValue(other.version)
        return thisVersionValue.compareTo(oVersionValue)
    }

    @Override
    override fun equals(other: Any?): Boolean {
        if (this == other) return true
        if (other !is Change) return false
        return this.version == other.version
    }

    @Override
    override fun hashCode(): Int {
        return this.version.hashCode()
    }

    @Override
    override fun toString(): String {
        val  sb = StringBuilder("Change{")
        sb.append("version='").append(this.version).append('\'')
        sb.append(", tasks=").append(this.tasks)
        sb.append('}')
        return sb.toString()
    }
}
