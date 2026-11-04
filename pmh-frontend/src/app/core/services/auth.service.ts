import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API } from '../constants/api-endpoint.constant';
import { ApiResponse } from '../models/api-response.model';
import { LoginRequest, RefreshTokenRequest, TokenResponse } from '../models/auth.model';

const TOKEN_KEY       = 'pmh_access_token';
const REFRESH_KEY     = 'pmh_refresh_token';
const PERMISSIONS_KEY = 'pmh_permissions';
const USER_KEY        = 'pmh_user_info';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<ApiResponse<TokenResponse>> {
    return this.http.post<ApiResponse<TokenResponse>>(API.AUTH.LOGIN, request).pipe(
      tap(res => {
        if (res.data) {
          this.storeTokenData(res.data);
        }
      })
    );
  }

  refreshToken(): Observable<ApiResponse<TokenResponse>> {
    const request: RefreshTokenRequest = {
      refreshToken: this.getRefreshToken() || '',
    };
    return this.http.post<ApiResponse<TokenResponse>>(API.AUTH.REFRESH_TOKEN, request).pipe(
      tap(res => {
        if (res.data) {
          this.storeTokenData(res.data);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(PERMISSIONS_KEY);
    localStorage.removeItem(USER_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  getPermissions(): string[] {
    const raw = localStorage.getItem(PERMISSIONS_KEY);
    return raw ? JSON.parse(raw) : [];
  }

  hasPermission(permission: string): boolean {
    const perms = this.getPermissions();
    if (perms.includes(permission) || perms.includes('ROLE_ADMIN') || perms.includes('ADMIN')) {
      return true;
    }
    // Fallback nếu DB chưa seed quyền GROUP_CATEGORY_* nhưng user đã có quyền COMPONENT_* tương ứng
    if (permission.startsWith('GROUP_CATEGORY_')) {
      const action = permission.replace('GROUP_CATEGORY_', 'COMPONENT_');
      if (perms.includes(action)) {
        return true;
      }
    }
    return false;
  }

  getUserInfo(): { username: string; fullName: string } | null {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private storeTokenData(data: TokenResponse): void {
    localStorage.setItem(TOKEN_KEY, data.accessToken);
    localStorage.setItem(REFRESH_KEY, data.refreshToken);
    localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(data.permissions));
    localStorage.setItem(USER_KEY, JSON.stringify({
      username: data.username,
      fullName: data.fullName,
    }));
  }
}
