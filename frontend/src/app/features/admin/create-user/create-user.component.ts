import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-create-user',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './create-user.component.html',
  styleUrl: './create-user.component.scss'
})
export class CreateUserComponent {

  form: FormGroup;
  loading = signal(false);
  success = signal<string | null>(null);
  error = signal<string | null>(null);

  roles = ['OWNER_EVENT', 'CLIENT'];

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: ['CLIENT', Validators.required]
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.success.set(null);
    this.error.set(null);

    this.authService.createUser(this.form.value).subscribe({
      next: () => {
        this.success.set('Utilizatorul a fost creat cu succes');
        this.form.reset({ role: 'CLIENT' });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.error ?? 'Eroare la crearea utilizatorului');
        this.loading.set(false);
      }
    });
  }
}
