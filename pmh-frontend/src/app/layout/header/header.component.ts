import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  get fullName(): string {
    return this.authService.getUserInfo()?.fullName || 'Nguyễn Văn A';
  }

  get username(): string {
    return this.authService.getUserInfo()?.username || 'admin';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
