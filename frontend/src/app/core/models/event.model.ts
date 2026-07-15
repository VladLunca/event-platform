export interface EventModel {
  id: number;
  name: string;
  location?: string;
  description?: string;
  seatCount?: number;
  _links?: Record<string, { href: string }>;
}

export interface EventPackage {
  id: number;
  name: string;
  location?: string;
  description?: string;
  seatCount?: number;
  _links?: Record<string, { href: string }>;
}

export interface Ticket {
  code: string;
  event?: string;
  location?: string;
  packageEvents?: string[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  totalPages: number;
  totalElements: number;
}
