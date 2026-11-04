import { ApprovalStatus } from './component.model';

// --- Search Request ---
export interface GroupCategorySearchRequest {
  paramName?: string;           // Single-value filter
  paramValue?: string;          // Single-value filter
  paramType?: string;           // Single-value filter
  componentCode?: string;       // Single-value filter
  status?: number;              // Single-value filter
  isActive?: number;            // Single-value filter
  paramNames?: string[];        // Multi-select (tên thành phần)
  paramValues?: string[];       // Multi-select (giá trị thành phần)
  paramTypes?: string[];        // Multi-select (danh mục theo nhóm)
  componentCodes?: string[];    // Multi-select (cấu phần xử lý)
  statuses?: number[];          // Lọc theo STATUS
  isActives?: number[];         // Lọc theo IS_ACTIVE
  page?: number;                // Mặc định: 0
  size?: number;                // Mặc định: 10
}

// --- Create Request ---
export interface GroupCategoryCreateRequest {
  paramName: string;        // Bắt buộc, max 255 ký tự
  paramValue: string;       // Bắt buộc, max 255 ký tự
  paramType: string;        // Bắt buộc, max 255 ký tự
  description?: string;     // Optional, max 4000 ký tự
  componentCode: string;    // Bắt buộc — tham chiếu đến PMH_COMPONENTS
  isActive: number;         // Bắt buộc: 0 hoặc 1
  effectiveDate: string;    // Bắt buộc, format: yyyy-MM-dd
  endEffectiveDate?: string;// Optional, format: yyyy-MM-dd
}

// --- Update Request ---
export interface GroupCategoryUpdateRequest extends GroupCategoryCreateRequest {
  id: number; // Bắt buộc
}

// --- Response ---
export interface GroupCategoryResponse {
  id: number;
  paramName: string;
  paramValue: string;
  paramType: string;
  description: string | null;
  componentCode: string;
  status: ApprovalStatus;       // Số nguyên: 1, 3, 4, 5, 7
  isActive: number;             // 0 hoặc 1
  isDisplay: number;            // 1 = cho phép xóa; 2 = không cho phép xóa
  newData: string | null;       // JSON string — parse để lấy dữ liệu mới khi xem chi tiết
  effectiveDate: string;        // yyyy-MM-dd
  endEffectiveDate: string | null;
}
