package io.github.sds100.keymapper.util

import com.github.salomonbrys.kotson.contains
import com.github.salomonbrys.kotson.forEach
import com.github.salomonbrys.kotson.get
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert

/**
 * Created by sds100 on 25/01/21.
 */
object JsonTestUtils {
    private const val NAME_SEPARATOR = '/'

    fun compareBothWays(element: JsonElement, elementName: String, other: JsonElement, otherName: String) {
        compare("", element, elementName, other, otherName)
        compare("", other, elementName, element, elementName)
    }

    private fun compare(parentNamePath: String = "", element: JsonElement, elementName: String, rootToCompare: JsonElement, rootName: String) {
        when (element) {
            is JsonObject -> {
                element.forEach { name, jsonElement ->
                    val newPath = if (parentNamePath.isBlank()) {
                        name
                    } else {
                        "$parentNamePath$NAME_SEPARATOR$name"
                    }

                    compare(newPath, jsonElement, elementName, rootToCompare, rootName)
                }
            }

            is JsonArray -> {
                val pathToArrayToCompare = parentNamePath.split(NAME_SEPARATOR)
                var arrayToCompare: JsonArray? = null

                var parentElement: JsonElement = rootToCompare
                pathToArrayToCompare.forEach {
                    if (it == "") return@forEach

                    parentElement = parentElement[it]
                }

                if (parentElement is JsonArray) {
                    arrayToCompare = parentElement as JsonArray
                }

                Assert.assertNotNull("can't find array $elementName/$parentNamePath in $rootName", arrayToCompare)
                arrayToCompare ?: return

                element.forEachIndexed { index, arrayElement ->
                    val validIndex = index <= arrayToCompare.toList().lastIndex

                    assertThat("$rootName/${pathToArrayToCompare.last()} doesn't contain $arrayElement at $index index", validIndex)

                    compare("", arrayElement, "$elementName/${pathToArrayToCompare.last()}", arrayToCompare[index]!!, "$rootName/${pathToArrayToCompare.last()}")
                }
            }

            is JsonPrimitive -> {
                val names = parentNamePath.split(NAME_SEPARATOR)
                var parentElement: JsonElement = rootToCompare

                if (names == listOf("")) {
                    assertThat("$elementName/:$element doesn't match $rootName/:$parentElement", (parentElement), `is`(element))
                } else {
                    names.forEachIndexed { index, name ->
                        if (parentElement is JsonObject) {
                            assertThat("$elementName/$parentNamePath not found in $rootName", (parentElement as JsonObject).contains(name))
                        }

                        parentElement = parentElement[name]

                        if (index == names.lastIndex) {
                            assertThat("$elementName/$parentNamePath:$element doesn't match $rootName/$parentNamePath:$parentElement", (parentElement as JsonPrimitive), `is`(element))
                        }
                    }
                }
            }
        }
    }
}
