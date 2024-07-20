import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';
import {environment} from "../../environemnts/environment";
import {InstanceDetails} from "./instance-details.model";
import {AuthService} from "../auth/auth.service";


@Injectable({
  providedIn: 'root'
})
export class InstanceDetailsService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {
  }

  public getInstanceDetails(instanceName: String): Observable<InstanceDetails> {
    return this.http.get<InstanceDetails>(`${this.apiUrl}/instance/${instanceName}`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<InstanceDetails>(`${this.apiUrl}/instance/${instanceName}`)
              )
            );
          } else {
            throw error;
          }
        })
      );
  }
}
