import { ApprovalStatus } from '../models/component.model';

// Map status → label hiển thị
export const APPROVAL_STATUS_LABEL: Record<ApprovalStatus, string> = {
  [ApprovalStatus.NEW]: 'Tạo mới',
  [ApprovalStatus.PENDING]: 'Chờ duyệt',
  [ApprovalStatus.APPROVED]: 'Đã duyệt',
  [ApprovalStatus.REJECTED]: 'Từ chối',
  [ApprovalStatus.CANCELLED]: 'Hủy duyệt',
};

// Map status → badge color class
export const APPROVAL_STATUS_COLOR: Record<ApprovalStatus, string> = {
  [ApprovalStatus.NEW]: 'status-new',             // Vàng
  [ApprovalStatus.PENDING]: 'status-pending',      // Xanh dương
  [ApprovalStatus.APPROVED]: 'status-approved',    // Xanh lá
  [ApprovalStatus.REJECTED]: 'status-rejected',    // Đỏ
  [ApprovalStatus.CANCELLED]: 'status-cancelled',  // Cam
};

// Danh sách status để dùng trong select box filter
export const APPROVAL_STATUS_OPTIONS = [
  { value: ApprovalStatus.NEW, label: '1 - Tạo mới' },
  { value: ApprovalStatus.PENDING, label: '3 - Chờ duyệt' },
  { value: ApprovalStatus.APPROVED, label: '4 - Đã duyệt' },
  { value: ApprovalStatus.REJECTED, label: '5 - Từ chối' },
  { value: ApprovalStatus.CANCELLED, label: '7 - Hủy duyệt' },
];

// Danh sách trạng thái hoạt động cho select box
export const ACTIVE_STATUS_OPTIONS = [
  { value: 1, label: 'Hoạt động' },
  { value: 0, label: 'Không hoạt động' },
];
