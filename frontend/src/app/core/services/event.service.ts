import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EventModel, EventPackage, Ticket, TicketDetail } from '../models/event.model';
import { HateoasService } from './hateoas.service';

export interface CreateEventRequest {
  name: string;
  description: string;
  location: string;
}

@Injectable({ providedIn: 'root' })
export class EventService {

  private readonly apiUrl = '/api/events';

  constructor(
    private http: HttpClient,
    private hateoas: HateoasService
  ) {}

  listEvents(page = 0, size = 10, name?: string): Observable<EventModel[]> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (name) params = params.set('name', name);
    return this.http.get<EventModel[]>(this.apiUrl, { params }).pipe(
      map(body => this.hateoas.unwrapCollection<EventModel>(body))
    );
  }

  getEvent(id: number): Observable<EventModel> {
    return this.http.get<EventModel>(`${this.apiUrl}/${id}`);
  }

  createEvent(data: CreateEventRequest): Observable<EventModel> {
    return this.http.post<EventModel>(this.apiUrl, data);
  }

  updateEvent(event: EventModel, data: CreateEventRequest): Observable<EventModel> {
    return this.hateoas.followWrite<EventModel>(event, 'edit', 'PUT', data);
  }

  deleteEvent(event: EventModel): Observable<void> {
    return this.hateoas.followWrite<void>(event, 'delete', 'DELETE'
    );
  }
  listPackages(event: EventModel): Observable<EventPackage[]> {
    return this.hateoas.follow<EventPackage[]>(event, 'packages');
  }

  createPackage(event: EventModel, data: Partial<EventPackage>): Observable<EventPackage> {
    return this.hateoas.followWrite<EventPackage>(
      event,
      'create-package',
      'POST',
      data
    );
  }


  deletePackage(packageResource: EventPackage): Observable<void> {
    return this.hateoas.followWrite<void>(
      packageResource,
      'delete-package',
      'DELETE'
    );
  }

  listTickets(packageResource: EventPackage): Observable<Ticket[]> {
    return this.hateoas.follow<Ticket[]>(
      packageResource,
      'tickets'
    );
  }

  getTicketDetail(ticket: Ticket): Observable<TicketDetail> {
    return this.hateoas.follow<TicketDetail>(
      ticket,
      'self'
    );
  }

  purchaseTicket(packageResource: EventPackage): Observable<Ticket> {
    return this.hateoas.followWrite<Ticket>(
      packageResource,
      'purchase',
      'POST'
    );
  }
}
