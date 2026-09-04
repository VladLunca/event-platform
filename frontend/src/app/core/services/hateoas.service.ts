import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';

export interface HateoasResource {
  _links: Record<string, { href: string }>;
}

type WriteMethod = 'PUT' | 'DELETE' | 'PATCH';

@Injectable({ providedIn: 'root' })
export class HateoasService {
  constructor(private http: HttpClient) {}

  has(resource: HateoasResource, rel: string): boolean {
    return !!resource._links?.[rel];
  }

  follow<T>(resource: HateoasResource, rel: string): Observable<T> {
    const href = resource._links?.[rel]?.href;
    if (!href) {
      return throwError(() => new Error(`Link "${rel}" indisponibil pe resursă`));
    }
    return this.http.get<T>(`/api${href}`);
  }

  followWrite<T>(resource: HateoasResource, rel: string, method: WriteMethod, body?: unknown): Observable<T> {
    const href = resource._links?.[rel]?.href;
    if (!href) {
      return throwError(() => new Error(`Link "${rel}" indisponibil pe resursă`));
    }
    const url = `/api${href}`;
    switch (method) {
      case 'PUT':
        return this.http.put<T>(url, body ?? {});
      case 'PATCH':
        return this.http.patch<T>(url, body ?? {});
      case 'DELETE':
        return this.http.delete<T>(url);
    }
  }
}
