export interface ClientProfile {
  email: string;
  firstName?: string;
  lastName?: string;
  publicInfo?: boolean;
  socialMedia?: {
    linkedin?: string;
    publicSocialMedia?: boolean;
  };
  tickets?: string[];
}
