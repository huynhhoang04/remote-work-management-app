import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Appbar, Badge, useTheme } from 'react-native-paper';

export interface HeaderAction {
  icon: string;
  onPress: () => void;
  accessibilityLabel?: string;
  badge?: number;
}

interface Props {
  title: string;
  subtitle?: string;
  actions?: HeaderAction[];
}

/** Header chuẩn cho các tab (thay header mặc định của navigator). */
export function TabHeader({ title, subtitle, actions }: Props) {
  const theme = useTheme();

  return (
    <Appbar.Header
      style={[styles.header, { backgroundColor: theme.colors.surface }]}
      statusBarHeight={undefined}
      elevated={false}
    >
      <Appbar.Content
        title={title}
        titleStyle={styles.title}
        subtitle={subtitle}
        subtitleStyle={{ color: theme.colors.onSurfaceVariant }}
      />
      {actions?.map((action) => (
        <View key={action.icon}>
          <Appbar.Action
            icon={action.icon}
            onPress={action.onPress}
            accessibilityLabel={action.accessibilityLabel}
            color={theme.colors.onSurface}
          />
          {action.badge && action.badge > 0 ? (
            <Badge size={16} style={[styles.badge, { backgroundColor: theme.colors.error }]}>
              {action.badge > 99 ? '99+' : action.badge}
            </Badge>
          ) : null}
        </View>
      ))}
    </Appbar.Header>
  );
}

const styles = StyleSheet.create({
  header: { elevation: 0, shadowOpacity: 0 },
  title: { fontWeight: '700', fontSize: 20 },
  badge: { position: 'absolute', top: 6, right: 4 },
});
