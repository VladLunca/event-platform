import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { ClientService, CreateClientRequest, UpdateClientRequest } from '../../core/services/client.service';
import { EventService } from '../../core/services/event.service';
import { ClientProfile } from '../../core/models/client.model';
import { TicketDetail } from '../../core/models/event.model';

type Mode = 'loading' | 'create' | 'view' | 'edit';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {

  mode = signal<Mode>('loading');
  profile = signal<ClientProfile | null>(null);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  email = '';

  createForm: CreateClientRequest = { email: '', firstName: '', lastName: '', publicInfo: false, socialMedia: { linkedin: '', publicSocialMedia: false } };

  editForm: UpdateClientRequest = { firstName: '', lastName: '', publicInfo: false, socialMedia: { linkedin: '', publicSocialMedia: false } };

  newTicketId = '';
  addTicketError = signal<string | null>(null);
  ticketDetails = signal<Map<string, TicketDetail>>(new Map());

  constructor(
    private authService: AuthService,
    private clientService: ClientService,
    private eventService: EventService
  ) {}

  ngOnInit(): void {
    const userEmail = this.authService.getUserEmail();
    if (!userEmail) {
      this.error.set('Nu s-a putut determina emailul utilizatorului. Reconectati-va.');
      this.mode.set('view');
      return;
    }
    this.email = userEmail;
    this.loadProfile();
  }

  private loadTicketDetails(tickets: string[]): void {
    tickets.forEach(id => {
      this.eventService.getTicketDetail(id).subscribe({
        next: (detail) => this.ticketDetails.update(m => new Map(m).set(id, detail)),
        error: () => {}
      });
    });
  }

  private loadProfile(): void {
    this.mode.set('loading');
    this.clientService.getProfile(this.email).subscribe({
      next: (data) => {
        this.profile.set(data);
        this.mode.set('view');
        if (data.tickets?.length) {
          this.loadTicketDetails(data.tickets);
        }
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.createForm.email = this.email;
          this.mode.set('create');
        } else {
          this.error.set('Eroare la incarcarea profilului.');
          this.mode.set('view');
        }
      }
    });
  }

  submitCreate(): void {
    this.error.set(null);
    this.clientService.createProfile(this.createForm).subscribe({
      next: (data) => {
        this.profile.set(data);
        this.mode.set('view');
        this.success.set('Profil creat cu succes!');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.error?.error ?? 'Eroare la crearea profilului.');
      }
    });
  }

  enterEdit(): void {
    const p = this.profile();
    if (!p) return;
    this.editForm = {
      firstName: p.firstName ?? '',
      lastName: p.lastName ?? '',
      publicInfo: p.publicInfo ?? false,
      socialMedia: {
        linkedin: p.socialMedia?.linkedin ?? '',
        publicSocialMedia: p.socialMedia?.publicSocialMedia ?? false
      }
    };
    this.mode.set('edit');
    this.error.set(null);
    this.success.set(null);
  }

  cancelEdit(): void {
    this.mode.set('view');
    this.error.set(null);
  }

  submitEdit(): void {
    this.error.set(null);
    this.clientService.updateProfile(this.email, this.editForm).subscribe({
      next: (data) => {
        this.profile.set(data);
        this.mode.set('view');
        this.success.set('Profil actualizat!');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.error?.error ?? 'Eroare la actualizarea profilului.');
      }
    });
  }

  addTicket(): void {
    this.addTicketError.set(null);
    if (!this.newTicketId.trim()) return;
    this.clientService.addTicket(this.email, this.newTicketId.trim()).subscribe({
      next: (tickets) => {
        const p = this.profile();
        if (p) this.profile.set({ ...p, tickets });
        this.loadTicketDetails([this.newTicketId.trim()]);
        this.newTicketId = '';
        this.success.set('Bilet adaugat!');
      },
      error: (err: HttpErrorResponse) => {
        this.addTicketError.set(err.error?.error ?? 'Eroare la adaugarea biletului.');
      }
    });
  }
}
