import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventModel, EventPackage, Ticket, TicketDetail } from '../models/event.model';

@Injectable({ providedIn: 'root' })
export class EventService {

  private readonly apiUrl = '/api/events';

  constructor(private http: HttpClient) {}

  // ── Events ──────────────────────────────────────────────────────────────

  listEvents(page = 0, size = 10, name?: string): Observable<EventModel[]> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (name) params = params.set('name', name);
    return this.http.get<EventModel[]>(this.apiUrl, { params });
  }

  getEvent(id: number): Observable<EventModel> {
    return this.http.get<EventModel>(`${this.apiUrl}/${id}`);
  }

  createEvent(data: Partial<EventModel>): Observable<EventModel> {
    return this.http.post<EventModel>(this.apiUrl, data);
  }

  updateEvent(id: number, data: Partial<EventModel>): Observable<EventModel> {
    return this.http.put<EventModel>(`${this.apiUrl}/${id}`, data);
  }

  deleteEvent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ── Packages ─────────────────────────────────────────────────────────────

  listPackages(eventId: number): Observable<EventPackage[]> {
    return this.http.get<EventPackage[]>(`${this.apiUrl}/${eventId}/packages`);
  }

  createPackage(eventId: number, data: Partial<EventPackage>): Observable<EventPackage> {
    return this.http.post<EventPackage>(`${this.apiUrl}/${eventId}/packages`, data);
  }

  updatePackage(eventId: number, packageId: number, data: Partial<EventPackage>): Observable<EventPackage> {
    return this.http.put<EventPackage>(`${this.apiUrl}/${eventId}/packages/${packageId}`, data);
  }

  deletePackage(eventId: number, packageId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${eventId}/packages/${packageId}`);
  }

  // ── Tickets ───────────────────────────────────────────────────────────────

  listTickets(eventId: number, packageId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.apiUrl}/${eventId}/packages/${packageId}/tickets`);
  }

  getTicketDetail(ticketId: string): Observable<TicketDetail> {
    return this.http.get<TicketDetail>(`${this.apiUrl}/tickets/${ticketId}`);
  }

  purchaseTicket(eventId: number, packageId: number): Observable<Ticket> {
    return this.http.post<Ticket>(
      `${this.apiUrl}/${eventId}/packages/${packageId}/tickets`, {}
    );
  }
}
