import {Observable, throwError} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';

export interface HateoasResource {
  _links: Record<string, { href: string }>;
}

type WriteMethod = 'POST' | 'PUT' | 'DELETE' | 'PATCH';

@Injectable({ providedIn: 'root' })
export class HateoasService {

  constructor(private http: HttpClient) {}

  has(resource: HateoasResource, rel: string): boolean {
    return !!resource._links?.[rel];
  }

  follow<T>(
    resource: HateoasResource,
    rel: string
  ): Observable<T> {
    const href = resource._links?.[rel]?.href;

    if (!href) {
      return throwError(
        () => new Error(`Link "${rel}" indisponibil pe resursă`)
      );
    }

    return this.http.get<T>(`/api${href}`);
  }

  followWrite<T>(
    resource: HateoasResource,
    rel: string,
    method: WriteMethod,
    body?: unknown
  ): Observable<T> {

    const href = resource._links?.[rel]?.href;

    if (!href) {
      return throwError(
        () => new Error(`Link "${rel}" indisponibil pe resursă`)
      );
    }

    const url = `/api${href}`;

    switch (method) {
      case 'POST':
        return this.http.post<T>(url, body ?? {});

      case 'PUT':
        return this.http.put<T>(url, body ?? {});

      case 'PATCH':
        return this.http.patch<T>(url, body ?? {});

      case 'DELETE':
        return this.http.delete<T>(url);
    }
  }
}
