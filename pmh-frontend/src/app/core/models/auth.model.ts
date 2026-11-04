// --- Requests ---
export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

// --- Response ---
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;        // Luôn là "Bearer"
  username: string;
  fullName: string;
  permissions: string[];    // Danh sách permission code, vd: ["COMPONENT_VIEW", "COMPONENT_CREATE"]
}
