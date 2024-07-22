import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {map, Observable} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';
import {environment} from "../../environemnts/environment";
import {Instance, InstanceDto} from "./instance.model";
import {AuthService} from "../auth/auth.service";


@Injectable({
  providedIn: 'root'
})
export class InstanceListService {
  private apiUrl = environment.apiUrl;
  private vCPUsPattern = /\d+ vCPUs/;
  private memoryPattern = /[\S ]+ Memory/;
  private networkPattern = /[\S ]+ Network/;

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
          instances.map(instance => this.extractInformationFromTags(instance)
          )
        )
      );
  }

  private extractInformationFromTags(instance: InstanceDto): Instance {
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
      otherTags: otherTags
    }
  }
}
