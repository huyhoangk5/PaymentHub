// --- Enum trạng thái dùng chung ---
export enum ApprovalStatus {
  NEW       = 1,   // Tạo mới
  PENDING   = 3,   // Chờ duyệt
  APPROVED  = 4,   // Đã duyệt
  REJECTED  = 5,   // Từ chối
  CANCELLED = 7,   // Hủy duyệt
}

// --- Search Request ---
export interface ComponentSearchRequest {
  componentCode?: string;     // Single-value filter
  componentName?: string;     // Single-value filter
  status?: number;            // Single-value filter
  isActive?: number;          // Single-value filter (0 hoặc 1)
  componentCodes?: string[];  // Multi-select, optional
  componentNames?: string[];  // Multi-select, optional
  statuses?: number[];        // Lọc theo STATUS
  isActives?: number[];       // Lọc theo IS_ACTIVE (0 hoặc 1)
  page?: number;              // Mặc định: 0
  size?: number;              // Mặc định: 10
}

// --- Create Request ---
export interface ComponentCreateRequest {
  componentCode: string;       // Bắt buộc, max 20 ký tự, chỉ chữ HOA
  componentName: string;       // Bắt buộc, max 150 ký tự
  messageType?: string;        // Optional, max 1500 ký tự
  connectionMethod?: string;   // Optional, max 100 ký tự
  effectiveDate: string;       // Bắt buộc, format: yyyy-MM-dd
  endEffectiveDate?: string;   // Optional, format: yyyy-MM-dd
  checkToken?: boolean;        // Optional, Y/N checkbox
}

// --- Update Request ---
export interface ComponentUpdateRequest extends ComponentCreateRequest {
  id: number;      // Bắt buộc
  status?: number; // Optional, để FE truyền khi cần
}

// --- Response ---
export interface ComponentResponse {
  id: number;
  componentCode: string;
  componentName: string;
  messageType: string | null;
  connectionMethod: string | null;
  checkToken: boolean | null;
  status: ApprovalStatus;       // Số nguyên: 1, 3, 4, 5, 7
  effectiveDate: string;        // yyyy-MM-dd
  endEffectiveDate: string | null;
  isActive: number;             // 0 hoặc 1
  isDispllay: number;           // Lưu ý: BE typo "isDispllay" — giữ nguyên để match JSON
  newData: string | null;       // JSON string — parse để lấy dữ liệu mới khi xem chi tiết
}
