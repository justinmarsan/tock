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

import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {HttpModule} from "@angular/http";
import {ConfirmDialogComponent} from "./confirm-dialog/confirm-dialog.component";
import {
  MdButtonModule,
  MdButtonToggleModule,
  MdCardModule,
  MdCheckboxModule,
  MdChipsModule,
  MdDatepickerModule,
  MdDialogModule,
  MdFormFieldModule,
  MdExpansionModule,
  MdIconModule,
  MdInputModule,
  MdListModule,
  MdPaginatorModule,
  MdProgressSpinnerModule,
  MdRadioModule,
  MdSelectModule,
  MdSidenavModule,
  MdSnackBarModule,
  MdTableModule,
  MdTabsModule,
  MdToolbarModule,
  MdTooltipModule,
} from "@angular/material";
import {MapToIterablePipe} from "./map-to-iterable.pipe";
import {ScrollComponent} from "../scroll/scroll.component";

@NgModule({
  imports: [
    CommonModule,
    MdButtonModule,
    MdButtonToggleModule,
    MdCardModule,
    MdCheckboxModule,
    MdChipsModule,
    MdDatepickerModule,
    MdDialogModule,
    MdExpansionModule,
    MdFormFieldModule,
    MdIconModule,
    MdInputModule,
    MdListModule,
    MdPaginatorModule,
    MdProgressSpinnerModule,
    MdRadioModule,
    MdSelectModule,
    MdSidenavModule,
    MdSnackBarModule,
    MdTableModule,
    MdTabsModule,
    MdToolbarModule,
    MdTooltipModule
  ],
  declarations: [ConfirmDialogComponent, MapToIterablePipe, ScrollComponent],
  providers: [],
  exports: [
    MdButtonModule,
    MdButtonToggleModule,
    MdCardModule,
    MdCheckboxModule,
    MdChipsModule,
    MdDatepickerModule,
    MdDialogModule,
    MdExpansionModule,
    MdFormFieldModule,
    MdIconModule,
    MdInputModule,
    MdListModule,
    MdPaginatorModule,
    MdProgressSpinnerModule,
    MdRadioModule,
    MdSelectModule,
    MdSidenavModule,
    MdSnackBarModule,
    MdTableModule,
    MdTabsModule,
    MdToolbarModule,
    MdTooltipModule,
    FormsModule,
    HttpModule,
    MapToIterablePipe],
  entryComponents: [
    ConfirmDialogComponent
  ]
})
export class SharedModule {
}
