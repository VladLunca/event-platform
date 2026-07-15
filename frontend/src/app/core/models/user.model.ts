export type Role = 'ADMIN' | 'OWNER_EVENT' | 'CLIENT';

export interface AuthResponse {
  token: string;
}

export interface TokenPayload {
  sub: string;
  role: Role;
  expiresAt: number;
  issuedAt: number;
}
