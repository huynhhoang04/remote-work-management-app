import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Avatar, Text, useTheme } from 'react-native-paper';
import { spacing } from '../theme/tokens';
import type { TaskAssigneeDto } from '../types/api';
import { colorFromString, initials } from '../utils/format';

interface Props {
  assignees: TaskAssigneeDto[];
  size?: number;
  max?: number;
}

/** Chồng avatar người được giao task + phần dư "+N". */
export function AssigneeAvatars({ assignees, size = 24, max = 3 }: Props) {
  const theme = useTheme();
  if (assignees.length === 0) return null;

  const shown = assignees.slice(0, max);
  const remaining = assignees.length - shown.length;

  return (
    <View style={styles.row}>
      {shown.map((assignee, index) => {
        const color = colorFromString(assignee.userId);
        return (
          <Avatar.Text
            key={assignee.userId}
            size={size}
            label={initials(assignee.fullName)}
            style={[
              styles.avatar,
              { backgroundColor: `${color}26`, borderColor: theme.colors.surface, marginLeft: index === 0 ? 0 : -size * 0.3 },
            ]}
            labelStyle={{ color, fontSize: size * 0.4, fontWeight: '700' }}
          />
        );
      })}
      {remaining > 0 ? (
        <View
          style={[
            styles.more,
            {
              width: size,
              height: size,
              borderRadius: size / 2,
              backgroundColor: theme.colors.surfaceVariant,
              marginLeft: -size * 0.3,
              borderColor: theme.colors.surface,
            },
          ]}
        >
          <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant, fontSize: size * 0.38 }}>
            +{remaining}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  avatar: { borderWidth: 2 },
  more: { alignItems: 'center', justifyContent: 'center', borderWidth: 2 },
});
