package app.template.patches.slack.shared

import app.morphe.patcher.patch.BytecodePatchContext

internal const val SINGLE_CLASS = "Lio/reactivex/rxjava3/core/Single;"

/**
 * @return The reference to a method of the Single class of RxJava. R8 changed their return types
 * to the classes of the operators, which differ between app versions.
 */
internal fun BytecodePatchContext.singleMethod(name: String, parameterType: String) =
    classDefBy(SINGLE_CLASS).methods.singleOrNull {
        it.name == name && it.parameterTypes.map(CharSequence::toString) == listOf(parameterType)
    }?.let { "$SINGLE_CLASS->$name($parameterType)${it.returnType}" }
        ?: throw IllegalStateException("Could not find $SINGLE_CLASS->$name($parameterType)")
