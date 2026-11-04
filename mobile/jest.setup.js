/* eslint-env jest */
// Mock các module native mà Jest (môi trường node) không chạy được.

jest.mock('expo-secure-store', () => {
  const store = new Map();
  return {
    getItemAsync: jest.fn(async (key) => (store.has(key) ? store.get(key) : null)),
    setItemAsync: jest.fn(async (key, value) => {
      store.set(key, value);
    }),
    deleteItemAsync: jest.fn(async (key) => {
      store.delete(key);
    }),
  };
});

jest.mock('expo-clipboard', () => ({
  setStringAsync: jest.fn(async () => true),
}));

// Paper dùng @expo/vector-icons — trong Jest không load được font asset, mock icon thành Text rỗng.
jest.mock('@expo/vector-icons/MaterialCommunityIcons', () => {
  const React = require('react');
  const { Text } = require('react-native');
  return {
    __esModule: true,
    default: (props) => React.createElement(Text, { ...props, children: '' }),
  };
});
