package com.littlekt.graphics.shader.generator.delegate

import com.littlekt.graphics.shader.generator.Instruction
import com.littlekt.graphics.shader.generator.InstructionType
import com.littlekt.graphics.shader.generator.type.Variable
import kotlin.reflect.KProperty

/**
 * @author Colton Daily
 * @date 11/25/2021
 */

class ConstructorDelegate<T : Variable>(
    private val v: T,
    private val initialValue: String? = null,
    genInitialValue: (() -> T)? = null,
) {
    private var define: Instruction
    private var defined: Boolean = false

    val lit: T
        get() {
            v.value = initialValue
            return v
        }

    init {
        val value = initialValue ?: genInitialValue?.invoke()?.value
        val definitionString = "${v.typeName} {def}${getInitializerExpr(value)}"
        define = Instruction(InstructionType.DEFINE, definitionString)
        v.builder.addInstruction(define)
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ConstructorDelegate<T> {
        v.value = property.name
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!defined) {
            define.result = define.result.replace("{def}", property.name)
            defined = true
        }
        return v
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (!defined) {
            define.result = define.result.replace("{def}", property.name)
            defined = true
        }
        v.builder.addInstruction(Instruction.assign(property.name, value.value))
    }

    private fun getInitializerExpr(initialValue: String?): String {
        return if (initialValue == null) "" else " = $initialValue"
    }
}