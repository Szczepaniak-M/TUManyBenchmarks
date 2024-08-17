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
import {NavbarComponent} from "./navbar/navbar.component";
import {AboutComponent} from "./about/about.component";
import {ListRowComponent} from "./instance-list/list-row/list-row.component";
import {ListFilterComponent} from "./instance-list/list-filter/list-filter.component";
import {CompareInstancesBenchmarkComponent} from "./compare-instances/compare-instances-benchmark/compare-instances-benchmark.component";
import {MonacoEditorModule} from "ngx-monaco-editor-v2";
import {MonacoEditorComponent} from "./instance-list/list-query/monaco-editor/monaco-editor.component";
import {NgApexchartsModule} from "ng-apexcharts";
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {MatFormField, MatLabel} from "@angular/material/form-field";
import {MatOption, MatSelect} from "@angular/material/select";
import {MatInput} from "@angular/material/input";
import {ListSortComponent} from "./instance-list/list-header/list-sort/list-sort.component";
import {MatSlideToggle} from "@angular/material/slide-toggle";
import {BenchmarkScatterPlotComponent} from "./common/benchmark-scatter-plot/benchmark-scatter-plot.component";
import {BenchmarkPlotComponent} from "./common/benchmark-plot/benchmark-plot.component";
import {BenchmarkLinePlotComponent} from "./common/benchmark-line-plot/benchmark-line-plot.component";
import {ListHeaderComponent} from "./instance-list/list-header/list-header.component";
import {ListQueryComponent} from "./instance-list/list-query/list-query.component";


@NgModule({
  declarations: [
    AppComponent,
    NavbarComponent,
    InstanceListComponent,
    ListRowComponent,
    ListFilterComponent,
    ListSortComponent,
    ListHeaderComponent,
    ListQueryComponent,
    InstanceDetailsComponent,
    BenchmarkPlotComponent,
    BenchmarkScatterPlotComponent,
    BenchmarkLinePlotComponent,
    CompareInstancesComponent,
    CompareInstancesBenchmarkComponent,
    AboutComponent,
    MonacoEditorComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    NgForOf,
    NgIf,
    MonacoEditorModule.forRoot(),
    NgApexchartsModule,
    MatFormField,
    MatSelect,
    MatOption,
    MatInput,
    MatLabel,
    MatSlideToggle
  ],
  providers: [
    provideHttpClient(
      withInterceptorsFromDi(),
    ),
    {provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true},
    provideAnimationsAsync(),
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
