import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-update-role',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './update-role.component.html',
  styleUrl: './update-role.component.scss'
})
export class UpdateRoleComponent {

  form: FormGroup;
  loading = signal(false);
  success = signal<string | null>(null);
  error = signal<string | null>(null);

  roles = ['OWNER_EVENT', 'CLIENT'];

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['CLIENT', Validators.required]
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.success.set(null);
    this.error.set(null);

    this.authService.updateUserRole(this.form.value).subscribe({
      next: () => {
        this.success.set('Rolul utilizatorului a fost actualizat cu succes');
        this.form.reset({ role: 'CLIENT' });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error ?? 'Eroare la actualizarea rolului');
        this.loading.set(false);
      }
    });
  }
}
