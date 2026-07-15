export interface ClientProfile {
  email: string;
  firstName?: string;
  lastName?: string;
  publicInfo?: boolean;
  socialMedia?: {
    linkedin?: string;
    public?: boolean;
  };
  tickets?: string[];
}
