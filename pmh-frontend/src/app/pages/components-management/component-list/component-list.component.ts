import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
  TuiAlertService,
  TuiButton,
  TuiDataList,
  TuiDialogService,
  TuiFilterByInputPipe,
  TuiTextfield
} from '@taiga-ui/core';
import {
  TuiChevron,
  TuiDataListWrapper,
  TuiInputChip,
  TuiMultiSelect,
  TuiPagination
} from '@taiga-ui/kit';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridOptions, GridReadyEvent, GridApi, CellClickedEvent } from 'ag-grid-community';

import { ComponentService } from '../../../core/services/component.service';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { ComponentResponse, ComponentSearchRequest, ApprovalStatus } from '../../../core/models/component.model';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';
import { ComponentFormModalComponent, ComponentFormDialogOptions } from '../component-form-modal/component-form-modal.component';
import { ComponentActionModalComponent, ComponentActionDialogOptions } from '../component-action-modal/component-action-modal.component';
import { AuditLogDialogComponent, AuditLogDialogData } from '../../../shared/components/audit-log-dialog/audit-log-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogOptions, ConfirmDialogResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-component-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HasPermissionDirective,
    AgGridAngular,
    TuiPagination,
    TuiTextfield,
    TuiInputChip,
    TuiMultiSelect,
    TuiDataList,
    TuiDataListWrapper,
    TuiFilterByInputPipe,
    TuiButton,
    TuiChevron
  ],
  templateUrl: './component-list.component.html',
  styleUrls: ['./component-list.component.scss']
})
export class ComponentListComponent implements OnInit {
  private fb = inject(FormBuilder);
  private componentService = inject(ComponentService);
  private auditLogService = inject(AuditLogService);
  private dialogService = inject(TuiDialogService);
  private alerts = inject(TuiAlertService);

  filterForm: FormGroup = this.fb.group({
    componentCodes: [[]],
    componentNames: [[]],
    statuses: [[]],
    isActives: [[]]
  });

  items: ComponentResponse[] = [];
  selectedRows: ComponentResponse[] = [];
  totalElements = 0;
  totalPages = 0;
  page = 0;
  size = 10;
  loading = false;
  bulkLoading = false;

  readonly ApprovalStatus = ApprovalStatus;

  // Dropdown filter options - populated from backend distinct options
  componentCodeOptions: string[] = [];
  componentNameOptions: string[] = [];

  statusOptions = [
    { value: ApprovalStatus.NEW, label: 'Tạo mới' },
    { value: ApprovalStatus.PENDING, label: 'Chờ duyệt' },
    { value: ApprovalStatus.APPROVED, label: 'Đã phê duyệt' },
    { value: ApprovalStatus.REJECTED, label: 'Từ chối' },
    { value: ApprovalStatus.CANCELLED, label: 'Hủy duyệt' }
  ];

  activeOptions = [
    { value: 1, label: 'Hoạt động' },
    { value: 0, label: 'Không hoạt động' }
  ];

  statusSelectItems: number[] = this.statusOptions.map((o) => o.value);
  readonly stringifyStatus = (value: number): string => {
    return this.statusOptions.find((o) => o.value === Number(value))?.label ?? String(value ?? '');
  };

  activeSelectItems: number[] = this.activeOptions.map((o) => o.value);
  readonly stringifyActive = (value: number): string => {
    return this.activeOptions.find((o) => o.value === Number(value))?.label ?? String(value ?? '');
  };

  private gridApi!: GridApi;

