import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AllCommunityModule,
  ColDef,
  GridApi,
  GridOptions,
  GridReadyEvent,
  ModuleRegistry
} from 'ag-grid-community';
import {
  TuiAlertService,
  TuiButton,
  TuiDialogService,
  TuiInput,
  TuiTextfield
} from '@taiga-ui/core';
import { TuiPagination } from '@taiga-ui/kit';
import { PolymorpheusComponent } from '@taiga-ui/polymorpheus';

import { PermissionManagementService } from '../../../core/services/permission-management.service';
import {
  PermissionCatalogResponse,
  UserPermissionSummary
} from '../../../core/models/permission-management.model';
import {
  PermissionEditModalComponent,
  PermissionEditModalData
} from '../permission-edit-modal/permission-edit-modal.component';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-permission-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    AgGridAngular,
    TuiButton,
    TuiTextfield,
    TuiInput,
    TuiPagination
  ],
  templateUrl: './permission-list.component.html',
  styleUrls: ['./permission-list.component.scss']
})
export class PermissionListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly permissionService = inject(PermissionManagementService);
  private readonly dialogService = inject(TuiDialogService);
  private readonly alerts = inject(TuiAlertService);
  private readonly cdr = inject(ChangeDetectorRef);

  filterForm: FormGroup = this.fb.group({
    keyword: ['']
  });

  users: UserPermissionSummary[] = [];
  catalog: PermissionCatalogResponse = { roles: [], permissions: [] };
  totalElements = 0;
  totalPages = 0;
  page = 0;
  size = 10;
  loading = false;

  private gridApi?: GridApi;

  columnDefs: ColDef[] = [
    {
      headerName: 'STT',
      valueGetter: (p) => this.page * this.size + ((p.node?.rowIndex ?? 0) + 1),
      width: 68,
      pinned: 'left',
      sortable: false,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' }
    },
    {
      headerName: 'Tài khoản',
      field: 'username',
      minWidth: 160,
      flex: 1,
      cellStyle: { fontWeight: '600', color: '#1e293b' }
    },
    {
      headerName: 'Trạng thái',
      field: 'enabled',
      width: 120,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
      cellRenderer: (params: any) => {
        const active = (params.value || '').toUpperCase() === 'Y';
        const span = document.createElement('span');
        span.className = active ? 'status-pill active' : 'status-pill inactive';
        span.textContent = active ? 'Hoạt động' : 'Đã khóa';
        return span;
      }
    },
    {
      headerName: 'Vai trò',
      field: 'roles',
      minWidth: 190,
      flex: 1.1,
      sortable: false,
      autoHeight: true,
      wrapText: true,
      cellRenderer: (params: any) => {
        const u: UserPermissionSummary = params.data;
        const container = document.createElement('div');
        container.className = 'badge-wrap';
        if (!u?.roles?.length) {
          const empty = document.createElement('span');
          empty.className = 'muted-text';
          empty.textContent = 'Chưa gán vai trò';
          container.appendChild(empty);
          return container;
        }
        u.roles.forEach((r) => {
          const badge = document.createElement('span');
          badge.className = 'role-badge';
          badge.textContent = r.roleCode;
          badge.title = r.roleName;
          container.appendChild(badge);
        });
        return container;
      }
    },
    {
      headerName: 'Quyền hạn',
      field: 'effectivePermissions',
      minWidth: 340,
      flex: 2.2,
      sortable: false,
      autoHeight: true,
      wrapText: true,
      cellRenderer: (params: any) => {
        const u: UserPermissionSummary = params.data;
        const container = document.createElement('div');
        container.className = 'badge-wrap';
        const perms = u?.effectivePermissions || [];
        const granted = new Set(u?.grantedPermissions || []);
        const revoked = u?.revokedPermissions || [];

        if (perms.length === 0 && revoked.length === 0) {
          const empty = document.createElement('span');
          empty.className = 'muted-text';
          empty.textContent = 'Chưa có quyền nào';
          container.appendChild(empty);
          return container;
        }

        const previewLimit = 8;
        perms.slice(0, previewLimit).forEach((p) => {
          const badge = document.createElement('span');
          badge.className = granted.has(p.code) ? 'perm-badge granted' : 'perm-badge';
          badge.textContent = granted.has(p.code) ? `+ ${p.code}` : p.code;
          badge.title = p.name;
          container.appendChild(badge);
        });

        if (perms.length > previewLimit) {
          const more = document.createElement('span');
          more.className = 'perm-badge more';
          more.textContent = `+${perms.length - previewLimit} quyền khác`;
          container.appendChild(more);
        }

        if (revoked.length > 0) {
          const rev = document.createElement('span');
          rev.className = 'perm-badge revoked';
          rev.textContent = `-${revoked.length} thu hồi`;
          rev.title = 'Thu hồi: ' + revoked.join(', ');
          container.appendChild(rev);
        }

        return container;
      }
    },
    {
      headerName: 'Email',
      field: 'email',
      minWidth: 180,
      flex: 1,
      valueFormatter: (params: any) => params.value || '-'
    },
    {
      headerName: 'Thao tác',
      width: 130,
      pinned: 'right',
      sortable: false,
      cellStyle: { display: 'flex', alignItems: 'center', justifyContent: 'center' },
      cellRenderer: (params: any) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'grid-edit-btn';
        btn.textContent = 'Chỉnh sửa';
        btn.addEventListener('click', () => {
          this.openEditModal(params.data);
        });
        return btn;
      }
    }
  ];

  gridOptions: GridOptions = {
    theme: 'legacy',
    domLayout: 'autoHeight',
    rowHeight: 72,
    headerHeight: 44,
    suppressRowClickSelection: true,
    defaultColDef: {
      resizable: true,
      sortable: true
    }
  };

  ngOnInit(): void {
    this.loadInitialData();
  }

  onGridReady(params: GridReadyEvent): void {
    this.gridApi = params.api;
  }

  loadInitialData(): void {
    this.loading = true;
    const keyword = this.filterForm.value.keyword || '';
    forkJoin({
      usersRes: this.permissionService.getUsers(keyword, this.page, this.size),
      catalogRes: this.permissionService.getCatalog()
    }).subscribe({
      next: ({ usersRes, catalogRes }) => {
        this.catalog = catalogRes.data || { roles: [], permissions: [] };
        this.applyUserPage(usersRes.data);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.alerts
          .open(err?.error?.message || 'Không thể tải dữ liệu phân quyền', { appearance: 'negative' })
          .subscribe();
        this.cdr.detectChanges();
      }
    });
  }

  loadUsers(): void {
    this.loading = true;
    const keyword = this.filterForm.value.keyword || '';
    this.permissionService.getUsers(keyword, this.page, this.size).subscribe({
      next: (res) => {
        this.applyUserPage(res.data);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.alerts
          .open(err?.error?.message || 'Không thể tải danh sách tài khoản', { appearance: 'negative' })
          .subscribe();
        this.cdr.detectChanges();
      }
    });
  }

  onSearch(): void {
    this.page = 0;
    this.loadUsers();
  }

  onReset(): void {
    this.filterForm.reset({ keyword: '' });
    this.page = 0;
    this.loadUsers();
  }

  onPageChange(newPage: number): void {
    this.page = newPage;
    this.loadUsers();
  }

  openEditModal(user: UserPermissionSummary): void {
    const dialogData: PermissionEditModalData = {
      user,
      catalog: this.catalog
    };

    this.dialogService
      .open<boolean>(new PolymorpheusComponent(PermissionEditModalComponent), {
        data: dialogData,
        size: 'l',
        dismissible: true
      })
      .subscribe((saved) => {
        if (saved) {
          this.loadUsers();
        }
      });
  }

  private applyUserPage(data: any): void {
    this.users = data?.content || [];
    this.totalElements = data?.totalElements || 0;
    this.totalPages = data?.totalPages || 0;
    if (this.gridApi) {
      this.gridApi.setGridOption('rowData', this.users);
    }
  }
}
