import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClientProfile } from '../models/client.model';

export interface CreateClientRequest {
  email: string;
  firstName?: string;
  lastName?: string;
  publicInfo?: boolean;
  socialMedia?: {
    linkedin?: string;
    publicSocialMedia?: boolean;
  };
}

export interface UpdateClientRequest {
  firstName?: string;
  lastName?: string;
  publicInfo?: boolean;
  socialMedia?: {
    linkedin?: string;
    publicSocialMedia?: boolean;
  };
}

export interface AddTicketRequest {
  ticketId: string;
}

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly apiUrl = '/api/clients';

  constructor(private http: HttpClient) {}

  getProfile(email: string): Observable<ClientProfile> {
    return this.http.get<ClientProfile>(`${this.apiUrl}/${email}`);
  }

  createProfile(data: CreateClientRequest): Observable<ClientProfile> {
    return this.http.post<ClientProfile>(`${this.apiUrl}/`, data);
  }

  updateProfile(email: string, data: UpdateClientRequest): Observable<ClientProfile> {
    return this.http.patch<ClientProfile>(`${this.apiUrl}/${email}`, data);
  }

  deleteProfile(email: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${email}`);
  }

  getTickets(email: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/${email}/tickets`);
  }

  addTicket(email: string, ticketId: string): Observable<string[]> {
    return this.http.post<string[]>(`${this.apiUrl}/${email}/tickets`, { ticketId });
  }
}
