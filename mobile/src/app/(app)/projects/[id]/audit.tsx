import { Stack, useLocalSearchParams } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, ScrollView, StyleSheet, View } from 'react-native';
import { Card, Chip, Menu, Text, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { useAuditLogs } from '@/features/projects/hooks';
import { radius, spacing } from '@/theme/tokens';
import { formatDateTime } from '@/utils/format';
import { STR } from '@/utils/constants';
import { auditActorName, formatAuditMetadata } from '@/utils/audit';

/**
 * Nhật ký hoạt động dự án (chỉ Owner xem được).
 * @reference docs/plan-app/02-phase-2.md §5.3
 */
export default function ProjectAuditScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const [action, setAction] = useState<string>('');
  const [menuVisible, setMenuVisible] = useState(false);

  const logs = useAuditLogs(id, action);
  const items = useMemo(() => logs.data?.pages.flatMap((page) => page.content) ?? [], [logs.data]);
  const actions = Object.keys(STR.audit);

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.projectsExtra.audit }} />

      <View style={styles.filters}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipRow}>
          <Chip selected={!action} onPress={() => setAction('')} style={styles.chip}>
            {STR.common.all}
          </Chip>
          <Menu
            visible={menuVisible}
            onDismiss={() => setMenuVisible(false)}
            anchor={
              <Chip icon="chevron-down" onPress={() => setMenuVisible(true)} style={styles.chip}>
                {action ? (STR.audit as Record<string, string>)[action] ?? action : STR.projectsExtra.auditFilter}
              </Chip>
            }
          >
            {actions.map((item) => (
              <Menu.Item
                key={item}
                title={(STR.audit as Record<string, string>)[item] ?? item}
                onPress={() => {
                  setAction(item);
                  setMenuVisible(false);
                }}
              />
            ))}
          </Menu>
        </ScrollView>
      </View>

      {logs.isPending ? (
        <SkeletonList count={5} />
      ) : logs.isError ? (
        <StateView status="error" errorText={normalizeError(logs.error).message} onRetry={() => logs.refetch()} />
      ) : items.length === 0 ? (
        <StateView
          status="empty"
          icon="history"
          emptyText={STR.projectsExtra.auditEmpty}
          emptyHint={STR.projectsExtra.auditEmptyHint}
        />
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          onEndReachedThreshold={0.4}
          onEndReached={() => {
            if (logs.hasNextPage && !logs.isFetchingNextPage) void logs.fetchNextPage();
          }}
          ListFooterComponent={logs.isFetchingNextPage ? <ActivityIndicator style={styles.footer} /> : undefined}
          renderItem={({ item }) => {
            const details = formatAuditMetadata(item.metadata);
            return (
              <Card mode="elevated" style={styles.card}>
                <Card.Content style={styles.row}>
                  <View style={styles.actionBadge}>
                    <Text variant="labelSmall" style={{ color: theme.colors.primary, fontWeight: '700' }}>
                      {(STR.audit as Record<string, string>)[item.action] ?? item.action}
                    </Text>
                  </View>
                  <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    {STR.auditDetails.entities[item.entityType] ?? STR.auditDetails.entity}
                  </Text>
                  {details ? (
                    <Text variant="bodySmall" style={{ color: theme.colors.onSurface }}>
                      {details}
                    </Text>
                  ) : null}
                  <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    {formatDateTime(item.createdAt)} · {auditActorName(item)}
                  </Text>
                </Card.Content>
              </Card>
            );
          }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  filters: { paddingHorizontal: spacing.lg, paddingVertical: spacing.sm },
  chipRow: { gap: spacing.xs, paddingRight: spacing.lg },
  chip: { borderRadius: 999 },
  list: { paddingHorizontal: spacing.lg, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.md, marginBottom: spacing.sm },
  row: { gap: 3 },
  actionBadge: { alignSelf: 'flex-start' },
  footer: { paddingVertical: spacing.lg },
});
