import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../constants/api-endpoint.constant';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  GroupCategorySearchRequest,
  GroupCategoryCreateRequest,
  GroupCategoryUpdateRequest,
  GroupCategoryResponse
} from '../models/group-category.model';

@Injectable({
  providedIn: 'root'
})
export class GroupCategoryService {
  constructor(private http: HttpClient) {}

  search(req: GroupCategorySearchRequest): Observable<ApiResponse<PageResponse<GroupCategoryResponse>>> {
    return this.http.post<ApiResponse<PageResponse<GroupCategoryResponse>>>(API.GROUP_CATEGORIES.SEARCH, req);
  }

  create(req: GroupCategoryCreateRequest): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.CREATE, req);
  }

  update(id: number, req: GroupCategoryUpdateRequest): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.put<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.UPDATE(id), req);
  }

  copy(id: number, newParamName?: string): Observable<ApiResponse<GroupCategoryResponse>> {
    let params = new HttpParams();
    if (newParamName) {
      params = params.set('newParamName', newParamName);
    }
    return this.http.post<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.COPY(id), {}, { params });
  }

  sendApprove(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.SEND_APPROVE(id), {});
  }

  approve(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.APPROVE(id), {});
  }

  reject(id: number, reason?: string): Observable<ApiResponse<GroupCategoryResponse>> {
    let params = new HttpParams();
    if (reason) {
      params = params.set('reason', reason);
    }
    return this.http.post<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.REJECT(id), {}, { params });
  }

  cancel(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.http.post<ApiResponse<GroupCategoryResponse>>(API.GROUP_CATEGORIES.CANCEL(id), {});
  }

  delete(id: number): Observable<ApiResponse<GroupCategoryResponse>> {
    return this.cancel(id);
  }
}
