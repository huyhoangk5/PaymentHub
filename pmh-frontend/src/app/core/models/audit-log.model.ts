// --- Response ---
export interface AuditLogResponse {
  id: number;
  performedBy: string;          // Username + họ tên người thực hiện
  actionDate: string;           // Format: "dd/MM/yyyy HH:mm:ss" (BE định dạng sẵn)
  action: string;               // Tên hành động: "Tạo mới", "Gửi duyệt", "Phê duyệt"...
  ipAddress: string;
  description: string;
  statusBefore: number | null;  // Trạng thái trước khi thực hiện
  statusAfter: number | null;   // Trạng thái sau khi thực hiện
}
