// import {Injectable} from "@angular/core";
// import {HttpClient} from "@angular/common/http";
// import {Observable} from "rxjs";
// import {catchError, switchMap} from "rxjs/operators";
// import {environment} from "../../environemnts/environment";
// import {AuthService} from "../auth/auth.service";
// import {InstanceExplorerRequest, InstanceExplorerResponse} from "./instance-explorer.model";
//
//
// @Injectable({
//   providedIn: "root"
// })
// export class InstanceDetailsService {
//   private apiUrl = environment.apiUrl;
//
//   constructor(private http: HttpClient, private authService: AuthService) {
//   }
//
//   public executeQuery(aggregationStages: string[], partialResults: boolean): Observable<InstanceExplorerResponse> {
//     const request: InstanceExplorerRequest = {
//       aggregationStages: aggregationStages,
//       partialResults: partialResults,
//     };
//     return this.http.post<InstanceExplorerResponse>(`${this.apiUrl}/explorer`, request)
//       .pipe(
//         catchError(error => {
//           if (error.status === 401) {
//             return this.authService.refreshApiKey().pipe(
//               switchMap(_ =>
//                 this.http.post<InstanceExplorerResponse>(`${this.apiUrl}/explorer`, request)
//               )
//             );
//           } else {
//             throw error;
//           }
//         })
//       );
//   }
// }
