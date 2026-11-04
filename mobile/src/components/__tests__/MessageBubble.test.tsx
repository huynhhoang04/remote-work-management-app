import { render } from '@testing-library/react-native';
import React from 'react';
import { PaperProvider } from 'react-native-paper';
import type { MessageDto } from '../../types/api';
import { MessageBubble } from '../MessageBubble';

const message: MessageDto = {
  id: 'm2',
  conversationId: 'c1',
  senderId: 'u1',
  senderName: 'An',
  content: 'Mai họp lúc 9h nhé',
  replyToId: 'm1',
  clientMessageId: null,
  createdAt: new Date().toISOString(),
};

const replied: MessageDto = {
  ...message,
  id: 'm1',
  content: 'Chốt lịch họp',
  replyToId: null,
};

function renderWithTheme(ui: React.ReactElement) {
  return render(<PaperProvider>{ui}</PaperProvider>);
}

describe('MessageBubble', () => {
  test('hiển thị nội dung, người gửi, trả lời và trạng thái đã đọc', async () => {
    const { getByText } = await renderWithTheme(
      <MessageBubble message={message} isMine showSender isRead replyTo={replied} />,
    );

    expect(getByText('Mai họp lúc 9h nhé')).toBeTruthy();
    expect(getByText('An')).toBeTruthy();
    expect(getByText('Chốt lịch họp')).toBeTruthy();
    expect(getByText('· Đã đọc')).toBeTruthy();
  });

  test('không hiện người gửi khi là tin của mình', async () => {
    const { queryByText } = await renderWithTheme(
      <MessageBubble message={message} isMine />,
    );

    expect(queryByText('An')).toBeNull();
  });
});
