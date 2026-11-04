import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { PaperProvider } from 'react-native-paper';
import { StateView } from '../StateView';

function renderWithTheme(ui: React.ReactElement) {
  return render(<PaperProvider>{ui}</PaperProvider>);
}

describe('StateView', () => {
  test('empty hiển thị text rỗng', async () => {
    const { getByText } = await renderWithTheme(
      <StateView status="empty" emptyText="Chưa có dự án" emptyHint="Tạo dự án đầu tiên" />,
    );

    expect(getByText('Chưa có dự án')).toBeTruthy();
    expect(getByText('Tạo dự án đầu tiên')).toBeTruthy();
  });

  test('error hiển thị lỗi và gọi onRetry', async () => {
    const onRetry = jest.fn();
    const { getByText } = await renderWithTheme(
      <StateView status="error" errorText="Không kết nối được máy chủ" onRetry={onRetry} />,
    );

    expect(getByText('Không kết nối được máy chủ')).toBeTruthy();

    fireEvent.press(getByText('Thử lại'));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  test('content render children', async () => {
    const { toJSON } = await renderWithTheme(
      <StateView status="content">
        <></>
      </StateView>,
    );

    expect(toJSON()).toBeTruthy();
  });
});
