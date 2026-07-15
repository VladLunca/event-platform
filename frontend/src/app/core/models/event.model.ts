export interface EventModel {
  eventResponseId: number;
  name: string;
  location?: string;
  description?: string;
  seatCount?: number;
  _links?: Record<string, { href: string }>;
}

export interface EventPackage {
  packageResponseId: number;
  name: string;
  location?: string;
  description?: string;
  seatCount?: number;
  availableSeats?: number;
  _links?: Record<string, { href: string }>;
}

export interface Ticket {
  ticketResponseId: string;
  ownerUserId?: string;
  _links?: Record<string, { href: string }>;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  totalPages: number;
  totalElements: number;
}
