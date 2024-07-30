import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {map, Observable} from "rxjs";
import {catchError, switchMap} from "rxjs/operators";
import {environment} from "../../environemnts/environment";
import {InstanceDetails, InstanceDetailsDto} from "./instance-details.model";
import {AuthService} from "../auth/auth.service";
import {convertInstanceDtoToInstance} from "../common/instance.utils";


@Injectable({
  providedIn: "root"
})
export class InstanceDetailsService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient, private authService: AuthService) {
  }

  public getInstanceDetails(instanceName: string): Observable<InstanceDetails> {
    return this.http.get<InstanceDetailsDto>(`${this.apiUrl}/instance/${instanceName}`)
      .pipe(
        catchError(error => {
          if (error.status === 401) {
            return this.authService.refreshApiKey().pipe(
              switchMap(_ =>
                this.http.get<InstanceDetailsDto>(`${this.apiUrl}/instance/${instanceName}`)
              )
            );
          } else {
            throw error;
          }
        }))
      .pipe(
        map(instanceDto => this.convertInstanceDetailsDtoToInstanceDetails(instanceDto))
      );
  }

  private convertInstanceDetailsDtoToInstanceDetails(instanceDto: InstanceDetailsDto) {
    const instance = convertInstanceDtoToInstance(instanceDto);
    const instanceDetails: InstanceDetails = {
      ...instance,
      benchmarks: instanceDto.benchmarks
    };
    return instanceDetails
  }
}
