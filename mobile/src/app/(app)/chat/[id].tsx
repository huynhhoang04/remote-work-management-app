import { Stack, useLocalSearchParams } from 'expo-router';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  View,
} from 'react-native';
import { useQueryClient } from '@tanstack/react-query';
import { Icon, IconButton, Text, TextInput, useTheme } from 'react-native-paper';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { chatApi } from '@/api/chat';
import { normalizeError } from '@/api/errors';
import {
  parseStompBody,
  realtime,
  sendChatMessageRealtime,
  sendReadReceiptRealtime,
} from '@/api/ws';
import { MessageBubble } from '@/components/MessageBubble';
import { useSnackbar } from '@/components/SnackbarProvider';
import { StateView } from '@/components/StateView';
import {
  removeMessageFromCache,
  upsertMessageToCache,
  useConversations,
  useMarkConversationRead,
  useMessages,
} from '@/features/chat/hooks';
import { useProject } from '@/features/projects/hooks';
import { useSession } from '@/store/session';
import { radius, spacing } from '@/theme/tokens';
import type { ConversationDto, MessageDto, ReadReceiptDto } from '@/types/api';
import { STR } from '@/utils/constants';
import { createClientMessageId } from '@/utils/id';

/**
 * Phòng chat: lịch sử phân trang, gửi realtime (STOMP) + fallback REST,
 * read receipt, trả lời tin nhắn.
 * @reference docs/plan-app/02-phase-2.md §5.8
 */
