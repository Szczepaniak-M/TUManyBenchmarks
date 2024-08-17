import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {map, Observable} from "rxjs";
import {catchError, switchMap} from "rxjs/operators";
import {environment} from "../../environemnts/environment";
import {BenchmarkDetails, BenchmarkStatistics, Instance} from "./instance.model";
import {AuthService} from "../auth/auth.service";
import {removeUnnecessaryTags} from "../common/instance/instance.utils";


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
      .pipe(
        map(instances => instances.map(instance => removeUnnecessaryTags(instance)))
      );
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
      .pipe(
        map(statistics => statistics.map(stats => {
            stats.min = Number(stats.min.toPrecision(5));
            stats.avg = Number(stats.avg.toPrecision(5));
            stats.median = Number(stats.median.toPrecision(5));
            stats.max = Number(stats.max.toPrecision(5));
            return stats;
          }
        ))
      )
  }
}
