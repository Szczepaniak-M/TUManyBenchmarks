import {Injectable} from '@angular/core';
import {HttpClient, HttpContext} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {switchMap, tap} from 'rxjs/operators';
import {environment} from "../../environemnts/environment";
import {BYPASS_INTERCEPTOR} from "./auth.interceptor";


@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private localStorageKey = 'public_api_key';

  constructor(private http: HttpClient) {
  }

  public getApiKey(): Observable<string> {
    const storedKey = localStorage.getItem(this.localStorageKey)
    if (storedKey) {
      return of(storedKey);
    } else {
      return this.refreshApiKey();
    }
  }

  public refreshApiKey(): Observable<string> {
    return this.http.get<{ apiKey: string }>(`${this.apiUrl}/key`,
      {context: new HttpContext().set(BYPASS_INTERCEPTOR, true)})
      .pipe(
        tap(response => {
          localStorage.setItem(this.localStorageKey, response.apiKey);
        }),
        switchMap(response => of(response.apiKey))
      );
  }
}
