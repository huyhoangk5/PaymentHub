import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { POLYMORPHEUS_CONTEXT, PolymorpheusComponent } from '@taiga-ui/polymorpheus';
import { TuiAlertService, TuiButton, TuiDialogContext, TuiDialogService } from '@taiga-ui/core';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridOptions } from 'ag-grid-community';

import { GroupCategoryResponse } from '../../../core/models/group-category.model';
import { ApprovalStatus } from '../../../core/models/component.model';
import { GroupCategoryService } from '../../../core/services/group-category.service';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { AuditLogResponse } from '../../../core/models/audit-log.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { ConfirmDialogComponent, ConfirmDialogOptions, ConfirmDialogResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { DateFormatPipe } from '../../../shared/pipes/date-format.pipe';
import { ActiveLabelPipe } from '../../../shared/pipes/active-label.pipe';
import { HasPermissionDirective } from '../../../shared/directives/has-permission.directive';

export interface GroupCategoryActionDialogOptions {
  data: GroupCategoryResponse;
}

@Component({
  selector: 'app-group-category-action-modal',
  standalone: true,
  imports: [
    CommonModule,
    StatusBadgeComponent,
    DateFormatPipe,
    ActiveLabelPipe,
    HasPermissionDirective,
    AgGridAngular,
    TuiButton
  ],
  templateUrl: './group-category-action-modal.component.html',
  styleUrls: ['./group-category-action-modal.component.scss']
})
export class GroupCategoryActionModalComponent implements OnInit {
  private groupCategoryService = inject(GroupCategoryService);
  private auditLogService = inject(AuditLogService);
  private dialogService = inject(TuiDialogService);
  private alerts = inject(TuiAlertService);

  newDataParsed: Partial<GroupCategoryResponse> | null = null;
  loading = false;
  errorMessage = '';

  showAuditLogs = false;
  auditLogs: AuditLogResponse[] = [];
  auditLogLoading = false;

  auditLogColumnDefs: ColDef[] = [
    {
      headerName: 'STT',
      valueGetter: (p: any) => p.node!.rowIndex! + 1,
      width: 55,
      sortable: false,
      filter: false,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' }
    },
    {
      headerName: 'Thời gian',
      field: 'actionDate',
      width: 155,
      cellStyle: { display: 'flex', alignItems: 'center' }
    },
    {
      headerName: 'Hành động',
      field: 'action',
      width: 135,
      cellStyle: { display: 'flex', alignItems: 'center' },
      cellRenderer: (p: any) => {
        const actionMap: Record<string, { color: string; bg: string }> = {
          'Tạo bản ghi':      { color: '#1a6fb5', bg: '#e8f1fb' },
          'Tạo mới':          { color: '#1a6fb5', bg: '#e8f1fb' },
          'Gửi duyệt':        { color: '#b57d1a', bg: '#fbf3e8' },
          'Phê duyệt':        { color: '#1a7a3a', bg: '#e8f5ec' },
          'Từ chối':          { color: '#b51a1a', bg: '#fbe8e8' },
          'Hủy phê duyệt':    { color: '#666666', bg: '#f0f0f0' },
          'Sửa':              { color: '#7a1a7a', bg: '#f5e8f5' },
          'Cập nhật':         { color: '#7a1a7a', bg: '#f5e8f5' },
          'Sao chép bản ghi': { color: '#1a5a7a', bg: '#e8f3fb' },
          'Sao chép':         { color: '#1a5a7a', bg: '#e8f3fb' }
        };
        const s = actionMap[p.value] || { color: '#333', bg: '#f0f0f0' };
        return `<span style="display:inline-flex;align-items:center;padding:2px 9px;border-radius:10px;font-size:12px;font-weight:600;background:${s.bg};color:${s.color}">${p.value || '-'}</span>`;
      }
    },
    {
      headerName: 'Người thực hiện',
      field: 'performedBy',
      width: 150,
      cellStyle: { display: 'flex', alignItems: 'center' }
    },
    {
      headerName: 'Mô tả',
      field: 'description',
      flex: 1,
      minWidth: 180,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => p.value || '-'
    },
    {
      headerName: 'IP',
      field: 'ipAddress',
      width: 125,
      cellStyle: { display: 'flex', alignItems: 'center' }
    }
  ];

