package com.shakster.gifcreator.util

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

abstract class InputTypeSerializer<T : Typed>(baseClass: KClass<T>) : JsonContentPolymorphicSerializer<T>(baseClass) {

    protected abstract val serializers: Map<String, DeserializationStrategy<T>>

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<T> {
        val inputType = try {
            element.jsonObject["type"]?.jsonPrimitive?.content
        } catch (t: Throwable) {
            throw IllegalArgumentException("Invalid input type in JSON:\n$element", t)
        }
        return serializers[inputType] ?: throw IllegalArgumentException("Unknown input type: $inputType")
    }
}

interface Typed {
    val type: String
}
