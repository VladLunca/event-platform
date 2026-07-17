import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClientService } from '../../../core/services/client.service';
import { EventModel, EventPackage, Ticket } from '../../../core/models/event.model';

interface PackageDraft {
  name: string;
  location: string;
  description: string;
  seatCount: number | null;
}

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './event-detail.component.html',
  styleUrl: './event-detail.component.scss'
})
export class EventDetailComponent implements OnInit {

  event = signal<EventModel | null>(null);
  packages = signal<EventPackage[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  purchaseMessage = signal<string | null>(null);

  showPackageForm = signal(false);
  packageDrafts = signal<PackageDraft[]>([]);
  packageSaving = signal(false);
  packageErrors = signal<string[]>([]);

  ticketsMap = signal<Record<number, Ticket[]>>({});
  expandedPackages = signal<Set<number>>(new Set());

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    private clientService: ClientService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.loading.set(true);
    this.eventService.getEvent(id).subscribe({
      next: (event) => {
        this.event.set(event);
        this.eventService.listPackages(id).subscribe({
          next: (pkgs) => {
            this.packages.set(pkgs);
            this.loading.set(false);
          }
        });
      },
      error: () => {
        this.error.set('Evenimentul nu a fost gasit');
        this.loading.set(false);
      }
    });
  }

  isOwner(): boolean {
    return this.authService.hasRole('OWNER_EVENT') || this.authService.hasRole('ADMIN');
  }

  deleteEvent(): void {
    const id = this.event()?.eventResponseId;
    if (!id) return;
    if (!confirm('Esti sigur ca vrei sa stergi acest eveniment?')) return;
    this.eventService.deleteEvent(id).subscribe({
      next: () => this.router.navigate(['/events']),
      error: (err) => this.error.set(err.error?.error ?? 'Eroare la stergere')
    });
  }

  togglePackageForm(): void {
    const next = !this.showPackageForm();
    this.showPackageForm.set(next);
    if (next && this.packageDrafts().length === 0) {
      this.addDraft();
    }
    if (!next) {
      this.packageDrafts.set([]);
      this.packageErrors.set([]);
    }
  }

  addDraft(): void {
    this.packageDrafts.update(drafts => [
      ...drafts,
      { name: '', location: '', description: '', seatCount: null }
    ]);
  }

  removeDraft(index: number): void {
    this.packageDrafts.update(drafts => drafts.filter((_, i) => i !== index));
  }

  saveAllPackages(): void {
    const eventId = this.event()?.eventResponseId;
    if (!eventId) return;

    const valid = this.packageDrafts().filter(d => d.name?.trim() && d.seatCount != null && d.seatCount >= 1);
    if (valid.length === 0) {
      this.packageErrors.set(['Completeaza cel putin un pachet cu nume si numar de locuri.']);
      return;
    }

    this.packageSaving.set(true);
    this.packageErrors.set([]);

    forkJoin(valid.map(d => this.eventService.createPackage(eventId, {
      name: d.name,
      location: d.location || undefined,
      description: d.description || undefined,
      seatCount: d.seatCount ?? undefined
    }))).subscribe({
      next: (created) => {
        this.packages.update(pkgs => [...pkgs, ...created]);
        this.packageDrafts.set([]);
        this.showPackageForm.set(false);
        this.packageSaving.set(false);
      },
      error: (err) => {
        this.packageErrors.set([err.error?.error ?? 'Eroare la crearea pachetelor.']);
        this.packageSaving.set(false);
      }
    });
  }

  deletePackage(pkg: EventPackage): void {
    const eventId = this.event()?.eventResponseId;
    if (!eventId) return;
    if (!confirm(`Stergi pachetul "${pkg.name}"?`)) return;
    this.eventService.deletePackage(eventId, pkg.packageResponseId).subscribe({
      next: () => this.packages.update(pkgs => pkgs.filter(p => p.packageResponseId !== pkg.packageResponseId)),
      error: (err) => this.error.set(err.error?.error ?? 'Eroare la stergerea pachetului')
    });
  }

  toggleTickets(pkg: EventPackage): void {
    const eventId = this.event()?.eventResponseId;
    if (!eventId) return;
    const expanded = new Set(this.expandedPackages());
    if (expanded.has(pkg.packageResponseId)) {
      expanded.delete(pkg.packageResponseId);
      this.expandedPackages.set(expanded);
      return;
    }
    expanded.add(pkg.packageResponseId);
    this.expandedPackages.set(expanded);

    if (!this.ticketsMap()[pkg.packageResponseId]) {
      this.eventService.listTickets(eventId, pkg.packageResponseId).subscribe({
        next: (tickets) => this.ticketsMap.update(m => ({ ...m, [pkg.packageResponseId]: tickets }))
      });
    }
  }

  isPackageExpanded(pkgId: number): boolean {
    return this.expandedPackages().has(pkgId);
  }

  purchase(pkg: EventPackage): void {
    const eventId = this.event()?.eventResponseId;
    if (!eventId) return;
    this.eventService.purchaseTicket(eventId, pkg.packageResponseId).subscribe({
      next: (ticket) => {
        this.packages.update(pkgs => pkgs.map(p =>
          p.packageResponseId === pkg.packageResponseId
            ? { ...p, availableSeats: (p.availableSeats ?? 1) - 1 }
            : p
        ));
        const email = this.authService.getUserEmail();
        if (!email) {
          this.purchaseMessage.set(`Bilet cumparat! ID: ${ticket.ticketResponseId}`);
          return;
        }
        this.clientService.addTicket(email, ticket.ticketResponseId).subscribe({
          next: () => this.purchaseMessage.set(`Bilet inregistrat in profil! ID: ${ticket.ticketResponseId}`),
          error: () => this.purchaseMessage.set(`Bilet cumparat, dar nu s-a putut inregistra in profil. ID: ${ticket.ticketResponseId}`)
        });
      },
      error: (err) => this.purchaseMessage.set(err.error?.error ?? 'Eroare la cumparare')
    });
  }
}
