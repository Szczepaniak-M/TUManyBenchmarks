import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {map, Observable} from "rxjs";
import {catchError, switchMap} from "rxjs/operators";
import {environment} from "../../environemnts/environment";
import {AuthService} from "../auth/auth.service";
import {removeUnnecessaryTags} from "../common/instance/instance.utils";
import {Instance} from "../instance-list/instance.model";


@Injectable({
  providedIn: "root"
})
export class InstanceDetailsService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {
  }

  public getInstanceDetails(instanceName: string): Observable<Instance> {
    return this.http.get<Instance>(`${this.apiUrl}/instance/${instanceName}`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<Instance>(`${this.apiUrl}/instance/${instanceName}`)
              )
            );
          } else {
            throw error;
          }
        }))
      .pipe(
        map(instance => removeUnnecessaryTags(instance))
      );
  }
}
