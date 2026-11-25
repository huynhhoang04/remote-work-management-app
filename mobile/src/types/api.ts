/**
 * Kiểu dữ liệu khớp DTO backend (docs/plan-backend/02-danh-sach-api.md).
 */
export type ProjectRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export interface UserDto {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string | null;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt: string;
  user: UserDto;
}

export interface ProjectDto {
  id: string;
  name: string;
  description?: string | null;
  ownerId: string;
  role: ProjectRole;
  createdAt: string;
}

export interface MemberDto {
  userId: string;
  fullName: string;
  email: string;
  role: ProjectRole;
  joinedAt: string;
}

export interface InviteDto {
  id: string;
  code: string;
  inviteUrl: string;
  expiresAt?: string | null;
}

export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  path?: string;
}

export interface PageDto<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ---------------------------------------------------------------------------
// Task
// ---------------------------------------------------------------------------

export type TaskStatus = 'TODO' | 'DOING' | 'REVIEW' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface LabelDto {
  id: string;
  name: string;
  color: string;
}

export interface TaskAssigneeDto {
  userId: string;
  fullName: string;
}

export interface SubtaskDto {
  id: string;
  title: string;
  done: boolean;
  position: number;
}

export interface FileDto {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  url: string;
  taskId?: string | null;
  messageId?: string | null;
}

export interface TaskDto {
  id: string;
  projectId: string;
  parentTaskId?: string | null;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  startDate?: string | null;
  deadline?: string | null;
  position: number;
  createdBy: string;
  completedAt?: string | null;
  version: number;
  assignees: TaskAssigneeDto[];
  labels: LabelDto[];
  subtasks: SubtaskDto[];
  attachments: FileDto[];
  commentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CommentDto {
  id: string;
  taskId: string;
  userId: string;
  fullName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskPayload {
  projectId: string;
  title: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  startDate?: string | null;
  deadline?: string | null;
  labelIds?: string[];
  assigneeIds?: string[];
  parentTaskId?: string | null;
  position?: number | null;
  version?: number;
}

export type UpdateTaskPayload = Omit<CreateTaskPayload, 'projectId'>;

// ---------------------------------------------------------------------------
// Schedule & stats
// ---------------------------------------------------------------------------

export interface ScheduleEventDto {
  id: string;
  title: string;
  description?: string | null;
  startTime: string;
  endTime: string;
  taskId?: string | null;
  createdAt: string;
}

export interface ScheduleEventPayload {
  title: string;
  description?: string | null;
  startTime: string;
  endTime: string;
  taskId?: string | null;
}

export interface TaskBriefDto {
  id: string;
  projectId: string;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  deadline?: string | null;
}

export interface EventBriefDto {
  id: string;
  title: string;
  startTime: string;
  endTime: string;
  taskId?: string | null;
}

export interface TodayDto {
  overdueTasks: TaskBriefDto[];
  dueTodayTasks: TaskBriefDto[];
  upcomingTasks: TaskBriefDto[];
  todayEvents: EventBriefDto[];
}

export interface CountPointDto {
  period: string;
  count: number;
}

export interface MyStatsDto {
  completedPerDay: CountPointDto[];
  completedPerWeek: CountPointDto[];
  completedPerMonth: CountPointDto[];
  onTimeRate: number;
}

export interface MemberStatDto {
  userId: string;
  fullName: string;
  total: number;
  done: number;
}

export interface ProjectStatsDto {
  totalTasks: number;
  perStatus: Record<string, number>;
  percentDone: number;
  overdueCount: number;
  perMember: MemberStatDto[];
}

// ---------------------------------------------------------------------------
// Notification
// ---------------------------------------------------------------------------

export type NotificationType =
  | 'TASK_ASSIGNED'
  | 'MENTION'
  | 'REMIND_1D'
  | 'REMIND_1H'
  | 'OVERDUE'
  | 'MESSAGE';

export interface NotificationDto {
  id: string;
  type: NotificationType | string;
  title: string;
  body: string;
  refType?: string | null;
  refId?: string | null;
  readAt?: string | null;
  createdAt: string;
}

export interface UnreadCountDto {
  count: number;
}

// ---------------------------------------------------------------------------
// Chat
// ---------------------------------------------------------------------------

export type ConversationType = 'GROUP' | 'DIRECT';

export interface MessageDto {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  replyToId?: string | null;
  clientMessageId?: string | null;
  createdAt: string;
}

export interface ConversationDto {
  id: string;
  type: ConversationType;
  projectId?: string | null;
  name: string;
  lastReadMessageId?: string | null;
  unreadCount: number;
  lastMessage?: MessageDto | null;
  createdAt: string;
}

export interface ReadReceiptDto {
  conversationId: string;
  userId: string;
  lastReadMessageId: string;
  readAt: string;
}

export interface SendMessagePayload {
  content: string;
  replyToId?: string | null;
  clientMessageId?: string | null;
}

// ---------------------------------------------------------------------------
// Audit
// ---------------------------------------------------------------------------

export interface AuditLogDto {
  id: string;
  actorId: string | null;
  actorName?: string | null;
  action: string;
  entityType: string;
  entityId: string;
  metadata?: string | null;
  createdAt: string;
}
