import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { TuiButton, TuiInput, TuiTextfield } from '@taiga-ui/core';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TuiTextfield, TuiInput, TuiButton],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  loading = false;
  errorMessage = '';

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const { username, password } = this.loginForm.value;

    this.authService.login({ username, password }).subscribe({
      next: () => {
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/components';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại tài khoản hoặc mật khẩu!';
      }
    });
  }
}
