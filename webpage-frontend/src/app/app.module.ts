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


@NgModule({
  declarations: [
    AppComponent,
    InstanceListComponent,
    InstanceDetailsComponent,
    CompareInstancesComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FormsModule,
    NgForOf,
    NgIf
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