  columnDefs: ColDef[] = [
    {
      headerName: 'STT',
      valueGetter: (p: any) => this.page * this.size + (p.node!.rowIndex! + 1),
      width: 70,
      pinned: 'left',
      sortable: false,
      filter: false,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' }
    },
    {
      headerName: 'Mã cấu phần',
      field: 'componentCode',
      width: 140,
      pinned: 'left',
      cellStyle: { display: 'flex', alignItems: 'center', fontWeight: '600', color: '#1f2937' }
    },
    {
      headerName: 'Tên cấu phần',
      field: 'componentName',
      width: 190,
      pinned: 'left',
      cellStyle: { display: 'flex', alignItems: 'center' }
    },
    {
      headerName: 'Trạng thái tham số',
      field: 'status',
      width: 155,
      pinned: 'left',
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
      cellRenderer: (params: any) => {
        const statusMap: Record<number, { label: string; color: string; bg: string; dot: string }> = {
          1: { label: 'Tạo mới',      color: '#1a6fb5', bg: '#e8f1fb', dot: '#1a6fb5' },
          3: { label: 'Chờ duyệt',    color: '#b57d1a', bg: '#fbf3e8', dot: '#d99018' },
          4: { label: 'Đã phê duyệt', color: '#1a7a3a', bg: '#e8f5ec', dot: '#1a7a3a' },
          5: { label: 'Từ chối',      color: '#b51a1a', bg: '#fbe8e8', dot: '#d92626' },
          7: { label: 'Hủy duyệt',    color: '#666666', bg: '#f0f0f0', dot: '#888888' }
        };
        const s = statusMap[params.value] || { label: String(params.value ?? '-'), color: '#666', bg: '#f0f0f0', dot: '#888' };
        return `<span style="display:inline-flex;align-items:center;justify-content:center;gap:6px;width:120px;height:26px;box-sizing:border-box;border-radius:13px;font-size:12px;font-weight:600;line-height:1;background:${s.bg};color:${s.color}"><span style="width:6px;height:6px;border-radius:50%;background:${s.dot};flex-shrink:0;"></span><span>${s.label}</span></span>`;
      }
    },
    {
      headerName: 'Chuẩn tin điện',
      field: 'messageType',
      width: 160,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => p.value || '-'
    },
    {
      headerName: 'Tên kết nối',
      field: 'connectionMethod',
      width: 160,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => p.value || '-'
    },
    {
      headerName: 'Kiểm tra Token/ký số',
      field: 'checkToken',
      width: 160,
      cellStyle: { display: 'flex', alignItems: 'center' },
      cellRenderer: (p: any) => {
        const isChecked = p.value === true || p.value === 1 || p.value === 'Y' || p.value === 'Có';
        if (isChecked) {
          return `<span style="display:inline-flex;align-items:center;gap:5px;color:#0D6B5E;font-weight:500;">Có</span>`;
        }
        return `<span style="display:inline-flex;align-items:center;gap:5px;color:#64748b;">Không</span>`;
      }
    },
    {
      headerName: 'Ngày hiệu lực',
      field: 'effectiveDate',
      width: 140,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => {
        if (!p.value) return '-';
        const parts = String(p.value).split('-');
        return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : p.value;
      }
    },
    {
      headerName: 'Ngày hết hiệu lực',
      field: 'endEffectiveDate',
      width: 150,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => {
        if (!p.value) return '-';
        const parts = String(p.value).split('-');
        return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : p.value;
      }
    },
    {
      headerName: 'Trạng thái hoạt động',
      field: 'isActive',
      width: 160,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => (p.value === 1 ? 'Hoạt động' : 'Không hoạt động')
    },
    {
      headerName: 'Thao tác',
      colId: 'actions',
      width: 140,
      sortable: false,
      filter: false,
      pinned: 'right',
      cellStyle: { display: 'flex', alignItems: 'center' },
      cellRenderer: (params: any) => {
        const item = params.data as ComponentResponse;
        const canEdit = item && (item.status === 1 || item.status === 4 || item.status === 5 || item.status === 7);
        const editBtn = canEdit
          ? `<button data-action="edit" title="Chỉnh sửa" style="background:none;border:none;cursor:pointer;padding:5px 6px;border-radius:4px;color:#555;display:inline-flex;align-items:center;" onmouseover="this.style.background='#e6f2f0';this.style.color='#0D6B5E'" onmouseout="this.style.background='none';this.style.color='#555'"><svg xmlns='http://www.w3.org/2000/svg' width='15' height='15' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.75' stroke-linecap='round' stroke-linejoin='round'><path d='M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z'/><path d='m15 5 4 4'/></svg></button>`
          : '';
        const copyBtn = `<button data-action="copy" title="Sao chép" style="background:none;border:none;cursor:pointer;padding:5px 6px;border-radius:4px;color:#555;display:inline-flex;align-items:center;" onmouseover="this.style.background='#e6f2f0';this.style.color='#0D6B5E'" onmouseout="this.style.background='none';this.style.color='#555'"><svg xmlns='http://www.w3.org/2000/svg' width='15' height='15' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.75' stroke-linecap='round' stroke-linejoin='round'><rect width='14' height='14' x='8' y='8' rx='2' ry='2'/><path d='M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2'/></svg></button>`;
        const detailBtn = `<button data-action="detail" title="Xem chi tiết" style="background:none;border:none;cursor:pointer;padding:5px 6px;border-radius:4px;color:#555;display:inline-flex;align-items:center;" onmouseover="this.style.background='#e6f2f0';this.style.color='#0D6B5E'" onmouseout="this.style.background='none';this.style.color='#555'"><svg xmlns='http://www.w3.org/2000/svg' width='15' height='15' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.75' stroke-linecap='round' stroke-linejoin='round'><path d='M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0'/><circle cx='12' cy='12' r='3'/></svg></button>`;
        const historyBtn = `<button data-action="history" title="Xem lịch sử thao tác" style="background:none;border:none;cursor:pointer;padding:5px 6px;border-radius:4px;color:#555;display:inline-flex;align-items:center;" onmouseover="this.style.background='#e6f2f0';this.style.color='#0D6B5E'" onmouseout="this.style.background='none';this.style.color='#555'"><svg xmlns='http://www.w3.org/2000/svg' width='15' height='15' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.75' stroke-linecap='round' stroke-linejoin='round'><path d='M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8'/><path d='M3 3v5h5'/><path d='M12 7v5l4 2'/></svg></button>`;
        return `<div style="display:flex;align-items:center;gap:2px;">${editBtn}${copyBtn}${detailBtn}${historyBtn}</div>`;
      }
    }
  ];

