// Wrapper chung cho mọi response từ BE
export interface ApiResponse<T> {
  status: number;       // HTTP status code (200, 400, 401, 403, 500...)
  message: string;      // Thông báo (vd: "Thành công")
  data: T;              // Payload thực tế
  timestamp: string;    // ISO datetime string
}

// Wrapper cho response dạng phân trang (Spring Page)
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;       // Số trang hiện tại (0-indexed)
  first: boolean;
  last: boolean;
}
