import {Injectable} from '@angular/core';
import {HttpContextToken, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {AuthService} from "./auth.service";

export const BYPASS_INTERCEPTOR = new HttpContextToken(() => false);

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.context.get(BYPASS_INTERCEPTOR))
      return next.handle(req);

    return this.authService.getApiKey().pipe(
      switchMap(key => {
        const clonedReq = req.clone({
          setHeaders: {
            'X-API-Key': `${key}`
          }
        });
        return next.handle(clonedReq);
      })
    );
  }
}
