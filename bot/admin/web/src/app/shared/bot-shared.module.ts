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

import {CommonModule} from "@angular/common";
import {NgModule} from "@angular/core";
import {BotMessageComponent} from "./bot-message/bot-message.component";
import {SentenceElementComponent} from "./bot-message/sentence-element.component";
import {BotMessageSentenceComponent} from "./bot-message/bot-message-sentence";
import {BotMessageChoiceComponent} from "./bot-message/bot-message-choice";
import {BotMessageLocationComponent} from "./bot-message/bot-message-location";
import {BotMessageAttachmentComponent} from "./bot-message/bot-message-attachment";
import {MapToIterablePipe} from "./map-to-iterable.pipe";
import {SharedModule} from "tock-nlp-admin/src/app/shared/shared.module";
import {BotSharedService} from "./bot-shared.service";
import {DisplayDialogComponent} from "./bot-dialog/display-dialog.component";
import {MomentModule} from "angular2-moment";
@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    MomentModule,
  ],
  declarations: [
    BotMessageComponent,
    SentenceElementComponent,
    BotMessageSentenceComponent,
    BotMessageChoiceComponent,
    BotMessageLocationComponent,
    BotMessageAttachmentComponent,
    MapToIterablePipe,
    DisplayDialogComponent
  ],
  exports: [BotMessageComponent, DisplayDialogComponent],
  providers: [BotSharedService],
  entryComponents: []
})
export class BotSharedModule {
}
