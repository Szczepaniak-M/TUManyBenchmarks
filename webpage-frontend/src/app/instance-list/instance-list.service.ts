import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {map, Observable} from "rxjs";
import {catchError, switchMap} from "rxjs/operators";
import {environment} from "../../environemnts/environment";
import {Instance, InstanceDto} from "./instance.model";
import {AuthService} from "../auth/auth.service";
import {convertInstanceDtoToInstance} from "../common/instance/instance.utils";


@Injectable({
  providedIn: "root"
})
export class InstanceListService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {
  }

  public getInstances(): Observable<Instance[]> {
    return this.http.get<InstanceDto[]>(`${this.apiUrl}/instance`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<InstanceDto[]>(`${this.apiUrl}/instance`)
              )
            );
          } else {
            throw error;
          }
        })
      )
      .pipe(
        map((instances: InstanceDto[]) =>
          instances.map(instance => convertInstanceDtoToInstance(instance))
        )
      );
  }
}