  auditLogGridOptions: GridOptions = {
    domLayout: 'autoHeight',
    rowHeight: 46,
    suppressRowClickSelection: true,
    pagination: false
  };

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<boolean, GroupCategoryActionDialogOptions>
  ) {}

  get item(): GroupCategoryResponse {
    return this.context.data.data;
  }

  get ApprovalStatus(): typeof ApprovalStatus {
    return ApprovalStatus;
  }

  get isFirstTimeCreate(): boolean {
    return this.item.isDisplay === 1 && !this.newDataParsed && this.item.status !== ApprovalStatus.APPROVED;
  }

  get canDelete(): boolean {
    return (
      this.item.isDisplay === 1 &&
      (this.item.status === ApprovalStatus.NEW || this.item.status === ApprovalStatus.REJECTED)
    );
  }

  ngOnInit(): void {
    if (this.item.newData) {
      try {
        this.newDataParsed = JSON.parse(this.item.newData);
      } catch {
        this.newDataParsed = null;
      }
    }
  }

  isFieldChanged(field: keyof GroupCategoryResponse): boolean {
    if (!this.newDataParsed) return false;
    const newVal = this.newDataParsed[field];
    if (newVal === undefined) return false;
    const normOld = String(this.item[field] ?? '').trim();
    const normNew = String(newVal ?? '').trim();
    return normOld !== normNew;
  }

  close(): void {
    this.context.completeWith(false);
  }

  sendApprove(): void {
    this.loading = true;
    this.groupCategoryService.sendApprove(this.item.id).subscribe({
      next: () => {
        this.loading = false;
        this.alerts.open('Gửi duyệt thành công!', { appearance: 'positive' }).subscribe();
        this.context.completeWith(true);
      },
      error: (err: any) => this.handleError(err)
    });
  }

  deleteItem(): void {
    this.dialogService
      .open<ConfirmDialogResult>(new PolymorpheusComponent(ConfirmDialogComponent), {
        data: {
          title: 'Xóa bản ghi',
          message: `Bạn có chắc chắn muốn xóa bản ghi [${this.item.paramName}]?`,
          confirmText: 'Xóa',
          isDanger: true
        } as ConfirmDialogOptions,
        size: 's'
      })
      .subscribe((result) => {
        if (result?.confirmed) {
          this.loading = true;
          this.groupCategoryService.delete(this.item.id).subscribe({
            next: () => {
              this.loading = false;
              this.alerts.open('Xóa bản ghi thành công!', { appearance: 'positive' }).subscribe();
              this.context.completeWith(true);
            },
            error: (err: any) => this.handleError(err)
          });
        }
      });
  }

  approve(): void {
    this.dialogService
      .open<ConfirmDialogResult>(new PolymorpheusComponent(ConfirmDialogComponent), {
        data: {
          title: 'Phê duyệt tham số danh mục',
          message: `Bạn có chắc chắn muốn phê duyệt bản ghi [${this.item.paramName}]?`,
          confirmText: 'Phê duyệt'
        } as ConfirmDialogOptions,
        size: 's'
      })
      .subscribe((result) => {
        if (result?.confirmed) {
          this.loading = true;
          this.groupCategoryService.approve(this.item.id).subscribe({
            next: () => {
              this.loading = false;
              this.alerts.open('Phê duyệt thành công!', { appearance: 'positive' }).subscribe();
              this.context.completeWith(true);
            },
            error: (err: any) => this.handleError(err)
          });
        }
      });
  }

  reject(): void {
    this.dialogService
      .open<ConfirmDialogResult>(new PolymorpheusComponent(ConfirmDialogComponent), {
        data: {
          title: 'Từ chối phê duyệt',
          message: `Từ chối phê duyệt bản ghi [${this.item.paramName}].`,
          showReasonInput: true,
          reasonMaxLength: 250,
          confirmText: 'Từ chối',
          isDanger: true
        } as ConfirmDialogOptions,
        size: 's'
      })
      .subscribe((result) => {
        if (result?.confirmed) {
          this.loading = true;
          this.groupCategoryService.reject(this.item.id, result.reason).subscribe({
            next: () => {
              this.loading = false;
              this.alerts.open('Từ chối thành công!', { appearance: 'warning' }).subscribe();
              this.context.completeWith(true);
            },
            error: (err: any) => this.handleError(err)
          });
        }
      });
  }

  cancel(): void {
    this.dialogService
      .open<ConfirmDialogResult>(new PolymorpheusComponent(ConfirmDialogComponent), {
        data: {
          title: 'Hủy phê duyệt',
          message: `Bạn có chắc chắn muốn hủy phê duyệt bản ghi [${this.item.paramName}]?`,
          confirmText: 'Hủy phê duyệt',
          isDanger: true
        } as ConfirmDialogOptions,
        size: 's'
      })
      .subscribe((result) => {
        if (result?.confirmed) {
          this.loading = true;
          this.groupCategoryService.cancel(this.item.id).subscribe({
            next: () => {
              this.loading = false;
              this.alerts.open('Hủy phê duyệt thành công!', { appearance: 'warning' }).subscribe();
              this.context.completeWith(true);
            },
            error: (err: any) => this.handleError(err)
          });
        }
      });
  }

  toggleAuditLogs(): void {
    this.showAuditLogs = !this.showAuditLogs;
    if (this.showAuditLogs && this.auditLogs.length === 0) {
      this.auditLogLoading = true;
      this.auditLogService.getAuditLogs('GROUP_CATEGORY', this.item.id).subscribe({
        next: (res) => {
          this.auditLogs = res.data?.content || [];
          this.auditLogLoading = false;
        },
        error: () => {
          this.auditLogLoading = false;
        }
      });
    }
  }

  private handleError(err: any): void {
    this.loading = false;
    this.errorMessage = err?.error?.message || 'Thao tác thất bại. Vui lòng thử lại!';
    this.alerts.open(this.errorMessage, { appearance: 'negative' }).subscribe();
  }
}
