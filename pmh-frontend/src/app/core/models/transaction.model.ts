export interface TransactionLogResponse {
  id: number;
  transactionCode: string;
  accountNo: string;
  amount: number;
  status: string;
  createdAt: string;
  description: string | null;
  referenceNo: string | null;
  updatedAt: string | null;
}

export interface TransactionSearchRequest {
  keyword?: string | null;
  transactionCode?: string | null;
  accountNo?: string | null;
  status?: string | null;
  referenceNo?: string | null;
  fromDate?: string | null;
  toDate?: string | null;
  minAmount?: number | null;
  maxAmount?: number | null;
  page?: number;
  size?: number;
}

export interface ExportJobResponse {
  id?: number;
  jobId: string;
  userId: string;
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | string;
  progress: number;
  fileUrl?: string | null;
  downloadUrl?: string | null;
  totalRecords?: number | null;
  errorMessage?: string | null;
  createdAt?: string;
  updatedAt?: string;
  expired?: boolean;
  filterSummary?: string | null;
}

export interface ExportNotificationEvent {
  jobId: string;
  userId: string;
  status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED' | string;
  progress: number;
  fileUrl?: string | null;
  downloadUrl?: string | null;
  totalRecords?: number | null;
  errorMessage?: string | null;
  updatedAt?: string;
  filterSummary?: string | null;
}

