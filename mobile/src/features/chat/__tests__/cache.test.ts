import { QueryClient } from '@tanstack/react-query';
import type { MessageDto } from '../../../types/api';
import { removeMessageFromCache, upsertMessageToCache } from '../hooks';

interface MessagesData {
  pages: MessageDto[][];
  pageParams: unknown[];
}

const clients: QueryClient[] = [];

function makeClient(): QueryClient {
  const client = new QueryClient();
  clients.push(client);
  return client;
}

afterEach(() => {
  clients.forEach((client) => client.clear());
  clients.length = 0;
});

function message(overrides: Partial<MessageDto>): MessageDto {
  return {
    id: 'm1',
    conversationId: 'c1',
    senderId: 'u1',
    senderName: 'Người gửi',
    content: 'Xin chào',
    replyToId: null,
    clientMessageId: null,
    createdAt: '2026-03-10T09:00:00.000Z',
    ...overrides,
  };
}

function cache(client: QueryClient): MessagesData | undefined {
  return client.getQueryData<MessagesData>(['messages', 'c1']);
}

describe('chat cache helpers', () => {
  test('chèn tin mới vào đầu trang mới nhất', () => {
    const client = makeClient();
    client.setQueryData<MessagesData>(['messages', 'c1'], {
      pages: [[message({ id: 'old' })]],
      pageParams: [undefined],
    });

    upsertMessageToCache(client, message({ id: 'new' }), 'c1');

    expect(cache(client)?.pages[0].map((item) => item.id)).toEqual(['old', 'new']);
  });

  test('thay tin optimistic khi trùng clientMessageId', () => {
    const client = makeClient();
    client.setQueryData<MessagesData>(['messages', 'c1'], {
      pages: [[message({ id: 'local-1', clientMessageId: 'abc' })]],
      pageParams: [undefined],
    });

    upsertMessageToCache(client, message({ id: 'server-1', clientMessageId: 'abc' }), 'c1');

    const page = cache(client)?.pages[0] ?? [];
    expect(page).toHaveLength(1);
    expect(page[0].id).toBe('server-1');
  });

  test('không nhân đôi khi nhận lại cùng id từ WS', () => {
    const client = makeClient();
    client.setQueryData<MessagesData>(['messages', 'c1'], {
      pages: [[message({ id: 'm1' })]],
      pageParams: [undefined],
    });

    upsertMessageToCache(client, message({ id: 'm1' }), 'c1');

    expect(cache(client)?.pages[0]).toHaveLength(1);
  });

  test('xoá tin optimistic khi gửi lỗi', () => {
    const client = makeClient();
    client.setQueryData<MessagesData>(['messages', 'c1'], {
      pages: [[message({ id: 'local-1' }), message({ id: 'm2' })]],
      pageParams: [undefined],
    });

    removeMessageFromCache(client, 'c1', 'local-1');

    expect(cache(client)?.pages[0].map((item) => item.id)).toEqual(['m2']);
  });
});
