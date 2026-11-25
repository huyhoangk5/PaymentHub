import { ChangeDetectorRef, Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import {
  TuiAlertService,
  TuiButton,
  TuiDialogContext,
  TuiInput,
  TuiTextfield
} from '@taiga-ui/core';
import {
  PermissionCatalogResponse,
  PermissionOption,
  RoleOption,
  UserPermissionSummary
} from '../../../core/models/permission-management.model';
import { PermissionManagementService } from '../../../core/services/permission-management.service';
import { AuthService } from '../../../core/services/auth.service';

export interface PermissionEditModalData {
  user: UserPermissionSummary;
  catalog: PermissionCatalogResponse;
}

interface PermissionGroup {
  module: string;
  permissions: PermissionOption[];
}

@Component({
  selector: 'app-permission-edit-modal',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TuiButton,
    TuiTextfield,
    TuiInput
  ],
  templateUrl: './permission-edit-modal.component.html',
  styleUrls: ['./permission-edit-modal.component.scss']
})
export class PermissionEditModalComponent implements OnInit {
  private readonly permissionService = inject(PermissionManagementService);
  private readonly authService = inject(AuthService);
  private readonly alerts = inject(TuiAlertService);
  private readonly cdr = inject(ChangeDetectorRef);

  user!: UserPermissionSummary;
  roles: RoleOption[] = [];
  allPermissions: PermissionOption[] = [];
  filteredGroups: PermissionGroup[] = [];

  selectedRoleIds = new Set<number>();
  selectedPermissionIds = new Set<number>();
  roleBaselineCodes = new Set<string>();

  searchText = '';
  saving = false;

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    private readonly context: TuiDialogContext<boolean, PermissionEditModalData>
  ) {}

  ngOnInit(): void {
    this.user = this.context.data.user;
    this.roles = this.context.data.catalog.roles || [];
    this.allPermissions = this.context.data.catalog.permissions || [];

    this.user.roles.forEach((r) => this.selectedRoleIds.add(r.id));
    this.user.effectivePermissions.forEach((p) => this.selectedPermissionIds.add(p.id));
    this.recalculateRoleBaseline();
    this.rebuildGroups();
  }

  get roleBaselineCount(): number {
    let count = 0;
    for (const perm of this.allPermissions) {
      if (this.roleBaselineCodes.has(perm.code)) {
        count++;
      }
    }
    return count;
  }

  get grantedCount(): number {
    let count = 0;
    for (const perm of this.allPermissions) {
      if (this.selectedPermissionIds.has(perm.id) && !this.roleBaselineCodes.has(perm.code)) {
        count++;
      }
    }
    return count;
  }

  get revokedCount(): number {
    let count = 0;
    for (const perm of this.allPermissions) {
      if (!this.selectedPermissionIds.has(perm.id) && this.roleBaselineCodes.has(perm.code)) {
        count++;
      }
    }
    return count;
  }

  onSearchChange(value: string): void {
    this.searchText = value;
    this.rebuildGroups();
    this.cdr.markForCheck();
  }

  trackByGroup(_: number, group: PermissionGroup): string {
    return group.module;
  }

  trackByPerm(_: number, perm: PermissionOption): number {
    return perm.id;
  }

  toggleRole(role: RoleOption, checked: boolean): void {
    if (checked) {
      this.selectedRoleIds.add(role.id);
    } else {
      this.selectedRoleIds.delete(role.id);
    }

    const prevBaseline = new Set(this.roleBaselineCodes);
    this.recalculateRoleBaseline();

    for (const perm of this.allPermissions) {
      const wasInRole = prevBaseline.has(perm.code);
      const nowInRole = this.roleBaselineCodes.has(perm.code);
      if (!wasInRole && nowInRole) {
        this.selectedPermissionIds.add(perm.id);
      } else if (wasInRole && !nowInRole) {
        this.selectedPermissionIds.delete(perm.id);
      }
    }
    this.cdr.markForCheck();
  }

  togglePermission(permId: number, checked: boolean): void {
    if (checked) {
      this.selectedPermissionIds.add(permId);
    } else {
      this.selectedPermissionIds.delete(permId);
    }
    this.cdr.markForCheck();
  }

  isGroupAllSelected(group: PermissionGroup): boolean {
    return group.permissions.length > 0 && group.permissions.every((p) => this.selectedPermissionIds.has(p.id));
  }

  toggleGroup(group: PermissionGroup, checked: boolean): void {
    for (const perm of group.permissions) {
      if (checked) {
        this.selectedPermissionIds.add(perm.id);
      } else {
        this.selectedPermissionIds.delete(perm.id);
      }
    }
    this.cdr.markForCheck();
  }

  resetToRoleDefaults(): void {
    this.selectedPermissionIds.clear();
    for (const perm of this.allPermissions) {
      if (this.roleBaselineCodes.has(perm.code)) {
        this.selectedPermissionIds.add(perm.id);
      }
    }
    this.cdr.markForCheck();
  }

  getPermissionStatus(perm: PermissionOption): 'GRANTED' | 'REVOKED' | 'ROLE' | 'NONE' {
    const selected = this.selectedPermissionIds.has(perm.id);
    const inRole = this.roleBaselineCodes.has(perm.code);
    if (selected && !inRole) return 'GRANTED';
    if (!selected && inRole) return 'REVOKED';
    if (selected && inRole) return 'ROLE';
    return 'NONE';
  }

  save(): void {
    this.saving = true;
    const payload = {
      roleIds: Array.from(this.selectedRoleIds),
      permissionIds: Array.from(this.selectedPermissionIds)
    };

    this.permissionService.updateUserAccess(this.user.id, payload).subscribe({
      next: () => {
        const currentUser = this.authService.getUserInfo();
        if (currentUser && currentUser.username === this.user.username) {
          this.authService.refreshToken().subscribe({ error: () => {} });
        }
        this.saving = false;
        this.alerts.open('Cập nhật phân quyền thành công!', { appearance: 'positive' }).subscribe();
        this.context.completeWith(true);
      },
      error: (err) => {
        this.saving = false;
        this.cdr.markForCheck();
        this.alerts
          .open(err?.error?.message || 'Không thể cập nhật phân quyền', { appearance: 'negative' })
          .subscribe();
      }
    });
  }

  cancel(): void {
    this.context.completeWith(false);
  }

  private rebuildGroups(): void {
    const query = (this.searchText || '').trim().toLowerCase();
    const map = new Map<string, PermissionOption[]>();

    for (const perm of this.allPermissions) {
      if (query) {
        const matchCode = perm.code.toLowerCase().includes(query);
        const matchName = (perm.name || '').toLowerCase().includes(query);
        const matchModule = (perm.module || '').toLowerCase().includes(query);
        if (!matchCode && !matchName && !matchModule) {
          continue;
        }
      }
      const mod = perm.module || 'OTHER';
      if (!map.has(mod)) {
        map.set(mod, []);
      }
      map.get(mod)!.push(perm);
    }

    this.filteredGroups = Array.from(map.entries()).map(([module, permissions]) => ({
      module,
      permissions
    }));
  }

  private recalculateRoleBaseline(): void {
    this.roleBaselineCodes.clear();
    for (const role of this.roles) {
      if (this.selectedRoleIds.has(role.id)) {
        (role.permissionCodes || []).forEach((code) => this.roleBaselineCodes.add(code));
      }
    }
  }
}
