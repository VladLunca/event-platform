import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { EventService } from '../../../core/services/event.service';
import { EventModel } from '../../../core/models/event.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-event-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './event-list.component.html',
  styleUrl: './event-list.component.scss'
})
export class EventListComponent implements OnInit {

  events = signal<EventModel[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  searchName = '';
  currentPage = 0;
  pageSize = 10;

  constructor(
    private eventService: EventService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadEvents();
  }

  loadEvents(): void {
    this.loading.set(true);
    this.error.set(null);
    this.eventService.listEvents(this.currentPage, this.pageSize, this.searchName || undefined)
      .subscribe({
        next: (data) => {
          this.events.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Eroare la incarcarea evenimentelor');
          this.loading.set(false);
        }
      });
  }

  search(): void {
    this.currentPage = 0;
    this.loadEvents();
  }

  nextPage(): void {
    this.currentPage++;
    this.loadEvents();
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadEvents();
    }
  }
}
