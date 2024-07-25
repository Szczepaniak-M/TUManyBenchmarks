import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';
import {environment} from "../../environemnts/environment";
import {InstanceDetails, InstanceDetailsDto} from "./instance-details.model";
import {AuthService} from "../auth/auth.service";
import {Instance, InstanceDto} from "../instance-list/instance.model";


@Injectable({
  providedIn: 'root'
})
export class InstanceDetailsService {
  private apiUrl = environment.apiUrl;
  private vCPUsPattern = /\d+ vCPUs/;
  private memoryPattern = /[\S ]+ Memory/;
  private networkPattern = /[\S ]+ Network/;

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
            map((instance: InstanceDetailsDto) => this.extractInformationFromTags(instance))
          );
  }

  private extractInformationFromTags(instance: InstanceDetailsDto): InstanceDetails {
    const vCpu = instance.tags.filter(tag => this.vCPUsPattern.test(tag))[0]
    const memory = instance.tags.filter(tag => this.memoryPattern.test(tag))[0]
    const network = instance.tags.filter(tag => this.networkPattern.test(tag))[0]
    const excludeTags = [vCpu, memory, network];
    const otherTags = instance.tags.filter(tag => !excludeTags.includes(tag))
    return {
      id: instance.id,
      name: instance.name,
      vCpu: vCpu,
      memory: memory,
      network: network,
      otherTags: otherTags,
      benchmarks: instance.benchmarks
    }
  }
}
