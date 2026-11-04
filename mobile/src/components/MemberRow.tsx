import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Avatar, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { spacing } from '../theme/tokens';
import type { MemberDto } from '../types/api';
import { colorFromString, initials } from '../utils/format';
import { RoleChip } from './RoleChip';

interface Props {
  member: MemberDto;
  onPress?: () => void;
  right?: React.ReactNode;
}

/**
 * Một dòng thành viên dự án.
 * @reference docs/plan-app/01-phase-1.md §4.2
 */
export function MemberRow({ member, onPress, right }: Props) {
  const theme = useTheme();
  const avatarColor = colorFromString(member.userId);

  return (
    <TouchableRipple onPress={onPress} disabled={!onPress}>
      <View style={styles.row}>
        <Avatar.Text
          size={40}
          label={initials(member.fullName)}
          style={{ backgroundColor: `${avatarColor}1F` }}
          labelStyle={[styles.avatarLabel, { color: avatarColor }]}
        />
        <View style={styles.info}>
          <Text variant="titleSmall" numberOfLines={1}>
            {member.fullName}
          </Text>
          <Text variant="bodySmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant }}>
            {member.email}
          </Text>
        </View>
        {right ?? <RoleChip role={member.role} />}
      </View>
    </TouchableRipple>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.lg,
    gap: spacing.md,
  },
  info: { flex: 1 },
  avatarLabel: { fontSize: 14, fontWeight: '700' },
});
