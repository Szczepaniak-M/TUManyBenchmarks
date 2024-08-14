import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {catchError, switchMap} from "rxjs/operators";
import {environment} from "../../environemnts/environment";
import {BenchmarkDetails, BenchmarkStatistics, Instance} from "./instance.model";
import {AuthService} from "../auth/auth.service";


@Injectable({
  providedIn: "root"
})
export class InstanceListService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {
  }

  public getInstances(): Observable<Instance[]> {
    return this.http.get<Instance[]>(`${this.apiUrl}/instance`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<Instance[]>(`${this.apiUrl}/instance`)
              )
            );
          } else {
            throw error;
          }
        })
      )
  }

  public getBenchmarks(): Observable<BenchmarkDetails[]> {
    return this.http.get<BenchmarkDetails[]>(`${this.apiUrl}/benchmark`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<BenchmarkDetails[]>(`${this.apiUrl}/benchmark`)
              )
            );
          } else {
            throw error;
          }
        })
      )
  }

  public getStatistics(): Observable<BenchmarkStatistics[]> {
    return this.http.get<BenchmarkStatistics[]>(`${this.apiUrl}/statistics`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<BenchmarkStatistics[]>(`${this.apiUrl}/statistics`)
              )
            );
          } else {
            throw error;
          }
        })
      )
  }
}
