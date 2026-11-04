import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API } from '../constants/api-endpoint.constant';
import { ApiResponse, PageResponse } from '../models/api-response.model';
import {
  ExportJobResponse,
  TransactionLogResponse,
  TransactionSearchRequest
} from '../models/transaction.model';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  search(req: TransactionSearchRequest): Observable<ApiResponse<PageResponse<TransactionLogResponse>>> {
    return this.http.post<ApiResponse<PageResponse<TransactionLogResponse>>>(
      API.TRANSACTIONS.SEARCH,
      req
    );
  }

  requestExport(req: TransactionSearchRequest): Observable<ApiResponse<ExportJobResponse>> {
    return this.http.post<ApiResponse<ExportJobResponse>>(
      API.TRANSACTIONS.EXPORT,
      req
    );
  }

  getMyExportJobs(): Observable<ApiResponse<ExportJobResponse[]>> {
    return this.http.get<ApiResponse<ExportJobResponse[]>>(API.TRANSACTIONS.MY_JOBS);
  }

  getExportJobStatus(jobId: string): Observable<ApiResponse<ExportJobResponse>> {
    return this.http.get<ApiResponse<ExportJobResponse>>(API.TRANSACTIONS.JOB_DETAIL(jobId));
  }

  /**
   * Tạo URL tải xuống trực tiếp kèm JWT để kích hoạt trình quản lý tải xuống gốc của trình duyệt (Chrome Download Manager)
   * mà không cần nạp toàn bộ file lớn vào bộ nhớ RAM của trình duyệt.
   */
  buildDirectDownloadUrl(jobId: string): string {
    const token = this.authService.getToken() || '';
    return `${API.TRANSACTIONS.DOWNLOAD(jobId)}?token=${encodeURIComponent(token)}`;
  }
}