  gridOptions: GridOptions = {
    theme: 'legacy',
    selectionColumnDef: {
      pinned: 'left',
      width: 48,
      maxWidth: 48,
      minWidth: 48,
      resizable: false,
      suppressHeaderMenuButton: true,
    },
    rowSelection: {
      mode: 'multiRow',
      checkboxes: true,
      headerCheckbox: true,
      enableClickSelection: false,
    },
    pagination: false,
    suppressRowClickSelection: true,
    domLayout: 'autoHeight',
    rowHeight: 54,
    onSelectionChanged: () => {
      if (this.gridApi) {
        this.selectedRows = this.gridApi.getSelectedRows() || [];
      }
    },
    onRowDoubleClicked: (event: any) => {
      if (event.data) this.openDetailModal(event.data);
    },
    onCellClicked: (event: CellClickedEvent) => {
      if (event.column.getColId() !== 'actions') return;
      const target = event.event?.target as HTMLElement;
      const btn = target.closest('[data-action]') as HTMLElement;
      if (!btn) return;
      const action = btn.dataset['action'];
      const item = event.data as ComponentResponse;
      if (action === 'edit')    this.openEditModal(item);
      if (action === 'copy')    this.openCopyModal(item);
      if (action === 'detail')  this.openDetailModal(item);
      if (action === 'history') this.openHistoryModal(item);
    }
  };

