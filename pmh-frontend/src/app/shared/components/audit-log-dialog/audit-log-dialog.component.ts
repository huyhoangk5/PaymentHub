import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { TuiDialogContext } from '@taiga-ui/core';
import { AgGridAngular } from 'ag-grid-angular';
import { ColDef, GridOptions } from 'ag-grid-community';
import { AuditLogResponse } from '../../../core/models/audit-log.model';

export interface AuditLogDialogData {
  title: string;
  logs: AuditLogResponse[];
}

@Component({
  selector: 'app-audit-log-dialog',
  standalone: true,
  imports: [CommonModule, AgGridAngular],
  templateUrl: './audit-log-dialog.component.html',
  styleUrls: ['./audit-log-dialog.component.scss']
})
export class AuditLogDialogComponent {
  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<void, AuditLogDialogData>
  ) {}

  get data(): AuditLogDialogData {
    return this.context.data;
  }

  columnDefs: ColDef[] = [
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
      width: 150,
      cellStyle: { display: 'flex', alignItems: 'center' }
    },
    {
      headerName: 'Hành động',
      field: 'action',
      width: 130,
      cellStyle: { display: 'flex', alignItems: 'center' },
      cellRenderer: (p: any) => {
        const actionMap: Record<string, { color: string; bg: string }> = {
          'Tạo mới':       { color: '#1a6fb5', bg: '#e8f1fb' },
          'Gửi duyệt':     { color: '#b57d1a', bg: '#fbf3e8' },
          'Phê duyệt':     { color: '#1a7a3a', bg: '#e8f5ec' },
          'Từ chối':       { color: '#b51a1a', bg: '#fbe8e8' },
          'Hủy phê duyệt': { color: '#666666', bg: '#f0f0f0' },
          'Cập nhật':      { color: '#7a1a7a', bg: '#f5e8f5' },
          'Sao chép':      { color: '#1a5a7a', bg: '#e8f3fb' }
        };
        const s = actionMap[p.value] || { color: '#333', bg: '#f0f0f0' };
        return `<span style="display:inline-flex;align-items:center;padding:2px 9px;border-radius:10px;font-size:12px;font-weight:600;background:${s.bg};color:${s.color}">${p.value || '-'}</span>`;
      }
    },
    {
      headerName: 'Người thực hiện',
      field: 'performedBy',
      width: 160,
      cellStyle: { display: 'flex', alignItems: 'center' }
    },
    {
      headerName: 'Mô tả',
      field: 'description',
      flex: 1,
      minWidth: 160,
      cellStyle: { display: 'flex', alignItems: 'center' },
      valueFormatter: (p: any) => p.value || '-'
    },
    {
      headerName: 'IP',
      field: 'ipAddress',
      width: 120,
      cellStyle: { display: 'flex', alignItems: 'center' }
    }
  ];

  gridOptions: GridOptions = {
    domLayout: 'autoHeight',
    rowHeight: 48,
    suppressRowClickSelection: true,
    pagination: false
  };

  close(): void {
    this.context.completeWith();
  }
}
