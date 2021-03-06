/*
 * Copyright (C) 2017 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.vsct.tock.bot.connector.ga

import emoji4j.EmojiUtils
import fr.vsct.tock.bot.connector.ConnectorMessage
import fr.vsct.tock.bot.connector.ConnectorType
import fr.vsct.tock.bot.connector.ga.model.GAIntent
import fr.vsct.tock.bot.connector.ga.model.request.GAPermission
import fr.vsct.tock.bot.connector.ga.model.response.GABasicCard
import fr.vsct.tock.bot.connector.ga.model.response.GAButton
import fr.vsct.tock.bot.connector.ga.model.response.GACarouselSelect
import fr.vsct.tock.bot.connector.ga.model.response.GAExpectedInput
import fr.vsct.tock.bot.connector.ga.model.response.GAExpectedIntent
import fr.vsct.tock.bot.connector.ga.model.response.GAFinalResponse
import fr.vsct.tock.bot.connector.ga.model.response.GAImage
import fr.vsct.tock.bot.connector.ga.model.response.GAInputPrompt
import fr.vsct.tock.bot.connector.ga.model.response.GAItem
import fr.vsct.tock.bot.connector.ga.model.response.GALinkOutSuggestion
import fr.vsct.tock.bot.connector.ga.model.response.GAListSelect
import fr.vsct.tock.bot.connector.ga.model.response.GAOpenUrlAction
import fr.vsct.tock.bot.connector.ga.model.response.GAOptionInfo
import fr.vsct.tock.bot.connector.ga.model.response.GAOptionValueSpec
import fr.vsct.tock.bot.connector.ga.model.response.GAPermissionValueSpec
import fr.vsct.tock.bot.connector.ga.model.response.GARichResponse
import fr.vsct.tock.bot.connector.ga.model.response.GASimpleResponse
import fr.vsct.tock.bot.connector.ga.model.response.GASimpleSelect
import fr.vsct.tock.bot.connector.ga.model.response.GAStructuredResponse
import fr.vsct.tock.bot.definition.IntentAware
import fr.vsct.tock.bot.definition.StoryHandlerDefinition
import fr.vsct.tock.bot.definition.StoryStep
import fr.vsct.tock.bot.engine.BotBus
import fr.vsct.tock.bot.engine.action.SendChoice
import fr.vsct.tock.translator.UserInterfaceType.textAndVoiceAssistant
import fr.vsct.tock.translator.UserInterfaceType.textChat
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal const val GA_CONNECTOR_TYPE_ID = "ga"

/**
 * The Google Assistant [ConnectorType].
 */
val gaConnectorType = ConnectorType(GA_CONNECTOR_TYPE_ID, textAndVoiceAssistant)

/**
 * Add a [ConnectorMessage] for Google Assistant.
 */
fun BotBus.withGoogleAssistant(messageProvider: () -> ConnectorMessage): BotBus {
    return withMessage(gaConnectorType, messageProvider)
}

/**
 * Add a [ConnectorMessage] for Google Assistant - voice only.
 */
fun BotBus.withGoogleVoiceAssistant(messageProvider: () -> ConnectorMessage): BotBus {
    if (userInterfaceType != textChat) {
        withMessage(gaConnectorType, messageProvider)
    }
    return this
}

/**
 * Final Google Assistant message (end of conversation).
 */
fun BotBus.gaFinalMessage(richResponse: GARichResponse): GAResponseConnectorMessage
        = GAResponseConnectorMessage(finalResponse = GAFinalResponse(richResponse))

/**
 * Final Google Assistant message (end of conversation).
 */
fun BotBus.gaFinalMessage(text: CharSequence? = null): GAResponseConnectorMessage =
        gaFinalMessage(
                if (text == null)
                //empty rich response
                    richResponse(emptyList())
                else richResponse(text)
        )


/**
 * Google Assistant Message with all available parameters.
 */
fun BotBus.gaMessage(inputPrompt: GAInputPrompt,
                     possibleIntents: List<GAExpectedIntent> = listOf(
                             expectedTextIntent()
                     ),
                     speechBiasingHints: List<String> = emptyList()): GAResponseConnectorMessage =
        GAResponseConnectorMessage(
                GAExpectedInput(
                        inputPrompt,
                        if (possibleIntents.isEmpty()) listOf(expectedTextIntent()) else possibleIntents,
                        speechBiasingHints)
        )

/**
 * Google Assistant Message with suggestions.
 */
fun BotBus.gaMessage(text: CharSequence, vararg suggestions: String): GAResponseConnectorMessage
        = if (suggestions.isEmpty()) gaMessage(inputPrompt(text)) else gaMessage(richResponse(text, *suggestions))

/**
 * Google Assistant Message with [GABasicCard].
 */
fun BotBus.gaMessage(text: CharSequence, basicCard: GABasicCard): GAResponseConnectorMessage
        = gaMessage(richResponse(listOf(GAItem(simpleResponse(text)), GAItem(basicCard = basicCard))))


/**
 * Google Assistant Message with [GARichResponse].
 */
fun BotBus.gaMessage(richResponse: GARichResponse): GAResponseConnectorMessage
        = gaMessage(inputPrompt(richResponse))

/**
 * Google Assistant Message with one [GAExpectedIntent].
 */
fun BotBus.gaMessage(possibleIntent: GAExpectedIntent): GAResponseConnectorMessage =
        gaMessage(listOf(possibleIntent))

