import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { API_BASE_URL } from './client';
import { getAccessToken } from './tokenStore';

type MessageHandler = (message: IMessage) => void;
type ConnectionListener = (connected: boolean) => void;

function websocketUrl(): string | null {
  const token = getAccessToken();
  if (!token) return null;
  const base = API_BASE_URL.replace(/^http/i, 'ws').replace(/\/+$/, '');
  return `${base}/ws?token=${encodeURIComponent(token)}`;
}

/**
 * STOMP client dùng chung: chat realtime + thông báo.
 * Tự động kết nối lại và đăng ký lại subscription sau mỗi lần reconnect.
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 */
class RealtimeClient {
  private client: Client | null = null;
  private connected = false;
  private handlers = new Map<string, Set<MessageHandler>>();
  private subscriptions = new Map<string, StompSubscription>();
  private connectionListeners = new Set<ConnectionListener>();

  get isConnected(): boolean {
    return this.connected;
  }

  /** Bật kết nối (idempotent). Không có token → bỏ qua. */
  start(): void {
    if (this.client) return;
    const url = websocketUrl();
    if (!url) return;

    const client = new Client({
      webSocketFactory: () => new WebSocket(url) as unknown as WebSocket,
      reconnectDelay: 5000,
      connectionTimeout: 10000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      forceBinaryWSFrames: false,
      appendMissingNULLonIncoming: true,
    });

    client.onConnect = () => {
      this.connected = true;
      this.handlers.forEach((_, destination) => this.ensureSubscription(destination));
      this.emitConnection(true);
    };

    client.onDisconnect = () => {
      this.connected = false;
      this.subscriptions.clear();
      this.emitConnection(false);
    };

    client.onWebSocketClose = () => {
      this.connected = false;
      this.subscriptions.clear();
      this.emitConnection(false);
    };

    this.client = client;
    client.activate();
  }

  /** Ngắt kết nối khi logout/hết phiên. */
  async stop(): Promise<void> {
    const client = this.client;
    this.client = null;
    this.connected = false;
    this.subscriptions.clear();
    this.emitConnection(false);
    if (client) {
      try {
        await client.deactivate();
      } catch {
        // Bỏ qua lỗi khi đóng socket.
      }
    }
  }

  /** Đăng ký một destination; trả về hàm huỷ đăng ký. */
  subscribe(destination: string, handler: MessageHandler): () => void {
    let set = this.handlers.get(destination);
    if (!set) {
      set = new Set();
      this.handlers.set(destination, set);
    }
    set.add(handler);
    if (this.connected) this.ensureSubscription(destination);

    return () => {
      const current = this.handlers.get(destination);
      if (!current) return;
      current.delete(handler);
      if (current.size === 0) {
        this.handlers.delete(destination);
        this.subscriptions.get(destination)?.unsubscribe();
        this.subscriptions.delete(destination);
      }
    };
  }

  /** Gửi frame STOMP; trả false nếu chưa kết nối (caller dùng REST fallback). */
  publish(destination: string, body: unknown): boolean {
    if (!this.client || !this.connected) return false;
    try {
      this.client.publish({ destination, body: JSON.stringify(body) });
      return true;
    } catch {
      return false;
    }
  }

  onConnectionChange(listener: ConnectionListener): () => void {
    this.connectionListeners.add(listener);
    listener(this.connected);
    return () => {
      this.connectionListeners.delete(listener);
    };
  }

  private ensureSubscription(destination: string): void {
    if (!this.client || !this.connected || this.subscriptions.has(destination)) return;
    try {
      this.subscriptions.set(
        destination,
        this.client.subscribe(destination, (message) => {
          this.handlers.get(destination)?.forEach((handler) => handler(message));
        }),
      );
    } catch {
      // Kết nối vừa đóng — sẽ đăng ký lại ở lần reconnect sau.
    }
  }

  private emitConnection(connected: boolean): void {
    this.connectionListeners.forEach((listener) => listener(connected));
  }
}

export const realtime = new RealtimeClient();

export function parseStompBody<T>(message: IMessage): T | null {
  try {
    return JSON.parse(message.body) as T;
  } catch {
    return null;
  }
}

/** Gửi tin nhắn qua STOMP; false → gọi REST fallback. */
export function sendChatMessageRealtime(
  conversationId: string,
  payload: { content: string; replyToId?: string | null; clientMessageId?: string | null },
): boolean {
  return realtime.publish('/app/chat.send', { conversationId, ...payload });
}

/** Gửi read receipt qua STOMP (REST fallback do caller xử lý). */
export function sendReadReceiptRealtime(conversationId: string, lastReadMessageId: string): boolean {
  return realtime.publish('/app/conversations.read', { conversationId, lastReadMessageId });
}
