export type LeadStatus = "NEW" | "CONTACTED" | "QUALIFIED" | "WON" | "LOST";

export interface Lead {
  id: string;
  name: string | null;
  phone: string | null;
  email: string | null;
  source: string;
  message: string | null;
  status: LeadStatus;
  assignedTo: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResp<T> {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
  totalPages: number;
}

export interface Stats {
  total: number;
  active: number;
  won: number;
  lost: number;
  conversionRate: number;
}

export interface MessageItem {
  id: string;
  direction: "INBOUND" | "OUTBOUND";
  channel: string;
  content: string;
  createdAt: string;
}

export interface Settings {
  name: string;
  industry: string | null;
  aiEnabled: boolean;
  autoReplyTemplate: string | null;
  webhookSecretConfigured: boolean;
  whatsappPhoneNumberId: string | null;
  whatsappAccessTokenConfigured: boolean;
  whatsappVerifyTokenConfigured: boolean;
  telegramBotTokenConfigured: boolean;
  telegramChatId: string | null;
  openaiApiKeyConfigured: boolean;
  onboardingCompletedAt: string | null;
}

export interface AuditEvent {
  id: string;
  actor: string | null;
  entityType: string;
  entityId: string;
  action: string;
  details: string | null;
  createdAt: string;
}

export type UserRole = "OWNER" | "ADMIN" | "AGENT" | "VIEWER";

export interface UserView {
  id: string;
  name: string | null;
  email: string;
  role: UserRole;
  enabled: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}
