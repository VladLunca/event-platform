import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EventService } from '../../../core/services/event.service';
import { AuthService } from '../../../core/services/auth.service';
import { EventModel, EventPackage, Ticket } from '../../../core/models/event.model';

@Component({
  selector: 'app-event-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
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
  packageForm: FormGroup;
  packageLoading = signal(false);
  packageError = signal<string | null>(null);

  ticketsMap = signal<Record<number, Ticket[]>>({});
  expandedPackages = signal<Set<number>>(new Set());

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private eventService: EventService,
    public authService: AuthService,
    private fb: FormBuilder
  ) {
    this.packageForm = this.fb.group({
      name: ['', Validators.required],
      location: [''],
      description: [''],
      seatCount: [null, [Validators.required, Validators.min(1)]]
    });
  }

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

  createPackage(): void {
    const eventId = this.event()?.eventResponseId;
    if (!eventId || this.packageForm.invalid) return;
    this.packageLoading.set(true);
    this.packageError.set(null);

    this.eventService.createPackage(eventId, this.packageForm.value).subscribe({
      next: (pkg) => {
        this.packages.update(pkgs => [...pkgs, pkg]);
        this.packageForm.reset();
        this.showPackageForm.set(false);
        this.packageLoading.set(false);
      },
      error: (err) => {
        this.packageError.set(err.error?.error ?? 'Eroare la crearea pachetului');
        this.packageLoading.set(false);
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
      next: () => this.purchaseMessage.set('Bilet cumparat cu succes!'),
      error: (err) => this.purchaseMessage.set(err.error?.error ?? 'Eroare la cumparare')
    });
  }
}
