import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { EventService } from '../../../core/services/event.service';

@Component({
  selector: 'app-create-event',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './create-event.component.html',
  styleUrl: './create-event.component.scss'
})
export class CreateEventComponent {

  form: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private eventService: EventService,
    private router: Router
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      location: [''],
      description: ['']
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.error.set(null);

    this.eventService.createEvent(this.form.value).subscribe({
      next: (event) => {
        const id = event?.eventResponseId;
        if (id != null && id > 0) {
          this.router.navigate(['/events', id]);
        } else {
          this.router.navigate(['/events']);
        }
      },
      error: (err) => {
        this.error.set(err.error?.error ?? 'Eroare la crearea evenimentului');
        this.loading.set(false);
      }
    });
  }
}
