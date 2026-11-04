import { render } from '@testing-library/react-native';
import React from 'react';
import { PaperProvider } from 'react-native-paper';
import { LabelChip, TaskPriorityChip, TaskStatusChip } from '../TaskChips';

function renderWithTheme(ui: React.ReactElement) {
  return render(<PaperProvider>{ui}</PaperProvider>);
}

describe('TaskChips', () => {
  test('hiển thị nhãn tiếng Việt cho trạng thái', async () => {
    const { getByText } = await renderWithTheme(<TaskStatusChip status="DOING" />);
    expect(getByText('Đang làm')).toBeTruthy();
  });

  test('hiển thị nhãn tiếng Việt cho độ ưu tiên', async () => {
    const { getByText } = await renderWithTheme(<TaskPriorityChip priority="URGENT" />);
    expect(getByText('Khẩn cấp')).toBeTruthy();
  });

  test('hiển thị tên nhãn dự án', async () => {
    const { getByText } = await renderWithTheme(
      <LabelChip label={{ id: 'l1', name: 'Backend', color: '#2C63E8' }} />,
    );
    expect(getByText('Backend')).toBeTruthy();
  });
});
