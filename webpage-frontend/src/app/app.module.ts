import {NgModule} from "@angular/core";
import {AppComponent} from "./app.component";
import {InstanceListComponent} from "./instance-list/instance-list.component";
import {FormsModule} from "@angular/forms";
import {NgForOf, NgIf} from "@angular/common";
import {InstanceDetailsComponent} from "./instance-details/instance-details.component";
import {CompareInstancesComponent} from "./compare-instances/compare-instances.component";
import {AppRoutingModule} from "./app-routing.module";
import {BrowserModule} from "@angular/platform-browser";
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {AuthInterceptor} from "./auth/auth.interceptor";
import { NavbarComponent } from './navbar/navbar.component';
import { AboutComponent } from './about/about.component';
import { InstanceListRowComponent } from './instance-list/instance-list-row/instance-list-row.component';
import { InstanceListFilterComponent } from './instance-list/instance-list-filter/instance-list-filter.component';
import { CompareInstancesBenchmarkComponent } from './compare-instances/compare-instances-benchmark.component';
import { InstanceExplorerComponent } from './instance-explorer/instance-explorer.component';
import { JsonViewerComponent } from './instance-explorer/json-viewer/json-viewer.component';
import {NgxJsonViewerModule} from "ngx-json-viewer";
import {MonacoEditorModule} from "ngx-monaco-editor-v2";
import { MonacoEditorComponent } from './instance-explorer/monaco-editor/monaco-editor.component';
import { ScatterBenchmarkPlotComponent} from "./benchmark-plot/benchmark-plot.component";
import {NgApexchartsModule} from "ng-apexcharts";


@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    InstanceListComponent,
    InstanceListRowComponent,
    InstanceListFilterComponent,
    InstanceDetailsComponent,
    CompareInstancesComponent,
    CompareInstancesBenchmarkComponent,
    AboutComponent,
    InstanceExplorerComponent,
    JsonViewerComponent,
    MonacoEditorComponent,
    ScatterBenchmarkPlotComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    NgForOf,
    NgIf,
    NgxJsonViewerModule,
    MonacoEditorModule.forRoot(),
    NgApexchartsModule
  ],
  providers: [
    provideHttpClient(
      withInterceptorsFromDi(),
    ),
    {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true},
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
