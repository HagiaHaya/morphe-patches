package app.template.patches.slack.shared

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/**
 * Renames [method] and adds a method with its former name and signature, so the new method can
 * call the original one. The methods of a class are kept in a set, so the method is removed
 * while its name, which is part of its hash, changes.
 */
internal fun MutableClass.wrapMethod(method: MutableMethod, originalName: String, registerCount: Int, smali: String) {
    val name = method.name
    methods.remove(method)
    method.name = originalName
    methods.add(method)

    methods.add(
        ImmutableMethod(
            method.definingClass,
            name,
            method.parameters.map { ImmutableMethodParameter(it.type, null, null) },
            method.returnType,
            method.accessFlags,
            null,
            null,
            MutableMethodImplementation(registerCount),
        ).toMutable().apply { addInstructions(0, smali) }
    )
}
