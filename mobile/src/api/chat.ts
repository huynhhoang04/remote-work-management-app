import type { ConversationDto, MessageDto } from '../types/api';
import { api } from './client';

/** API chat REST — docs/plan-backend/02-danh-sach-api.md §8. */
export const chatApi = {
  conversations(): Promise<ConversationDto[]> {
    return api.get<ConversationDto[]>('/conversations').then((response) => response.data);
  },

  createDirect(userId: string): Promise<ConversationDto> {
    return api
      .post<ConversationDto>('/conversations', { userId })
      .then((response) => response.data);
  },

  messages(conversationId: string, before?: string, size = 30): Promise<MessageDto[]> {
    return api
      .get<MessageDto[]>(`/conversations/${conversationId}/messages`, {
        params: { before: before ?? undefined, size },
      })
      .then((response) => response.data);
  },

  send(
    conversationId: string,
    payload: { content: string; replyToId?: string | null; clientMessageId?: string | null },
  ): Promise<MessageDto> {
    return api
      .post<MessageDto>(`/conversations/${conversationId}/messages`, payload)
      .then((response) => response.data);
  },

  markRead(conversationId: string, lastReadMessageId: string): Promise<void> {
    return api
      .patch<void>(`/conversations/${conversationId}/read`, { lastReadMessageId })
      .then(() => undefined);
  },
};
