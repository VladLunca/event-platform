import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EventService } from '../../../core/services/event.service';

@Component({
  selector: 'app-edit-event',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './edit-event.component.html',
  styleUrl: './edit-event.component.scss'
})
export class EditEventComponent implements OnInit {

  form: FormGroup;
  loading = signal(false);
  loadingData = signal(true);
  error = signal<string | null>(null);
  eventId!: number;

  constructor(
    private fb: FormBuilder,
    private eventService: EventService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      location: [''],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.eventId = Number(this.route.snapshot.paramMap.get('id'));
    this.eventService.getEvent(this.eventId).subscribe({
      next: (event) => {
        this.form.patchValue({
          name: event.name,
          location: event.location ?? '',
          description: event.description ?? ''
        });
        this.loadingData.set(false);
      },
      error: () => {
        this.error.set('Evenimentul nu a fost gasit');
        this.loadingData.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);

    this.eventService.updateEvent(this.eventId, this.form.value).subscribe({
      next: () => this.router.navigate(['/events', this.eventId]),
      error: (err) => {
        this.error.set(err.error?.error ?? 'Eroare la actualizarea evenimentului');
        this.loading.set(false);
      }
    });
  }
}
