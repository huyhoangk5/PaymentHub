import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../constants/api-endpoint.constant';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  PermissionCatalogResponse,
  UpdateUserAccessRequest,
  UserPermissionSummary
} from '../models/permission-management.model';

@Injectable({ providedIn: 'root' })
export class PermissionManagementService {
  constructor(private http: HttpClient) {}

  getUsers(keyword: string, page: number, size: number): Observable<ApiResponse<PageResponse<UserPermissionSummary>>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<ApiResponse<PageResponse<UserPermissionSummary>>>(API.PERMISSIONS_MANAGEMENT.USERS, { params });
  }

  getCatalog(): Observable<ApiResponse<PermissionCatalogResponse>> {
    return this.http.get<ApiResponse<PermissionCatalogResponse>>(API.PERMISSIONS_MANAGEMENT.CATALOG);
  }

  updateUserAccess(userId: number, request: UpdateUserAccessRequest): Observable<ApiResponse<UserPermissionSummary>> {
    return this.http.put<ApiResponse<UserPermissionSummary>>(
      API.PERMISSIONS_MANAGEMENT.UPDATE_USER(userId),
      request
    );
  }
}
