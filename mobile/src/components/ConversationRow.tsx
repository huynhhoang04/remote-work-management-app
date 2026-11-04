import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Avatar, Badge, Icon, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { radius, spacing } from '../theme/tokens';
import type { ConversationDto } from '../types/api';
import { colorFromString, formatChatTime, initials } from '../utils/format';

interface Props {
  conversation: ConversationDto;
  onPress: (conversation: ConversationDto) => void;
}

/** Một dòng hội thoại trong tab Chat. */
export function ConversationRow({ conversation, onPress }: Props) {
  const theme = useTheme();
  const isGroup = conversation.type === 'GROUP';
  const color = colorFromString(conversation.name);
  const last = conversation.lastMessage;

  return (
    <TouchableRipple onPress={() => onPress(conversation)}>
      <View style={[styles.row, { borderBottomColor: theme.colors.outline }]}>
      {isGroup ? (
        <View style={[styles.groupAvatar, { backgroundColor: `${color}1F` }]}>
          <Icon source="account-group-outline" size={24} color={color} />
        </View>
      ) : (
        <Avatar.Text
          size={48}
          label={initials(conversation.name)}
          style={{ backgroundColor: `${color}1F` }}
          labelStyle={{ color, fontWeight: '700' }}
        />
      )}

      <View style={styles.body}>
        <View style={styles.titleRow}>
          <Text variant="titleSmall" numberOfLines={1} style={styles.title}>
            {conversation.name}
          </Text>
          <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {formatChatTime(last?.createdAt ?? conversation.createdAt)}
          </Text>
        </View>
        <View style={styles.previewRow}>
          <Text
            variant="bodySmall"
            numberOfLines={1}
            style={[
              styles.preview,
              {
                color: conversation.unreadCount > 0 ? theme.colors.onSurface : theme.colors.onSurfaceVariant,
                fontWeight: conversation.unreadCount > 0 ? '600' : '400',
              },
            ]}
          >
            {last ? (last.senderName ? `${last.senderName}: ${last.content}` : last.content) : 'Chưa có tin nhắn'}
          </Text>
          {conversation.unreadCount > 0 ? (
            <Badge style={{ backgroundColor: theme.colors.primary }}>{conversation.unreadCount}</Badge>
          ) : null}
        </View>
        {isGroup && conversation.projectId ? (
          <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant, marginTop: 2 }}>
            Kênh dự án
          </Text>
        ) : null}
      </View>
      </View>
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  groupAvatar: { width: 48, height: 48, borderRadius: radius.lg, alignItems: 'center', justifyContent: 'center' },
  body: { flex: 1, gap: 2 },
  titleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.sm },
  title: { flex: 1, fontWeight: '600' },
  previewRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  preview: { flex: 1 },
});
