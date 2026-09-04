import { Component, OnInit, signal, DestroyRef, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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

  private destroyRef = inject(DestroyRef);

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

    this.eventService.getEvent(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (event) => {
          this.event.set(event);

          // HATEOAS: Transmitem obiectul `event` primit, nu `id`
          this.eventService.listPackages(event)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (pkgs) => {
                this.packages.set(pkgs);
                this.loading.set(false);
              },
              error: () => this.loading.set(false)
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
    const currentEvent = this.event();
    if (!currentEvent) return;
    if (!confirm('Esti sigur ca vrei sa stergi acest eveniment?')) return;

    // HATEOAS: Transmitem obiectul `currentEvent`
    this.eventService.deleteEvent(currentEvent)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
    const currentEvent = this.event();
    if (!currentEvent) return;

    const valid = this.packageDrafts().filter(d => d.name?.trim() && d.seatCount != null && d.seatCount >= 1);
    if (valid.length === 0) {
      this.packageErrors.set(['Completeaza cel putin un pachet cu nume si numar de locuri.']);
      return;
    }

    this.packageSaving.set(true);
    this.packageErrors.set([]);

    // HATEOAS: Transmitem `currentEvent` la createPackage
    forkJoin(valid.map(d => this.eventService.createPackage(currentEvent, {
      name: d.name,
      location: d.location || undefined,
      description: d.description || undefined,
      seatCount: d.seatCount ?? undefined
    })))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
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
    if (!confirm(`Stergi pachetul "${pkg.name}"?`)) return;

    // HATEOAS: Transmitem resursa `pkg` (EventPackage) care conține link-ul de ștergere
    this.eventService.deletePackage(pkg)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.packages.update(pkgs => pkgs.filter(p => p !== pkg)),
        error: (err) => this.error.set(err.error?.error ?? 'Eroare la stergerea pachetului')
      });
  }

  toggleTickets(pkg: EventPackage): void {
    const pkgId = pkg.packageResponseId;
    const expanded = new Set(this.expandedPackages());

    if (expanded.has(pkgId)) {
      expanded.delete(pkgId);
      this.expandedPackages.set(expanded);
      return;
    }
    expanded.add(pkgId);
    this.expandedPackages.set(expanded);

    if (!this.ticketsMap()[pkgId]) {
      // HATEOAS: Transmitem resursa `pkg` (EventPackage)
      this.eventService.listTickets(pkg)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (tickets) => this.ticketsMap.update(m => ({ ...m, [pkgId]: tickets }))
        });
    }
  }

  isPackageExpanded(pkgId: number): boolean {
    return this.expandedPackages().has(pkgId);
  }

  purchase(pkg: EventPackage): void {
    // HATEOAS: Transmitem resursa `pkg`
    this.eventService.purchaseTicket(pkg)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (ticket) => {
          this.packages.update(pkgs => pkgs.map(p =>
            p === pkg
              ? { ...p, availableSeats: (p.availableSeats ?? 1) - 1 }
              : p
          ));
          const email = this.authService.getUserEmail();
          if (!email) {
            this.purchaseMessage.set(`Bilet cumparat! ID: ${ticket.ticketResponseId}`);
            return;
          }
          this.clientService.addTicket(email, ticket.ticketResponseId)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: () => this.purchaseMessage.set(`Bilet inregistrat in profil! ID: ${ticket.ticketResponseId}`),
              error: () => this.purchaseMessage.set(`Bilet cumparat, dar nu s-a putut inregistra in profil. ID: ${ticket.ticketResponseId}`)
            });
        },
        error: (err) => this.purchaseMessage.set(err.error?.error ?? 'Eroare la cumparare')
      });
  }
}
