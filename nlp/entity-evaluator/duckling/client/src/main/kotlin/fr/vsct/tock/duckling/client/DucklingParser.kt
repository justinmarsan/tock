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

package fr.vsct.tock.duckling.client

import fr.vsct.tock.nlp.core.IntOpenRange
import fr.vsct.tock.nlp.core.service.entity.EntityEvaluator
import fr.vsct.tock.nlp.model.EntityCallContextForEntity
import fr.vsct.tock.shared.name
import fr.vsct.tock.nlp.entity.AmountOfMoneyValue
import fr.vsct.tock.nlp.entity.DistanceValue
import fr.vsct.tock.nlp.entity.EmailValue
import fr.vsct.tock.nlp.entity.NumberValue
import fr.vsct.tock.nlp.entity.OrdinalValue
import fr.vsct.tock.nlp.entity.PhoneNumberValue
import fr.vsct.tock.nlp.entity.UrlValue
import fr.vsct.tock.nlp.entity.Value
import fr.vsct.tock.nlp.entity.VolumeValue
import fr.vsct.tock.nlp.entity.date.DateEntityGrain
import fr.vsct.tock.nlp.entity.date.DateEntityValue
import fr.vsct.tock.nlp.entity.date.DateIntervalEntityValue
import fr.vsct.tock.nlp.entity.temperature.TemperatureUnit
import fr.vsct.tock.nlp.entity.temperature.TemperatureValue
import mu.KotlinLogging
import java.lang.Exception
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 *
 */
internal object DucklingParser : EntityEvaluator {

    data class ValueWithRange(override val start: Int,
                              override val end: Int,
                              val value: Value) : IntOpenRange {

    }

    private val logger = KotlinLogging.logger {}
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

    val dimensions =
            listOf("datetime",
                    "temperature",
                    "number",
                    "ordinal",
                    "distance",
                    "volume",
                    "amount-of-money",
                    "duration",
                    "email",
                    "url",
                    "phone-number")

    val entityTypes = dimensions.map { "duckling:${it}" }.toSet()

    private fun tockTypeToDucklingType(type: String): String {
        return when (type) {
            "datetime" -> "time"
            else -> type
        }
    }

    override fun evaluate(context: EntityCallContextForEntity, text: String): Value? {
        return try {
            val values = parse(
                    context.language.language,
                    tockTypeToDucklingType(context.entityType.name.name()),
                    context.referenceDate,
                    text)
            values.firstOrNull()
        } catch(e: Exception) {
            logger.error(e.message, e)
            null
        }
    }

    fun parse(language: String, dimension: String, referenceDate: ZonedDateTime, textToParse: String): List<Value> {
        val parseResult = DucklingClient.parse(language, listOf(dimension), referenceDate, textToParse)

        return when (dimension) {
            "time" -> parseDate(parseResult)
            "number" -> parseSimple(parseResult, dimension, { NumberValue(it[":value"].number()) })
            "ordinal" -> parseSimple(parseResult, dimension, { OrdinalValue(it[":value"].number()) })
            "distance" -> parseSimple(parseResult, dimension, { DistanceValue(it[":value"].number(), it[":unit"].string()) })
            "temperature" -> parseSimple(parseResult, dimension, { TemperatureValue(it[":value"].number(), TemperatureUnit.valueOf(it[":unit"].string())) })
            "volume" -> parseSimple(parseResult, dimension, { VolumeValue(it[":value"].number(), it[":unit"].string()) })
            "amount-of-money" -> parseSimple(parseResult, dimension, { AmountOfMoneyValue(it[":value"].number(), it[":unit"].string()) })
            "url" -> parseSimple(parseResult, dimension, { UrlValue(it[":value"].string()) })
            "email" -> parseSimple(parseResult, dimension, { EmailValue(it[":value"].string()) })
            "phone-number" -> parseSimple(parseResult, dimension, { PhoneNumberValue(it[":value"].string()) })
        //TODO duration
            else -> TODO("Not yet supported yet : $dimension")
        }
    }

