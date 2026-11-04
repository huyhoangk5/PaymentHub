export const API = {
  AUTH: {
    LOGIN: '/api/auth/login',
    REFRESH_TOKEN: '/api/auth/refresh-token',
  },
  COMPONENTS: {
    SEARCH: '/api/components/search',
    OPTIONS: '/api/components/options',
    CREATE: '/api/components/create',
    UPDATE: (id: number) => `/api/components/update/${id}`,
    COPY: (id: number) => `/api/components/copy/${id}`,
    SEND_APPROVE: (id: number) => `/api/components/send-approve/${id}`,
    APPROVE: (id: number) => `/api/components/approve/${id}`,
    REJECT: (id: number) => `/api/components/reject/${id}`,
    CANCEL: (id: number) => `/api/components/cancel/${id}`,
    DELETE: (id: number) => `/api/components/delete/${id}`,
  },
  GROUP_CATEGORIES: {
    SEARCH: '/api/group-categories/search',
    CREATE: '/api/group-categories/create',
    UPDATE: (id: number) => `/api/group-categories/update/${id}`,
    COPY: (id: number) => `/api/group-categories/copy/${id}`,
    SEND_APPROVE: (id: number) => `/api/group-categories/send-approve/${id}`,
    APPROVE: (id: number) => `/api/group-categories/approve/${id}`,
    REJECT: (id: number) => `/api/group-categories/reject/${id}`,
    CANCEL: (id: number) => `/api/group-categories/cancel/${id}`,
    DELETE: (id: number) => `/api/group-categories/delete/${id}`,
  },
  AUDIT_LOGS: {
    GET: '/api/audit-logs',
  },
  TRANSACTIONS: {
    SEARCH: '/api/transactions/search',
    EXPORT: '/api/transactions/export',
    MY_JOBS: '/api/transactions/export/my-jobs',
    JOB_DETAIL: (jobId: string) => `/api/transactions/export/${jobId}`,
    DOWNLOAD: (jobId: string) => `/api/transactions/export/${jobId}/download`,
  },
} as const;
