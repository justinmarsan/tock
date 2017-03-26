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

package fr.vsct.tock.nlp.admin

import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.front.client.FrontClient
import fr.vsct.tock.nlp.front.shared.config.EntityTypeDefinition
import fr.vsct.tock.nlp.front.shared.config.IntentDefinition
import fr.vsct.tock.shared.vertx.BadRequestException
import fr.vsct.tock.shared.vertx.UnauthorizedException
import fr.vsct.tock.shared.vertx.WebVerticle
import fr.vsct.tock.nlp.admin.model.ApplicationWithIntents
import fr.vsct.tock.nlp.admin.model.CreateEntityQuery
import fr.vsct.tock.nlp.admin.model.ParseQuery
import fr.vsct.tock.nlp.admin.model.SearchQuery
import fr.vsct.tock.nlp.admin.model.SentenceReport
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import mu.KotlinLogging
import java.util.Locale

/**
 *
 */
class AdminVerticle : WebVerticle(KotlinLogging.logger {}) {

    override val rootPath: String = "/rest/admin"

    override fun authProvider(): AuthProvider? = authProvider

    override fun configure() {
        val front = FrontClient
        val admin = AdminService

        blockingJsonGet("/applications") { context ->
            front.getApplications().filter {
                it.namespace == context.organization
            }.map {
                admin.getApplicationWithIntents(it)
            }
        }

        blockingJsonGet("/application/:id") { context ->
            admin.getApplicationWithIntents(context.pathParam("id"))
                    ?.takeIf { it.namespace == context.organization }
        }

        blockingJsonPost("/application") { context, application: ApplicationWithIntents ->
            if (context.organization == application.namespace
                    && (application._id == null || context.organization == front.getApplicationById(application._id)?.namespace)) {
                val appWithSameName = front.getApplicationByNamespaceAndName(application.namespace, application.name)
                if (appWithSameName != null && appWithSameName._id != application._id) {
                    throw BadRequestException("Application with same name already exists")
                }
                front.save(application.toApplication().copy(name = application.name.toLowerCase()))
            } else {
                throw UnauthorizedException()
            }
        }

        blockingDelete("/application/:id") {
            val id = it.pathParam("id")
            if (it.organization == front.getApplicationById(id)?.namespace) {
                front.deleteApplicationById(id)
            } else {
                throw UnauthorizedException()
            }
        }

        blockingDelete("/application/:appId/intent/:intentId") {
            val app = front.getApplicationById(it.pathParam("appId"))
            val intentId = it.pathParam("intentId")
            val intent = front.getIntentById(intentId)!!
            if (it.organization == app?.namespace) {
                front.switchIntent(app._id!!, intentId, Intent.unknownIntent)
                front.save(app.copy(intents = app.intents - intentId))
                front.save(intent.copy(applications = intent.applications - app._id!!))
            } else {
                throw UnauthorizedException()
            }
        }

        blockingDelete("/application/:appId/intent/:intentId/entity/:entityType/:role") {
            val app = front.getApplicationById(it.pathParam("appId"))
            val intentId = it.pathParam("intentId")
            val entityType = it.pathParam("entityType")
            val role = it.pathParam("role")
            val intent = front.getIntentById(intentId)!!
            if (it.organization == app?.namespace && it.organization == intent.namespace) {
                front.removeEntity(app._id!!, intentId, entityType, role)
                front.save(intent.copy(entities = intent.entities - intent.findEntity(entityType, role)!!))
            } else {
                throw UnauthorizedException()
            }
        }

        blockingJsonGet("/locales") {
            Locale.getAvailableLocales()
                    .filter { it.language.isNotEmpty() }
                    .distinctBy { it.language }
                    .map { it.language to it.getDisplayLanguage(Locale.ENGLISH).capitalize() }
                    .sortedBy { it.second }
        }

        blockingJsonPost("/parse") { context, query: ParseQuery ->
            if (context.organization == query.namespace) {
                admin.parseSentence(query)
            } else {
                throw UnauthorizedException()
            }
        }

        blockingJsonPost("/sentence") { context, s: SentenceReport ->
            if (context.organization == front.getApplicationById(s.applicationId)?.namespace) {
                front.save(s.toClassifiedSentence())
            } else {
                throw UnauthorizedException()
            }
        }

        blockingJsonPost("/sentences/search") { context, s: SearchQuery ->
            if (context.organization == s.namespace) {
                admin.searchSentences(s)
            } else {
                throw UnauthorizedException()
            }
        }



        blockingJsonPost("/intent") { context, intent: IntentDefinition ->
            if (context.organization == intent.namespace) {
                front.save(intent)
                intent.applications.forEach {
                    front.save(front.getApplicationById(it)!!.let { it.copy(intents = it.intents + intent._id!!) })
                }
                intent
            } else {
                throw UnauthorizedException()
            }
        }

        blockingJsonGet("/entities") { front.getEntityTypes() }

        blockingJsonPost<CreateEntityQuery, EntityTypeDefinition?>("/entity/create") { context, query ->
            val entityName = "${context.organization}:${query.type.toLowerCase()}"
            if (front.getEntityTypeByName(entityName) == null) {
                val entityType = EntityTypeDefinition(entityName, "")
                front.save(entityType)
                entityType
            } else {
                null
            }
        }
    }

    override fun healthcheck(): (RoutingContext) -> Unit {
        return { it.response().end() }
    }
}