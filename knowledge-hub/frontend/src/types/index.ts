// TypeScript types — kept in sync with backend Java models.
// In production, generate these from OpenAPI spec via openapi-typescript.

export type Role = 'USER' | 'EDITOR' | 'ADMIN';

export type Category =
  | 'JAVA' | 'SPRING' | 'DATABASE' | 'KAFKA' | 'CLOUD'
  | 'ALGORITHMS' | 'SYSTEM_DESIGN' | 'AI_ML' | 'INTEGRATION'
  | 'OBSERVABILITY' | 'TESTING' | 'BEHAVIORAL';

export type Priority = 'P0' | 'P1' | 'P2';

export type Status =
  | 'NOT_STARTED' | 'IN_PROGRESS' | 'STUDIED' | 'QUIZ_READY' | 'MASTERED';

export interface Topic {
  id: number;
  name: string;
  description?: string;
  category: Category;
  priority: Priority;
  status: Status;
  createdAt: string;
  updatedAt: string;
  resources?: Resource[];
}

export interface TopicInput {
  name: string;
  description?: string;
  category: Category;
  priority: Priority;
  status: Status;
}

export type ResourceType = 'ARTICLE' | 'VIDEO' | 'BOOK' | 'COURSE' | 'REPO' | 'TALK';

export interface Resource {
  id: number;
  title: string;
  url: string;
  type: ResourceType;
  source?: string;
  createdAt: string;
}

export interface Note {
  id: string;
  topicId: number;
  title: string;
  content: string;
  tags: string[];
  versions: NoteVersion[];
  attachments: Attachment[];
  createdAt: string;
  updatedAt: string;
}

export interface NoteVersion {
  content: string;
  editedBy: string;
  editedAt: string;
  comment: string;
}

export interface Attachment {
  filename: string;
  contentType: string;
  sizeBytes: number;
  storageUrl: string;
}

// ─── Auth types matching AuthDto ──────────────────────────────────
export interface UserInfo {
  id: number;
  username: string;
  email: string;
  roles: Role[];
  lastLoginAt: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  user: UserInfo;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  message: string;
  fieldErrors?: Record<string, string>;
}
