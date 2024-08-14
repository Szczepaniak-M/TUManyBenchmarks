import {RouterModule, Routes} from "@angular/router";
import {CompareInstancesComponent} from "./compare-instances/compare-instances.component";
import {InstanceDetailsComponent} from "./instance-details/instance-details.component";
import {InstanceListComponent} from "./instance-list/instance-list.component";
import {NgModule} from "@angular/core";
import {AboutComponent} from "./about/about.component";
// import {InstanceExplorerComponent} from "./instance-explorer/instance-explorer.component";

const routes: Routes = [
  {path: "", component: InstanceListComponent},
  {path: "about", component: AboutComponent},
  // {path: "explore", component: InstanceExplorerComponent},
  {path: "instance", redirectTo: ""},
  {path: "instance/compare", component: CompareInstancesComponent},
  {path: "instance/:instance", component: InstanceDetailsComponent},
  {path: "**", redirectTo: ""}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
