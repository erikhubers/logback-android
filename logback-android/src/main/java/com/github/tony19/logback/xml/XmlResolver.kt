package com.github.tony19.logback.xml

import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.anyName
import java.nio.charset.Charset
import java.util.*

class XmlResolver {
    fun <T> resolve(k: Konsumer, className: String): T {
        val inst = Class.forName(className).getDeclaredConstructor().newInstance()
        @Suppress("UNCHECKED_CAST")
        return resolve(k, inst) as T
    }

    fun resolve(k: Konsumer, inst: Any): Any {
        return inst.apply {
            val instMethods = inst.javaClass.methods
            k.children(anyName) {
                if (name?.localPart?.isNotEmpty()!!) {
                    val elemName = name?.localPart?.toLowerCase(Locale.US)
                    val setterMethod = instMethods
                            .find {
                                it.name.toLowerCase(Locale.US) in arrayOf("set${elemName}", "add${elemName}")
                                        && it.parameterTypes.size == 1 }

                    if (setterMethod == null) {
                        println("warning: no setter found for \"${name!!.localPart}\"")

                    } else {
                        val paramType = setterMethod.parameterTypes[0]
                        val value = when {
                            paramType.name == "java.lang.String" -> text()
                            paramType.name == "java.nio.charset.Charset" -> text { Charset.forName(it) }
                            paramType.isPrimitive -> text { parsePrimitive(paramType, it)!! }
                            else -> {
                                val paramInst = paramType.getDeclaredConstructor().newInstance()
                                resolve(this, paramInst).apply {
                                    // FIXME: We need to have LoggerContext set before calling start()
                                    //javaClass.methods.find { it.name == "start" }?.invoke(this)
                                }
                            }
                        }
                        setterMethod.invoke(inst, value)
                    }
                }
            }
        }
    }

    private val stringConverters by lazy {
        mapOf(
                "string" to String::toString,
                "byte" to String::toByte,
                "int" to String::toInt,
                "short" to String::toShort,
                "long" to String::toLong,
                "float" to String::toFloat,
                "double" to String::toDouble,
                "boolean" to String::toBoolean,
                "biginteger" to String::toBigInteger,
                "bigdecimal" to String::toBigDecimal
        )
    }

    private fun parsePrimitive(paramType: Class<*>, rawValue: String): Any? {
        return stringConverters[paramType.name.toLowerCase(Locale.US)]?.invoke(rawValue)
    }
}