  get canBulkDelete(): boolean {
    return (
      this.selectedRows.length > 0 &&
      this.selectedRows.every(
        (r) =>
          ((r as any).isDisplay === 1 || r.isDispllay === 1) &&
          (r.status === ApprovalStatus.NEW || r.status === ApprovalStatus.REJECTED)
      )
    );
  }

  get canBulkSendApprove(): boolean {
    return (
      this.selectedRows.length > 0 &&
      this.selectedRows.every(
        (r) =>
          r.status === ApprovalStatus.NEW ||
          r.status === ApprovalStatus.REJECTED ||
          r.status === ApprovalStatus.CANCELLED
      )
    );
  }

  get canBulkApprove(): boolean {
    return (
      this.selectedRows.length > 0 &&
      this.selectedRows.every((r) => r.status === ApprovalStatus.PENDING)
    );
  }

  get canBulkReject(): boolean {
    return (
      this.selectedRows.length > 0 &&
      this.selectedRows.every((r) => r.status === ApprovalStatus.PENDING)
    );
  }

  get canBulkCancelApprove(): boolean {
    return (
      this.selectedRows.length > 0 &&
      this.selectedRows.every(
        (r) => r.status === ApprovalStatus.PENDING || r.status === ApprovalStatus.APPROVED
      )
    );
  }

  ngOnInit(): void {
    this.loadFilterOptions();
    this.fetchData();
  }

  loadFilterOptions(): void {
    this.componentService.getDistinctOptions().subscribe({
      next: (res) => {
        this.componentCodeOptions = res.data?.componentCodes || [];
        this.componentNameOptions = res.data?.componentNames || [];
      },
      error: () => {
        const req: ComponentSearchRequest = { page: 0, size: 1000 };
        this.componentService.search(req).subscribe({
          next: (res) => {
            const allItems = res.data?.content || [];
            this.componentCodeOptions = [...new Set(allItems.map((i) => i.componentCode).filter(Boolean))];
            this.componentNameOptions = [...new Set(allItems.map((i) => i.componentName).filter(Boolean))];
          }
        });
      }
    });
  }