    private fun parseSimple(parseResult: JSONValue, dim: String, parseFunction: (JSONValue) -> Value): List<Value> {
        return parseResult.iterable().mapNotNull {
            if (it[":dim"].string() == dim) {
                parseFunction.invoke(it[":value"])
            } else {
                null
            }
        }
    }

    private fun parseDate(parseResult: JSONValue): List<Value> {
        var result = mutableListOf<ValueWithRange>()
        try {
            if (!parseResult.isEmpty()) {
                for (a in parseResult.iterable()) {
                    if (a[":dim"].string() == "time") {
                        val start = a[":start"].int()
                        val end = a[":end"].int()

                        val valueMap = a[":value"]

                        val grain = valueMap[":grain"]
                        if (grain.isNotNull()) {
                            result.add(ValueWithRange(
                                    start,
                                    end,
                                    DateEntityValue(
                                            ZonedDateTime.parse(valueMap[":value"].string(), formatter),
                                            DateEntityGrain.valueOf(grain.string())
                                    )))
                        } else {
                            //type interval
                            val fromMap = valueMap[":from"]
                            val toMap = valueMap[":to"]
                            var entityValue: ValueWithRange? = null
                            if (toMap.isNotNull() && fromMap.isNotNull()) {
                                val toGrain = toMap[":grain"]
                                if (toGrain.isNotNull()) {
                                    entityValue = ValueWithRange(
                                            start,
                                            end,
                                            DateIntervalEntityValue(
                                                    DateEntityValue(
                                                            ZonedDateTime.parse(fromMap[":value"].string(), formatter),
                                                            DateEntityGrain.valueOf(fromMap[":grain"].string())
                                                    ),
                                                    DateEntityValue(
                                                            ZonedDateTime.parse(toMap[":value"].string(), formatter),
                                                            DateEntityGrain.valueOf(toMap[":grain"].string())
                                                    )
                                            ))
                                }
                            }

                            if (entityValue == null) {
                                val vMap = if (fromMap.isNotNull()) fromMap else toMap
                                if (vMap.isNotNull()) {
                                    entityValue = ValueWithRange(
                                            start,
                                            end,
                                            DateEntityValue(
                                                    ZonedDateTime.parse(vMap[":value"].string(), formatter),
                                                    DateEntityGrain.valueOf(vMap[":grain"].string())
                                            ))
                                }
                            }

                            if (entityValue != null) {
                                result.add(entityValue)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            logger.error(e) { e.message }
        }

        //merge
        result.sort()
        if (result.size > 1) {
            var skipNext = false
            val result2 = mutableListOf<ValueWithRange>()
            for (i in result.indices) {
                if (!skipNext) {
                    if (i < result.size - 1) {
                        //overlap, try to mergeDate
                        if (result[i].end > result[i + 1].start) {
                            result2.add(mergeDate(result[i], result[i + 1]))
                            skipNext = true
                        } else {
                            result2.add(result[i])
                        }
                    } else {
                        result2.add(result[i])
                    }
                } else {
                    skipNext = false
                }
            }
            result = result2
        }

        return result.map { it.value }
    }

    private fun mergeDate(r1: ValueWithRange, r2: ValueWithRange): ValueWithRange {
        //overlap, try to merge
        if (r1.value is DateEntityValue && r2.value is DateEntityValue) {


            if (r1.value.grain == r2.value.grain) {
                return ValueWithRange(
                        r1.start,
                        r2.end,
                        DateIntervalEntityValue(r1.value, r2.value))
            } else {
                val dateGrain = if (r1.value.grain.time) r2.value else r1.value
                val timeGrain = if (r1.value.grain.time) r2.value else r1.value
                return ValueWithRange(
                        r1.start,
                        r2.end,
                        DateEntityValue(dateGrain.date.plus(Duration.ofSeconds(timeGrain.date.toLocalTime().toSecondOfDay().toLong())), timeGrain.grain))
            }
        }
        //return the first for now
        return r1
    }
}