package app.template.patches.pepper.vote

import app.morphe.patcher.Fingerprint

/**
 * What to show when returning to the app from the shop a deal links to. Its name is kept,
 * because it is parcelable. The app only sets votePromptData, which asks to vote on the deal
 * ("How was this deal?"), for contributors below the Gold level:
 *
 * ```
 * ThreadClickoutReturnData(
 *     votePromptData = if (shouldPromptToVote(thread, isGoldOrPlatinum)) VotePromptData(...) else null,
 *     keywordAlertSuggestionsPromptData = ...,
 *     clickoutTimestampInMillis = ...,
 * )
 * ```
 */
internal object ThreadClickoutReturnDataConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/pepper/presentation/threadclickoutreturn/ThreadClickoutReturnData;",
    name = "<init>",
    parameters = listOf(
        "Lcom/pepper/presentation/bottomsheet/voteprompt/VotePromptData;",
        "L",
        "Ljava/lang/Long;",
    ),
)