  fetchData(): void {
    this.loading = true;
    this.selectedRows = [];

    const formVal = this.filterForm.value;
    const req: ComponentSearchRequest = {
      page: this.page,
      size: this.size,
      componentCodes: Array.isArray(formVal.componentCodes) && formVal.componentCodes.length ? formVal.componentCodes : undefined,
      componentNames: Array.isArray(formVal.componentNames) && formVal.componentNames.length ? formVal.componentNames : undefined,
      statuses: Array.isArray(formVal.statuses) && formVal.statuses.length ? formVal.statuses.map(Number) : undefined,
      isActives: Array.isArray(formVal.isActives) && formVal.isActives.length ? formVal.isActives.map(Number) : undefined
    };

    this.componentService.search(req).subscribe({
      next: (res) => {
        this.items = res.data?.content || [];
        this.totalElements = res.data?.totalElements || 0;
        this.totalPages = res.data?.totalPages || 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    this.page = 0;
    this.fetchData();
  }

  onResetFilter(): void {
    this.filterForm.reset({
      componentCodes: [],
      componentNames: [],
      statuses: [],
      isActives: []
    });
    this.page = 0;
    this.fetchData();
  }

  onGridReady(params: GridReadyEvent): void {
    this.gridApi = params.api;
  }

  openCreateModal(): void {
    this.dialogService
      .open<boolean>(new PolymorpheusComponent(ComponentFormModalComponent), {
        data: { mode: 'CREATE' } as ComponentFormDialogOptions,
        size: 'l'
      })
      .subscribe((saved) => {
        if (saved) {
          this.loadFilterOptions();
          this.fetchData();
        }
      });
  }

  openEditModal(item: ComponentResponse, event?: Event): void {
    event?.stopPropagation();
    this.dialogService
      .open<boolean>(new PolymorpheusComponent(ComponentFormModalComponent), {
        data: { mode: 'EDIT', data: item } as ComponentFormDialogOptions,
        size: 'l'
      })
      .subscribe((saved) => {
        if (saved) {
          this.loadFilterOptions();
          this.fetchData();
        }
      });
  }

  openCopyModal(item: ComponentResponse, event?: Event): void {
    event?.stopPropagation();
    this.dialogService
      .open<boolean>(new PolymorpheusComponent(ComponentFormModalComponent), {
        data: { mode: 'COPY', data: item } as ComponentFormDialogOptions,
        size: 'l'
      })
      .subscribe((saved) => {
        if (saved) {
          this.loadFilterOptions();
          this.fetchData();
        }
      });
  }

  openDetailModal(item: ComponentResponse): void {
    this.dialogService
      .open<boolean>(new PolymorpheusComponent(ComponentActionModalComponent), {
        data: { data: item } as ComponentActionDialogOptions,
        size: 'l'
      })
      .subscribe((updated) => {
        if (updated) this.fetchData();
      });
  }

  openHistoryModal(item: ComponentResponse, event?: Event): void {
    event?.stopPropagation();
    this.auditLogService.getAuditLogs('COMPONENT', item.id).subscribe({
      next: (res) => {
        const logs = res.data?.content || [];
        this.dialogService
          .open<void>(new PolymorpheusComponent(AuditLogDialogComponent), {
            data: {
              title: `Lịch sử thao tác — ${item.componentCode}`,
              logs
            } as AuditLogDialogData,
            label: `Lịch sử thao tác — ${item.componentCode}`,
            size: 'l'
          })
          .subscribe();
      },
      error: () => {
        this.dialogService
          .open('Không thể tải lịch sử thao tác.', {
            label: 'Lỗi',
            size: 's'
          })
          .subscribe();
      }
    });
  }

  // Bulk Actions
  onBulkSendApprove(): void {
    const count = this.selectedRows.length;
    this.openConfirm({
      title: 'Xác nhận gửi duyệt',
      message: `Bạn có chắc chắn muốn gửi duyệt ${count} bản ghi đã chọn không?`,
      confirmText: 'Đồng ý',
      cancelText: 'Hủy'
    }).subscribe((res) => {
      if (res?.confirmed) {
        this.bulkLoading = true;
        const calls = this.selectedRows.map((r) => this.componentService.sendApprove(r.id));
        forkJoin(calls).subscribe({
          next: () => {
            this.bulkLoading = false;
            this.alerts.open(`Đã gửi duyệt ${count} bản ghi thành công!`, { appearance: 'positive' }).subscribe();
            this.fetchData();
          },
          error: (err) => {
            this.bulkLoading = false;
            this.alerts.open(err?.error?.message || 'Có lỗi xảy ra khi gửi duyệt hàng loạt!', { appearance: 'negative' }).subscribe();
            this.fetchData();
          }
        });
      }
    });
  }

  onBulkApprove(): void {
    const count = this.selectedRows.length;
    this.openConfirm({
      title: 'Xác nhận phê duyệt',
      message: `Bạn có chắc chắn muốn phê duyệt ${count} bản ghi đã chọn không?`,
      confirmText: 'Đồng ý',
      cancelText: 'Hủy'
    }).subscribe((res) => {
      if (res?.confirmed) {
        this.bulkLoading = true;
        const calls = this.selectedRows.map((r) => this.componentService.approve(r.id));
        forkJoin(calls).subscribe({
          next: () => {
            this.bulkLoading = false;
            this.alerts.open(`Đã phê duyệt ${count} bản ghi thành công!`, { appearance: 'positive' }).subscribe();
            this.fetchData();
          },
          error: (err) => {
            this.bulkLoading = false;
            this.alerts.open(err?.error?.message || 'Có lỗi xảy ra khi phê duyệt hàng loạt!', { appearance: 'negative' }).subscribe();
            this.fetchData();
          }
        });
      }
    });
  }

  onBulkReject(): void {
    const count = this.selectedRows.length;
    this.openConfirm({
      title: 'Xác nhận từ chối duyệt',
      message: `Vui lòng nhập lý do từ chối duyệt cho ${count} bản ghi đã chọn:`,
      showReasonInput: true,
      reasonMaxLength: 250,
      confirmText: 'Từ chối',
      cancelText: 'Hủy',
      isDanger: true
    }).subscribe((res) => {
      if (res?.confirmed) {
        this.bulkLoading = true;
        const calls = this.selectedRows.map((r) => this.componentService.reject(r.id, res.reason));
        forkJoin(calls).subscribe({
          next: () => {
            this.bulkLoading = false;
            this.alerts.open(`Đã từ chối duyệt ${count} bản ghi!`, { appearance: 'positive' }).subscribe();
            this.fetchData();
          },
          error: (err) => {
            this.bulkLoading = false;
            this.alerts.open(err?.error?.message || 'Có lỗi xảy ra khi từ chối duyệt!', { appearance: 'negative' }).subscribe();
            this.fetchData();
          }
        });
      }
    });
  }

  onBulkCancelApprove(): void {
    const count = this.selectedRows.length;
    this.openConfirm({
      title: 'Xác nhận hủy phê duyệt',
      message: `Bạn có chắc chắn muốn hủy phê duyệt ${count} bản ghi đã chọn không?`,
      confirmText: 'Đồng ý',
      cancelText: 'Hủy',
      isDanger: true
    }).subscribe((res) => {
      if (res?.confirmed) {
        this.bulkLoading = true;
        const calls = this.selectedRows.map((r) => this.componentService.cancel(r.id));
        forkJoin(calls).subscribe({
          next: () => {
            this.bulkLoading = false;
            this.alerts.open(`Đã hủy phê duyệt ${count} bản ghi thành công!`, { appearance: 'positive' }).subscribe();
            this.fetchData();
          },
          error: (err) => {
            this.bulkLoading = false;
            this.alerts.open(err?.error?.message || 'Có lỗi xảy ra khi hủy phê duyệt!', { appearance: 'negative' }).subscribe();
            this.fetchData();
          }
        });
      }
    });
  }

  onBulkDelete(): void {
    const count = this.selectedRows.length;
    this.openConfirm({
      title: 'Xác nhận xóa',
      message: `Bạn có chắc chắn muốn xóa ${count} bản ghi chưa phê duyệt đã chọn không?`,
      confirmText: 'Xóa',
      cancelText: 'Hủy',
      isDanger: true
    }).subscribe((res) => {
      if (res?.confirmed) {
        this.bulkLoading = true;
        const calls = this.selectedRows.map((r) => this.componentService.delete(r.id));
        forkJoin(calls).subscribe({
          next: () => {
            this.bulkLoading = false;
            this.alerts.open(`Đã xóa ${count} bản ghi thành công!`, { appearance: 'positive' }).subscribe();
            this.loadFilterOptions();
            this.fetchData();
          },
          error: (err) => {
            this.bulkLoading = false;
            this.alerts.open(err?.error?.message || 'Có lỗi xảy ra khi xóa bản ghi!', { appearance: 'negative' }).subscribe();
            this.fetchData();
          }
        });
      }
    });
  }

  private openConfirm(options: ConfirmDialogOptions) {
    return this.dialogService.open<ConfirmDialogResult>(
      new PolymorpheusComponent(ConfirmDialogComponent),
      { data: options, size: 's' }
    );
  }

  goToPage(p: number): void {
    if (p >= 0 && p < this.totalPages) {
      this.page = p;
      this.fetchData();
    }
  }

  exportExcel(): void {
    if (this.gridApi) {
      this.gridApi.exportDataAsCsv({ fileName: 'cau-phan-xu-ly.csv' });
    }
  }
}
