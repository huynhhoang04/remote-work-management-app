import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Text, TouchableRipple, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { MessageDto } from '../types/api';
import { formatRelative } from '../utils/format';

interface Props {
  message: MessageDto;
  isMine: boolean;
  showSender?: boolean;
  isRead?: boolean;
  replyTo?: MessageDto | null;
  onLongPress?: (message: MessageDto) => void;
}

/** Bubble tin nhắn trong phòng chat. */
export function MessageBubble({ message, isMine, showSender, isRead, replyTo, onLongPress }: Props) {
  const theme = useTheme();
  const background = isMine ? theme.colors.primary : theme.colors.surfaceVariant;
  const textColor = isMine ? theme.colors.onPrimary : theme.colors.onSurface;

  return (
    <View style={[styles.wrapper, isMine ? styles.mine : styles.theirs]}>
      {showSender && !isMine ? (
        <Text variant="labelSmall" style={[styles.sender, { color: theme.colors.primary }]}>
          {message.senderName}
        </Text>
      ) : null}

      <TouchableRipple
        onLongPress={() => onLongPress?.(message)}
        style={[styles.bubble, { backgroundColor: background }, isMine ? styles.bubbleMine : styles.bubbleTheirs]}
      >
        <View>
        {replyTo ? (
          <View style={[styles.reply, { borderLeftColor: isMine ? theme.colors.onPrimary : theme.colors.primary }]}>
            <Text
              variant="labelSmall"
              numberOfLines={1}
              style={{ color: isMine ? theme.colors.onPrimary : theme.colors.primary, fontWeight: '600' }}
            >
              {replyTo.senderName}
            </Text>
            <Text
              variant="bodySmall"
              numberOfLines={2}
              style={{ color: isMine ? theme.colors.onPrimary : theme.colors.onSurfaceVariant }}
            >
              {replyTo.content}
            </Text>
          </View>
        ) : null}

        <Text variant="bodyMedium" style={{ color: textColor }}>
          {message.content}
        </Text>

        <View style={styles.meta}>
          <Text variant="labelSmall" style={{ color: isMine ? `${theme.colors.onPrimary}CC` : theme.colors.onSurfaceVariant }}>
            {formatRelative(message.createdAt)}
          </Text>
          {isMine && isRead ? (
            <Text variant="labelSmall" style={{ color: `${theme.colors.onPrimary}CC` }}>
              · Đã đọc
            </Text>
          ) : null}
        </View>
        </View>
      </TouchableRipple>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { marginBottom: spacing.sm, maxWidth: '82%' },
  mine: { alignSelf: 'flex-end' },
  theirs: { alignSelf: 'flex-start' },
  sender: { fontWeight: '600', marginBottom: 2, marginLeft: spacing.xs },
  bubble: { borderRadius: radius.lg, paddingHorizontal: spacing.md, paddingVertical: spacing.sm },
  bubbleMine: { borderBottomRightRadius: radius.xs },
  bubbleTheirs: { borderBottomLeftRadius: radius.xs },
  reply: {
    borderLeftWidth: 3,
    paddingLeft: spacing.sm,
    marginBottom: spacing.xs,
    opacity: 0.9,
  },
  meta: { flexDirection: 'row', justifyContent: 'flex-end', marginTop: 3, gap: 3 },
});
