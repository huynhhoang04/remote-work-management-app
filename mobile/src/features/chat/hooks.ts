import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { ConversationDto, MessageDto } from '../../types/api';
import { chatApi } from '../../api/chat';

const MESSAGE_PAGE_SIZE = 30;

export function useConversations() {
  return useQuery({
    queryKey: ['conversations'],
    queryFn: chatApi.conversations,
    refetchOnWindowFocus: true,
  });
}

export function useCreateDirectConversation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => chatApi.createDirect(userId),
    onSuccess: async (conversation) => {
      await queryClient.invalidateQueries({ queryKey: ['conversations'] });
      queryClient.setQueryData(['conversation', conversation.id], conversation);
    },
  });
}

export function useMessages(conversationId: string) {
  return useInfiniteQuery({
    queryKey: ['messages', conversationId],
    queryFn: ({ pageParam }) =>
      chatApi.messages(conversationId, pageParam as string | undefined, MESSAGE_PAGE_SIZE),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage: MessageDto[]) => {
      if (lastPage.length < MESSAGE_PAGE_SIZE) return undefined;
      return lastPage[0]?.createdAt;
    },
    enabled: Boolean(conversationId),
  });
}

/** Gửi tin qua REST — fallback khi WebSocket chưa kết nối. */
export function useSendMessage(conversationId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { content: string; replyToId?: string | null; clientMessageId?: string | null }) =>
      chatApi.send(conversationId, payload),
    onSuccess: (message) => {
      appendMessageToCache(queryClient, message);
      void queryClient.invalidateQueries({ queryKey: ['conversations'] });
    },
  });
}

export function useMarkConversationRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { conversationId: string; lastReadMessageId: string }) =>
      chatApi.markRead(payload.conversationId, payload.lastReadMessageId),
    onSuccess: (_data, variables) => {
      queryClient.setQueryData<ConversationDto[]>(['conversations'], (current) =>
        current?.map((conversation) =>
          conversation.id === variables.conversationId
            ? { ...conversation, unreadCount: 0, lastReadMessageId: variables.lastReadMessageId }
            : conversation,
        ),
      );
    },
  });
}

type QueryClientLike = ReturnType<typeof useQueryClient>;

interface MessagesData {
  pages: MessageDto[][];
  pageParams: unknown[];
}

/**
 * Thêm/cập nhật tin trong cache. pages[0] là trang mới nhất nên tin mới chèn vào đầu.
 * Tin optimistic (id tạm) được thay bằng tin thật khi trùng clientMessageId.
 */
export function upsertMessageToCache(
  queryClient: QueryClientLike,
  message: MessageDto,
  conversationId = message.conversationId,
): void {
  queryClient.setQueryData<MessagesData>(['messages', conversationId], (current) => {
    if (!current) return current;

    const pages = current.pages.map((page) => [...page]);
    const flat = pages.flat();
    const existingIndex = flat.findIndex(
      (item) =>
        item.id === message.id ||
        (message.clientMessageId && item.clientMessageId === message.clientMessageId),
    );

    if (existingIndex >= 0) {
      for (const page of pages) {
        const index = page.findIndex(
          (item) =>
            item.id === message.id ||
            (message.clientMessageId && item.clientMessageId === message.clientMessageId),
        );
        if (index >= 0) page[index] = message;
      }
      return { ...current, pages };
    }

    pages[0] = [...(pages[0] ?? []), message];
    return { ...current, pages };
  });
}

export const appendMessageToCache = upsertMessageToCache;

/** Xoá tin optimistic khi gửi thất bại. */
export function removeMessageFromCache(
  queryClient: QueryClientLike,
  conversationId: string,
  messageId: string,
): void {
  queryClient.setQueryData<MessagesData>(['messages', conversationId], (current) => {
    if (!current) return current;
    return {
      ...current,
      pages: current.pages.map((page) => page.filter((item) => item.id !== messageId)),
    };
  });
}
