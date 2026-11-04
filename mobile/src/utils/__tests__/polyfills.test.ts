import { decodeUtf8, encodeUtf8 } from '../polyfills';

describe('UTF-8 polyfill', () => {
  test('mã hoá ASCII', () => {
    expect(Array.from(encodeUtf8('abc'))).toEqual([97, 98, 99]);
  });

  test('round-trip tiếng Việt có dấu', () => {
    const text = 'Xin chào, đây là tin nhắn tiếng Việt 🚀';
    expect(decodeUtf8(encodeUtf8(text))).toBe(text);
  });

  test('ký tự nhiều byte (emoji)', () => {
    const text = '😀🎉';
    const bytes = encodeUtf8(text);
    expect(bytes.length).toBe(8);
    expect(decodeUtf8(bytes)).toBe(text);
  });

  test('chuỗi rỗng', () => {
    expect(decodeUtf8(encodeUtf8(''))).toBe('');
  });
});
