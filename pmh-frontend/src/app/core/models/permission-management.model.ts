export interface UserRoleItem {
  id: number;
  roleCode: string;
  roleName: string;
}

export interface UserPermissionItem {
  id: number;
  code: string;
  name: string;
  module: string;
}

export interface UserPermissionSummary {
  id: number;
  username: string;
  fullName: string | null;
  email: string | null;
  enabled: string | null;
  roles: UserRoleItem[];
  rolePermissions: string[];
  grantedPermissions: string[];
  revokedPermissions: string[];
  effectivePermissions: UserPermissionItem[];
}

export interface RoleOption {
  id: number;
  roleCode: string;
  roleName: string;
  description: string | null;
  permissionCodes: string[];
}

export interface PermissionOption {
  id: number;
  code: string;
  name: string;
  module: string | null;
  description: string | null;
}

export interface PermissionCatalogResponse {
  roles: RoleOption[];
  permissions: PermissionOption[];
}

export interface UpdateUserAccessRequest {
  roleIds?: number[];
  permissionIds: number[];
}
