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

package fr.vsct.tock.bot.admin.test

import fr.vsct.tock.bot.admin.test.TestPlanService.toClientMessage
import fr.vsct.tock.bot.connector.rest.client.model.ClientAttachment
import fr.vsct.tock.bot.connector.rest.client.model.ClientAttachmentType
import fr.vsct.tock.bot.engine.action.SendAttachment
import fr.vsct.tock.bot.engine.message.Attachment
import org.junit.Test
import kotlin.test.assertEquals

/**
 *
 */
class TestPlanServiceTest {

    @Test
    fun clientMessage_withMessageWithNotVoidDelay_mustBeEquals() {
        val a = Attachment("a", SendAttachment.AttachmentType.file, 200)

        assertEquals(
                ClientAttachment("a", ClientAttachmentType.file),
                a.toClientMessage()
        )
    }
}