/**
 * Google Assistant Message with multiple [GAExpectedIntent].
 */
fun BotBus.gaMessage(possibleIntents: List<GAExpectedIntent>): GAResponseConnectorMessage =
        gaMessage(inputPrompt(GARichResponse(emptyList())), possibleIntents)

/**
 * Google Assistant Message with text and multiple [GAExpectedIntent].
 */
fun BotBus.gaMessage(text: String, possibleIntents: List<GAExpectedIntent>): GAResponseConnectorMessage =
        gaMessage(
                inputPrompt(richResponse(text)),
                possibleIntents
        )

/**
 * Google Assistant Message asking for [GAPermission].
 */
fun BotBus.permissionIntent(optionalContext: CharSequence?, vararg permissions: GAPermission): GAExpectedIntent {
    return GAExpectedIntent(
            GAIntent.permission,
            GAPermissionValueSpec(
                    translate(optionalContext).toString(),
                    permissions.toSet()
            )
    )
}

/**
 * Provides a [GALinkOutSuggestion].
 */
fun BotBus.linkOutSuggestion(destinationName: CharSequence, url: String): GALinkOutSuggestion {
    val d = translate(destinationName)
    if (d.length > 20) {
        logger.warn { "title $d has more than 20 chars" }
    }
    return GALinkOutSuggestion(d.toString(), url)
}

/**
 * Provides a [GAItem] with all available parameters.
 */
fun BotBus.item(simpleResponse: GASimpleResponse? = null, basicCard: GABasicCard? = null, structuredResponse: GAStructuredResponse? = null): GAItem
        = GAItem(simpleResponse, basicCard, structuredResponse)

/**
 * Provides a [GAItem] with a [GABasicCard].
 */
fun BotBus.item(basicCard: GABasicCard): GAItem
        = item(null, basicCard, null)

/**
 * Provides a [GAItem] with a [GASimpleResponse].
 */
fun BotBus.item(simpleResponse: GASimpleResponse): GAItem
        = item(simpleResponse, null, null)


/**
 * Provides a [GAImage] with all available parameters.
 */
fun BotBus.gaImage(url: String, accessibilityText: CharSequence, height: Int? = null, width: Int? = null): GAImage {
    val a = translate(accessibilityText)
    return GAImage(url, a.toString(), height, width)
}

/**
 * Provides a [GAOptionValueSpec] with all available parameters.
 */
fun BotBus.optionValueSpec(simpleSelect: GASimpleSelect? = null,
                           listSelect: GAListSelect? = null,
                           carouselSelect: GACarouselSelect? = null): GAOptionValueSpec
        = GAOptionValueSpec(simpleSelect, listSelect, carouselSelect)

/**
 * Provides a [GAInputPrompt] with all available parameters.
 */
fun BotBus.inputPrompt(richResponse: GARichResponse, noInputPrompts: List<GASimpleResponse> = emptyList()): GAInputPrompt
        = GAInputPrompt(richResponse, noInputPrompts)

/**
 * Provides a [GAInputPrompt] with a simple [GARichResponse] builder.
 */
fun BotBus.inputPrompt(text: CharSequence, linkOutSuggestion: GALinkOutSuggestion? = null, noInputPrompts: List<GASimpleResponse> = emptyList()): GAInputPrompt
        = inputPrompt(richResponse(text, linkOutSuggestion), noInputPrompts)

/**
 * Provides a [GAButton].
 */
fun BotBus.gaButton(title: CharSequence, url: String): GAButton {
    return GAButton(translate(title).toString(), GAOpenUrlAction(url))
}

internal fun BotBus.translateAndSetBlankAsNull(s: CharSequence?): String?
        = translate(s).run { setBlankAsNull() }

internal fun CharSequence?.setBlankAsNull(): String?
        = if (isNullOrBlank()) null else toString()

/**
 * Provides a [GAOptionInfo] with all available parameters.
 */
fun BotBus.optionInfo(
        title: CharSequence,
        targetIntent: IntentAware,
        step: StoryStep<out StoryHandlerDefinition>? = null,
        vararg parameters: Pair<String, String>
): GAOptionInfo {
    val t = translate(title)
    //add the title to the parameters as we need to double check the title in WebhookActionConverter
    val map = parameters.toMap() + (SendChoice.TITLE_PARAMETER to t.toString())
    return GAOptionInfo(
            SendChoice.encodeChoiceId(
                    this,
                    targetIntent,
                    step,
                    map),
            listOf(t.toString())
    )
}

/**
 * The common [GAIntent.text] [GAExpectedIntent].
 */
fun expectedTextIntent(): GAExpectedIntent = GAExpectedIntent(GAIntent.text)


internal fun String.endWithPunctuation(): Boolean
        = endsWith(".") || endsWith("!") || endsWith("?") || endsWith(",") || endsWith(";") || endsWith(":")

internal fun concat(s1: String?, s2: String?): String {
    val s = s1?.trim() ?: ""
    return s + (if (s.isEmpty() || s.endWithPunctuation()) " " else ". ") + (s2?.trim() ?: "")
}

internal fun String.removeEmojis(): String =
        EmojiUtils.removeAllEmojis(
                EmojiUtils.emojify(this.replace("://", "_____"))
                        .replace("\uD83D\uDC68", ":3")
                        .replace("\uD83D\uDE2E", ":0")
                        .replace("_____", "://")
        )