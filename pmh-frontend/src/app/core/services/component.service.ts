import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../constants/api-endpoint.constant';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  ComponentSearchRequest,
  ComponentCreateRequest,
  ComponentUpdateRequest,
  ComponentResponse
} from '../models/component.model';

export interface ComponentDistinctOptions {
  messageTypes: string[];
  connectionMethods: string[];
  componentCodes: string[];
  componentNames: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ComponentService {
  constructor(private http: HttpClient) {}

  search(req: ComponentSearchRequest): Observable<ApiResponse<PageResponse<ComponentResponse>>> {
    return this.http.post<ApiResponse<PageResponse<ComponentResponse>>>(API.COMPONENTS.SEARCH, req);
  }

  getDistinctOptions(): Observable<ApiResponse<ComponentDistinctOptions>> {
    return this.http.get<ApiResponse<ComponentDistinctOptions>>(API.COMPONENTS.OPTIONS);
  }

  create(req: ComponentCreateRequest): Observable<ApiResponse<ComponentResponse>> {
    return this.http.post<ApiResponse<ComponentResponse>>(API.COMPONENTS.CREATE, req);
  }

  update(id: number, req: ComponentUpdateRequest): Observable<ApiResponse<ComponentResponse>> {
    return this.http.put<ApiResponse<ComponentResponse>>(API.COMPONENTS.UPDATE(id), req);
  }

  copy(id: number, newComponentCode?: string): Observable<ApiResponse<ComponentResponse>> {
    let params = new HttpParams();
    if (newComponentCode) {
      params = params.set('newComponentCode', newComponentCode);
    }
    return this.http.post<ApiResponse<ComponentResponse>>(API.COMPONENTS.COPY(id), {}, { params });
  }

  sendApprove(id: number): Observable<ApiResponse<ComponentResponse>> {
    return this.http.post<ApiResponse<ComponentResponse>>(API.COMPONENTS.SEND_APPROVE(id), {});
  }

  approve(id: number): Observable<ApiResponse<ComponentResponse>> {
    return this.http.post<ApiResponse<ComponentResponse>>(API.COMPONENTS.APPROVE(id), {});
  }

  reject(id: number, reason?: string): Observable<ApiResponse<ComponentResponse>> {
    let params = new HttpParams();
    if (reason) {
      params = params.set('reason', reason);
    }
    return this.http.post<ApiResponse<ComponentResponse>>(API.COMPONENTS.REJECT(id), {}, { params });
  }

  cancel(id: number): Observable<ApiResponse<ComponentResponse>> {
    return this.http.post<ApiResponse<ComponentResponse>>(API.COMPONENTS.CANCEL(id), {});
  }

  delete(id: number): Observable<ApiResponse<ComponentResponse>> {
    return this.cancel(id);
  }
}
