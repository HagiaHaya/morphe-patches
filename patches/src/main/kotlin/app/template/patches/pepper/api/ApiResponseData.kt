package app.template.patches.pepper.api

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext

/**
 * Lets the extension read or change the data of each response of the REST API, such as a list
 * of deals, before the app uses it. Several patches can add a method.
 *
 * @param extensionMethod Static method of the extension that takes the data and the additional data,
 * such as the listing context of a list, and returns the data.
 */
internal fun BytecodePatchContext.processApiResponseData(extensionMethod: String) {
    ApiSuccessDeserializationConstructorFingerprint.method.addInstructions(
        0,
        """
            invoke-static/range { p2 .. p3 }, $extensionMethod(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p2
        """
    )
}
