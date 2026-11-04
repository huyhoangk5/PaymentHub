import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../constants/api-endpoint.constant';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import { AuditLogResponse } from '../models/audit-log.model';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  constructor(private http: HttpClient) {}

  getAuditLogs(
    module: 'COMPONENT' | 'GROUP_CATEGORY',
    recordId: number,
    page = 0,
    size = 10
  ): Observable<ApiResponse<PageResponse<AuditLogResponse>>> {
    const params = new HttpParams()
      .set('module', module)
      .set('recordId', recordId.toString())
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PageResponse<AuditLogResponse>>>(API.AUDIT_LOGS.GET, { params });
  }
}
