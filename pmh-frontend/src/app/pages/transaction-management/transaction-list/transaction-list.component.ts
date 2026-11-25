import { ChangeDetectorRef, Component, inject, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AgGridAngular } from 'ag-grid-angular';
import {
  ColDef,
  GridApi,
  GridOptions,
  GridReadyEvent,
  ModuleRegistry,
  AllCommunityModule
} from 'ag-grid-community';
import {
  TuiAlertService,
  TuiButton,
  TuiDataList,
  TuiInput,
  TuiTextfield
} from '@taiga-ui/core';
import {
  TuiChevron,
  TuiDataListWrapper,
  TuiInputDate,
  TuiPagination,
  TuiSelect
} from '@taiga-ui/kit';
import { TuiDay } from '@taiga-ui/cdk';
import { TransactionService } from '../../../core/services/transaction.service';
import { ExportWebsocketService } from '../../../core/services/export-websocket.service';
import {
  ExportJobResponse,
  ExportNotificationEvent,
  TransactionLogResponse,
  TransactionSearchRequest
} from '../../../core/models/transaction.model';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AgGridAngular,
    TuiButton,
    TuiTextfield,
    TuiInput,
    TuiSelect,
    TuiDataList,
    TuiDataListWrapper,
    TuiChevron,
    TuiInputDate,
    TuiPagination
  ],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.scss']
})
export class TransactionListComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly transactionService = inject(TransactionService);
  private readonly wsService = inject(ExportWebsocketService);
  private readonly alerts = inject(TuiAlertService);
  private readonly cdr = inject(ChangeDetectorRef);

  filterForm!: FormGroup;
  transactions: TransactionLogResponse[] = [];
  totalElements = 0;
  totalPages = 0;
  page = 0;
  size = 10;
  loading = false;
  exportRequesting = false;

  /**
   * Danh sách các Export Job của người dùng hiện tại (kết hợp từ DB & cập nhật thời gian thực qua WebSocket).
   */
  exportJobs: ExportJobResponse[] = [];
  showExportPanel = true;
  private wsSub?: Subscription;

  statusOptions = [
    { value: 'SUCCESS', label: 'Thành công (SUCCESS)' },
    { value: 'FAILED', label: 'Thất bại (FAILED)' }
  ];
  statusSelectItems: string[] = this.statusOptions.map((o) => o.value);
  readonly stringifyStatus = (value: string): string => {
    return this.statusOptions.find((o) => o.value === value)?.label ?? String(value ?? '');
  };

  private gridApi!: GridApi;

  private formatDateTime(value: string | null | undefined): string {
    if (!value) return '-';
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  private formatCurrency(value: number | null | undefined): string {
    if (value === null || value === undefined) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  private statusCellRenderer(params: any): string {
    const val = String(params.value ?? '').toUpperCase();
    const statusMap: Record<string, { label: string; color: string; bg: string; dot: string }> = {
      SUCCESS: { label: 'Thành công', color: '#1a7a3a', bg: '#e8f5ec', dot: '#1a7a3a' },
      FAILED:  { label: 'Thất bại',   color: '#b51a1a', bg: '#fbe8e8', dot: '#d92626' }
    };
    const s = statusMap[val] || { label: val || '-', color: '#666666', bg: '#f0f0f0', dot: '#888888' };
    return `<span style="display:inline-flex;align-items:center;justify-content:center;gap:6px;width:120px;height:26px;box-sizing:border-box;border-radius:13px;font-size:12px;font-weight:600;line-height:1;background:${s.bg};color:${s.color}"><span style="width:6px;height:6px;border-radius:50%;background:${s.dot};flex-shrink:0;"></span><span>${s.label}</span></span>`;
  }

  columnDefs: ColDef[] = [
    {
      headerName: 'STT',
      valueGetter: (p: any) => this.page * this.size + (p.node!.rowIndex! + 1),
      width: 75,
      pinned: 'left',
      sortable: false,
      filter: false,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' }
    },
    {
      headerName: 'Mã giao dịch',
      field: 'transactionCode',
      width: 195,
      pinned: 'left',
      cellStyle: { display: 'flex', alignItems: 'center', fontWeight: '600', color: '#1f2937' }
    },
    {
      headerName: 'Số tài khoản',
      field: 'accountNo',
      width: 150,
      pinned: 'left',
      cellStyle: { display: 'flex', alignItems: 'center' }
    },
    {
      headerName: 'Trạng thái',
      field: 'status',
      width: 155,
      pinned: 'left',
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
      cellRenderer: (p: any) => this.statusCellRenderer(p)
    },
    {
      headerName: 'Số tiền (VNĐ)',
      field: 'amount',
      width: 165,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'flex-end', fontWeight: '600', color: '#0f172a' },
      valueFormatter: (p: any) => this.formatCurrency(p.value)
    },
    {
      headerName: 'Mã tham chiếu',
      field: 'referenceNo',
      width: 170,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => p.value || '-'
    },
    {
      headerName: 'Mô tả',
      field: 'description',
      minWidth: 220,
      flex: 1,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => p.value || '-'
    },
    {
      headerName: 'Thời gian giao dịch',
      field: 'createdAt',
      width: 175,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => this.formatDateTime(p.value)
    },
    {
      headerName: 'Ngày cập nhật',
      field: 'updatedAt',
      width: 175,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => this.formatDateTime(p.value)
    }
  ];

  gridOptions: GridOptions = {
    theme: 'legacy',
    pagination: false,
    suppressRowClickSelection: true,
    domLayout: 'autoHeight',
    rowHeight: 54
  };

  ngOnInit(): void {
    this.filterForm = this.fb.group({
      transactionCode: [''],
      accountNo: [''],
      status: [null],
      fromDate: [null],
      toDate: [null]
    });

    this.loadData();
    this.loadMyExportJobs();
    this.initWebSocket();
  }

  ngOnDestroy(): void {
    if (this.wsSub) {
      this.wsSub.unsubscribe();
    }
  }

  onGridReady(params: GridReadyEvent): void {
    this.gridApi = params.api;
  }

  private initWebSocket(): void {
    this.wsService.connect();
    this.wsSub = this.wsService.notifications$.subscribe((event: ExportNotificationEvent) => {
      this.handleRealtimeExportEvent(event);
    });
  }

  private handleRealtimeExportEvent(event: ExportNotificationEvent): void {
    const idx = this.exportJobs.findIndex((j) => j.jobId === event.jobId);
    if (idx >= 0) {
      this.exportJobs[idx] = {
        ...this.exportJobs[idx],
        status: event.status,
        progress: event.progress,
        fileUrl: event.fileUrl ?? this.exportJobs[idx].fileUrl,
        downloadUrl: event.downloadUrl ?? this.exportJobs[idx].downloadUrl,
        totalRecords: event.totalRecords ?? this.exportJobs[idx].totalRecords,
        errorMessage: event.errorMessage ?? this.exportJobs[idx].errorMessage,
        updatedAt: event.updatedAt ?? this.exportJobs[idx].updatedAt,
        filterSummary: event.filterSummary ?? this.exportJobs[idx].filterSummary
      };
    } else {
      this.exportJobs.unshift({
        jobId: event.jobId,
        userId: event.userId,
        status: event.status,
        progress: event.progress,
        fileUrl: event.fileUrl,
        downloadUrl: event.downloadUrl,
        totalRecords: event.totalRecords,
        errorMessage: event.errorMessage,
        updatedAt: event.updatedAt,
        filterSummary: event.filterSummary
      });
    }

    if (event.status === 'SUCCESS') {
      this.alerts
        .open(
          `File Excel (${(event.totalRecords ?? 0).toLocaleString('vi-VN')} bản ghi) đã sẵn sàng tải xuống!`,
          { label: 'Xuất file thành công', appearance: 'positive', autoClose: 5000 }
        )
        .subscribe();
    } else if (event.status === 'FAILED') {
      this.alerts
        .open(event.errorMessage || 'Quá trình xuất file Excel thất bại', {
          label: 'Xuất file thất bại',
          appearance: 'negative',
          autoClose: 6000
        })
        .subscribe();
    }

    this.cdr.detectChanges();
  }

  loadMyExportJobs(): void {
    this.transactionService.getMyExportJobs().subscribe({
      next: (res) => {
        if (res.data) {
          this.exportJobs = res.data;
          this.cdr.detectChanges();
        }
      }
    });
  }

  private buildFilterRequest(includePagination: boolean): TransactionSearchRequest {
    const raw = this.filterForm.value;
    const formatTuiDay = (day: TuiDay | null): string | null => {
      if (!day) return null;
      const y = day.year;
      const m = String(day.month + 1).padStart(2, '0');
      const d = String(day.day).padStart(2, '0');
      return `${y}-${m}-${d}`;
    };

    const req: TransactionSearchRequest = {
      transactionCode: raw.transactionCode?.trim() || null,
      accountNo: raw.accountNo?.trim() || null,
      status: raw.status || null,
      fromDate: formatTuiDay(raw.fromDate),
      toDate: formatTuiDay(raw.toDate)
    };

    if (includePagination) {
      req.page = this.page;
      req.size = this.size;
    }
    return req;
  }

  loadData(): void {
    this.loading = true;
    const req = this.buildFilterRequest(true);
    this.transactionService.search(req).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.data) {
          this.transactions = res.data.content || [];
          this.totalElements = res.data.totalElements || 0;
          this.totalPages = res.data.totalPages || 0;
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        const msg = err?.error?.message || 'Không thể tải danh sách lịch sử giao dịch';
        this.alerts.open(msg, { label: 'Lỗi', appearance: 'negative' }).subscribe();
        this.cdr.detectChanges();
      }
    });
  }

  onSearch(): void {
    this.page = 0;
    this.loadData();
  }

  onReset(): void {
    this.filterForm.reset({
      transactionCode: '',
      accountNo: '',
      status: null,
      fromDate: null,
      toDate: null
    });
    this.page = 0;
    this.loadData();
  }

  onPageChange(newPage: number): void {
    this.page = newPage;
    this.loadData();
  }

  /**
   * Gửi yêu cầu Export bất đồng bộ theo đúng các điều kiện lọc hiện tại.
   */
  requestExportExcel(): void {
    if (this.exportRequesting) return;
    this.exportRequesting = true;

    const exportFilter = this.buildFilterRequest(false);
    this.transactionService.requestExport(exportFilter).subscribe({
      next: (res) => {
        this.exportRequesting = false;
        if (res.data) {
          const newJob = res.data;
          this.exportJobs = [newJob, ...this.exportJobs.filter((j) => j.jobId !== newJob.jobId)];
          this.showExportPanel = true;
        }
        this.alerts
          .open('Yêu cầu xuất Excel đã được tiếp nhận. Hệ thống đang xử lý ngầm.', {
            label: 'Đã tiếp nhận yêu cầu',
            appearance: 'info',
            autoClose: 4000
          })
          .subscribe();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.exportRequesting = false;
        const msg = err?.error?.message || 'Không thể tạo yêu cầu xuất Excel';
        this.alerts.open(msg, { label: 'Lỗi', appearance: 'negative' }).subscribe();
        this.cdr.detectChanges();
      }
    });
  }

  getDirectDownloadUrl(jobId: string): string {
    return this.transactionService.buildDirectDownloadUrl(jobId);
  }

  get activeJobsCount(): number {
    return this.exportJobs.filter((j) => j.status === 'PENDING' || j.status === 'PROCESSING').length;
  }

  get startRecord(): number {
    return this.totalElements === 0 ? 0 : this.page * this.size + 1;
  }

  get endRecord(): number {
    return Math.min((this.page + 1) * this.size, this.totalElements);
  }
}

