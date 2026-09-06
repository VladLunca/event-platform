import {Observable, throwError} from 'rxjs';
import {map} from 'rxjs/operators';
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';

export interface HateoasResource {
  _links: Record<string, { href: string }>;
}

interface HalCollection {
  _embedded?: Record<string, unknown>;
}

type WriteMethod = 'POST' | 'PUT' | 'DELETE' | 'PATCH';

@Injectable({ providedIn: 'root' })
export class HateoasService {

  constructor(private http: HttpClient) {}

  has(resource: HateoasResource, rel: string): boolean {
    return !!resource._links?.[rel];
  }

  // HAL serializeaza colectiile ca { _embedded: { <rel>: [...] } } in loc de un array simplu
  unwrapCollection<T>(body: T[] | HalCollection): T[] {
    if (Array.isArray(body)) {
      return body;
    }
    const embedded = body?._embedded;
    if (!embedded) return [];
    const values = Object.values(embedded);
    return (values[0] as T[]) ?? [];
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

    return this.http.get<T | HalCollection>(`/api${href}`).pipe(
      map(body => (Array.isArray(body) || (body as HalCollection)?._embedded
        ? this.unwrapCollection(body as T[] | HalCollection)
        : body) as T)
    );
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