export default function ChatRoomScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const { show } = useSnackbar();
  const { user } = useSession();
  const queryClient = useQueryClient();
  const listRef = useRef<FlatList<MessageDto>>(null);

  const conversations = useConversations();
  const conversation = conversations.data?.find((item) => item.id === id);
  const project = useProject(conversation?.projectId ?? '');
  const messagesQuery = useMessages(id);
  const markRead = useMarkConversationRead();
  const markReadMutate = markRead.mutate;
  const refetchMessages = messagesQuery.refetch;

  const [text, setText] = useState('');
  const [replyTo, setReplyTo] = useState<MessageDto | null>(null);
  const [sending, setSending] = useState(false);
  const [connected, setConnected] = useState(realtime.isConnected);
  const [otherReadId, setOtherReadId] = useState<string | null>(null);

  const readOnly = conversation?.type === 'GROUP' && project.data?.role === 'VIEWER';

  /** Tin nhắn theo thứ tự thời gian tăng dần (cũ → mới). */
  const chronological = useMemo(() => {
    const pages = messagesQuery.data?.pages ?? [];
    return [...pages].reverse().flat();
  }, [messagesQuery.data]);

  /** FlatList inverted: index 0 = tin mới nhất. */
  const displayMessages = useMemo(() => [...chronological].reverse(), [chronological]);

  const messageById = useMemo(() => {
    const map = new Map<string, MessageDto>();
    chronological.forEach((message) => map.set(message.id, message));
    return map;
  }, [chronological]);

  const readSet = useMemo(() => {
    if (!otherReadId) return new Set<string>();
    const index = chronological.findIndex((message) => message.id === otherReadId);
    if (index < 0) return new Set<string>();
    return new Set(chronological.slice(0, index + 1).map((message) => message.id));
  }, [chronological, otherReadId]);

  const updateConversation = useCallback(
    (updater: (conversation: ConversationDto) => ConversationDto) => {
      queryClient.setQueryData<ConversationDto[]>(['conversations'], (current) =>
        current?.map((item) => (item.id === id ? updater(item) : item)),
      );
    },
    [id, queryClient],
  );

  const markReadUpTo = useCallback(
    (messageId: string) => {
      updateConversation((item) => ({ ...item, unreadCount: 0, lastReadMessageId: messageId }));
      if (!sendReadReceiptRealtime(id, messageId)) {
        markReadMutate({ conversationId: id, lastReadMessageId: messageId });
      }
    },
    [id, markReadMutate, updateConversation],
  );

  useEffect(() => {
    realtime.start();
    const unsubscribeTopic = realtime.subscribe(`/topic/conversations.${id}`, (frame) => {
      const receipt = parseStompBody<ReadReceiptDto>(frame);
      if (receipt && 'lastReadMessageId' in receipt && !('content' in receipt)) {
        if (receipt.userId !== user?.id) setOtherReadId(receipt.lastReadMessageId);
        return;
      }

      const message = parseStompBody<MessageDto>(frame);
      if (!message) return;
      upsertMessageToCache(queryClient, message, id);
      updateConversation((item) => ({
        ...item,
        lastMessage: message,
        unreadCount: message.senderId === user?.id ? 0 : item.unreadCount + 1,
      }));
      if (message.senderId !== user?.id) markReadUpTo(message.id);
    });

    const unsubscribeErrors = realtime.subscribe('/user/queue/errors', (frame) => {
      const error = parseStompBody<{ message?: string }>(frame);
      if (error?.message) show(error.message);
      void refetchMessages();
    });

    const unsubscribeConnection = realtime.onConnectionChange(setConnected);

    return () => {
      unsubscribeTopic();
      unsubscribeErrors();
      unsubscribeConnection();
    };
  }, [id, markReadUpTo, queryClient, refetchMessages, show, updateConversation, user?.id]);

  const newestId = displayMessages[0]?.id;
  useEffect(() => {
    // Tin optimistic có id tạm — chỉ gửi read receipt với tin đã có id từ server.
    if (newestId && !newestId.startsWith('local-')) markReadUpTo(newestId);
  }, [newestId, markReadUpTo]);

  const onSend = async () => {
    const content = text.trim();
    if (!content || sending || readOnly || !user) return;

    const clientMessageId = createClientMessageId();
    const optimistic: MessageDto = {
      id: `local-${clientMessageId}`,
      conversationId: id,
      senderId: user.id,
      senderName: user.fullName,
      content,
      replyToId: replyTo?.id ?? null,
      clientMessageId,
      createdAt: new Date().toISOString(),
    };

    upsertMessageToCache(queryClient, optimistic, id);
    updateConversation((item) => ({ ...item, lastMessage: optimistic, unreadCount: 0 }));
    setText('');
    setReplyTo(null);
    setSending(true);
    requestAnimationFrame(() => listRef.current?.scrollToOffset({ offset: 0, animated: true }));

    const published = sendChatMessageRealtime(id, {
      content,
      replyToId: replyTo?.id ?? null,
      clientMessageId,
    });

    if (published) {
      setSending(false);
      return;
    }

    try {
      const saved = await chatApi.send(id, {
        content,
        replyToId: replyTo?.id ?? null,
        clientMessageId,
      });
      upsertMessageToCache(queryClient, saved, id);
      updateConversation((item) => ({ ...item, lastMessage: saved }));
    } catch (error) {
      removeMessageFromCache(queryClient, id, optimistic.id);
      setText(content);
      setReplyTo(replyTo);
      show(normalizeError(error).message);
    } finally {
      setSending(false);
    }
  };

  const onLongPress = (message: MessageDto) => {
    Alert.alert(message.senderName, undefined, [
      { text: STR.chat.reply, onPress: () => setReplyTo(message) },
      { text: STR.cancel, style: 'cancel' },
    ]);
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: conversation?.name ?? STR.chat.title }} />
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 92 : 0}
        style={styles.flex}
      >
        {!connected ? (
          <View style={[styles.banner, { backgroundColor: `${theme.colors.error}12` }]}>
            <Icon source="wifi-off" size={14} color={theme.colors.error} />
            <Text variant="labelSmall" style={{ color: theme.colors.error }}>
              {STR.chat.offline}
            </Text>
          </View>
        ) : null}

        {messagesQuery.isPending ? (
          <View style={styles.center}>
            <ActivityIndicator />
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant, marginTop: spacing.sm }}>
              {connected ? STR.loading : STR.chat.connecting}
            </Text>
          </View>
        ) : messagesQuery.isError ? (
          <StateView
            status="error"
            errorText={normalizeError(messagesQuery.error).message}
            onRetry={() => messagesQuery.refetch()}
          />
        ) : (
          <FlatList
            ref={listRef}
            data={displayMessages}
            keyExtractor={(item) => item.id}
            inverted
            contentContainerStyle={styles.list}
            keyboardShouldPersistTaps="handled"
            onEndReachedThreshold={0.3}
            onEndReached={() => {
              if (messagesQuery.hasNextPage && !messagesQuery.isFetchingNextPage) {
                void messagesQuery.fetchNextPage();
              }
            }}
            ListFooterComponent={
              messagesQuery.isFetchingNextPage ? <ActivityIndicator style={styles.loadingOlder} /> : undefined
            }
            ListEmptyComponent={
              <View style={styles.empty}>
                <Icon source="message-outline" size={42} color={theme.colors.onSurfaceVariant} />
                <Text variant="titleSmall" style={{ marginTop: spacing.sm }}>
                  {STR.chat.noMessages}
                </Text>
                <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.chat.noMessagesHint}
                </Text>
              </View>
            }
            renderItem={({ item }) => (
              <MessageBubble
                message={item}
                isMine={item.senderId === user?.id}
                showSender={conversation?.type === 'GROUP'}
                isRead={readSet.has(item.id)}
                replyTo={item.replyToId ? messageById.get(item.replyToId) ?? null : null}
                onLongPress={onLongPress}
              />
            )}
          />
        )}

        {replyTo ? (
          <View style={[styles.replyBar, { borderTopColor: theme.colors.outline, backgroundColor: theme.colors.surface }]}>
            <Icon source="reply" size={18} color={theme.colors.primary} />
            <View style={styles.flex}>
              <Text variant="labelSmall" style={{ color: theme.colors.primary, fontWeight: '600' }}>
                {STR.chat.replyingTo} {replyTo.senderName}
              </Text>
              <Text variant="bodySmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant }}>
                {replyTo.content}
              </Text>
            </View>
            <IconButton icon="close" size={18} onPress={() => setReplyTo(null)} />
          </View>
        ) : null}

        <View
          style={[
            styles.inputBar,
            {
              borderTopColor: theme.colors.outline,
              backgroundColor: theme.colors.surface,
              paddingBottom: Math.max(insets.bottom, spacing.sm),
            },
          ]}
        >
          {readOnly ? (
            <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant, flex: 1 }}>
              {STR.roles.VIEWER}: {STR.common.permissionDenied}
            </Text>
          ) : (
            <>
              <TextInput
                mode="flat"
                placeholder={STR.chat.messagePlaceholder}
                value={text}
                onChangeText={setText}
                multiline
                maxLength={4000}
                dense
                style={styles.input}
                underlineColor="transparent"
                activeUnderlineColor="transparent"
              />
              <IconButton
                icon="send"
                mode="contained"
                onPress={onSend}
                disabled={!text.trim() || sending}
                iconColor={theme.colors.onPrimary}
                style={{ backgroundColor: text.trim() ? theme.colors.primary : theme.colors.surfaceVariant }}
              />
            </>
          )}
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  flex: { flex: 1 },
  banner: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    paddingVertical: spacing.xs,
  },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  list: { padding: spacing.lg, paddingBottom: spacing.sm },
  loadingOlder: { paddingVertical: spacing.md },
  empty: { alignItems: 'center', justifyContent: 'center', paddingVertical: spacing.xxl, transform: [{ scaleY: -1 }] },
  replyBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing.xs,
    paddingHorizontal: spacing.sm,
    paddingTop: spacing.xs,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  input: { flex: 1, backgroundColor: 'transparent', maxHeight: 120 },
});
