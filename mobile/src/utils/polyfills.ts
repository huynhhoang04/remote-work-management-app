/**
 * Polyfill TextEncoder/TextDecoder cho Hermes — @stomp/stompjs cần để mã hoá frame STOMP.
 * Chỉ hỗ trợ UTF-8 (đủ cho giao thức STOMP dạng text).
 * @reference docs/plan-app/02-phase-2.md §3
 */
export function encodeUtf8(value: string): Uint8Array {
  const bytes: number[] = [];

  for (const char of value) {
    const code = char.codePointAt(0) ?? 0;
    if (code < 0x80) {
      bytes.push(code);
    } else if (code < 0x800) {
      bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f));
    } else if (code < 0x10000) {
      bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f));
    } else {
      bytes.push(
        0xf0 | (code >> 18),
        0x80 | ((code >> 12) & 0x3f),
        0x80 | ((code >> 6) & 0x3f),
        0x80 | (code & 0x3f),
      );
    }
  }

  return new Uint8Array(bytes);
}

export function decodeUtf8(bytes: Uint8Array): string {
  let result = '';
  let index = 0;

  while (index < bytes.length) {
    const byte = bytes[index];
    if (byte < 0x80) {
      result += String.fromCharCode(byte);
      index += 1;
    } else if ((byte & 0xe0) === 0xc0) {
      result += String.fromCharCode(((byte & 0x1f) << 6) | (bytes[index + 1] & 0x3f));
      index += 2;
    } else if ((byte & 0xf0) === 0xe0) {
      result += String.fromCharCode(
        ((byte & 0x0f) << 12) | ((bytes[index + 1] & 0x3f) << 6) | (bytes[index + 2] & 0x3f),
      );
      index += 3;
    } else {
      const codePoint =
        ((byte & 0x07) << 18) |
        ((bytes[index + 1] & 0x3f) << 12) |
        ((bytes[index + 2] & 0x3f) << 6) |
        (bytes[index + 3] & 0x3f);
      result += String.fromCodePoint(codePoint);
      index += 4;
    }
  }

  return result;
}

class Utf8TextEncoder {
  readonly encoding = 'utf-8';

  encode(value?: string): Uint8Array {
    return encodeUtf8(value ?? '');
  }
}

class Utf8TextDecoder {
  readonly encoding = 'utf-8';

  decode(input?: ArrayBuffer | ArrayBufferView): string {
    if (!input) return '';
    if (input instanceof ArrayBuffer) return decodeUtf8(new Uint8Array(input));
    return decodeUtf8(new Uint8Array(input.buffer, input.byteOffset, input.byteLength));
  }
}

const globalScope = globalThis as unknown as {
  TextEncoder?: unknown;
  TextDecoder?: unknown;
};

if (!globalScope.TextEncoder) globalScope.TextEncoder = Utf8TextEncoder;
if (!globalScope.TextDecoder) globalScope.TextDecoder = Utf8TextDecoder;
