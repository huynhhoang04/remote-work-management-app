import { Stack, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Avatar, Card, Icon, ProgressBar, Text, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { useProjectStats } from '@/features/stats/hooks';
import { radius, spacing, statusColors } from '@/theme/tokens';
import type { TaskStatus } from '@/types/api';
import { colorFromString, initials } from '@/utils/format';
import { STR } from '@/utils/constants';

const STATUSES: TaskStatus[] = ['TODO', 'DOING', 'REVIEW', 'DONE'];

/**
 * Dashboard tiến độ dự án.
 * @reference docs/plan-app/02-phase-2.md §5.6
 */
export default function ProjectStatsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const stats = useProjectStats(id);

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.stats.projectTitle }} />
      {stats.isPending ? (
        <SkeletonList count={4} />
      ) : stats.isError || !stats.data ? (
        <StateView status="error" errorText={normalizeError(stats.error).message} onRetry={() => stats.refetch()} />
      ) : (
        <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.progressCard}>
              <Text variant="labelLarge" style={{ color: theme.colors.onSurfaceVariant }}>
                {STR.stats.percentDone}
              </Text>
              <Text variant="displaySmall" style={styles.percent}>
                {Math.round(stats.data.percentDone)}%
              </Text>
              <ProgressBar
                progress={Math.min(1, Math.max(0, stats.data.percentDone / 100))}
                color={theme.colors.primary}
                style={styles.progress}
              />
              <View style={styles.summaryRow}>
                <Summary icon="clipboard-text-outline" label={STR.stats.totalTasks} value={stats.data.totalTasks} />
                <Summary icon="alert-circle-outline" label={STR.stats.overdue} value={stats.data.overdueCount} color={theme.colors.error} />
                <Summary icon="check-circle-outline" label={STR.tasks.statuses.DONE} value={stats.data.perStatus.DONE ?? 0} color="#16A34A" />
              </View>
            </Card.Content>
          </Card>

          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <Text variant="titleSmall" style={styles.cardTitle}>
                {STR.stats.byStatus}
              </Text>
              {STATUSES.map((status) => {
                const count = stats.data?.perStatus[status] ?? 0;
                const ratio = stats.data?.totalTasks ? count / stats.data.totalTasks : 0;
                return (
                  <View key={status} style={styles.statusRow}>
                    <Text variant="labelMedium" style={styles.statusLabel}>
                      {(STR.tasks.statuses as Record<string, string>)[status]}
                    </Text>
                    <ProgressBar
                      progress={ratio}
                      color={statusColors[status]}
                      style={styles.statusBar}
                    />
                    <Text variant="labelMedium" style={styles.statusCount}>
                      {count}
                    </Text>
                  </View>
                );
              })}
            </Card.Content>
          </Card>

          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <Text variant="titleSmall" style={styles.cardTitle}>
                {STR.stats.byMember}
              </Text>
              {stats.data.perMember.length === 0 ? (
                <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.common.noData}
                </Text>
              ) : (
                stats.data.perMember.map((member) => {
                  const color = colorFromString(member.userId);
                  const ratio = member.total ? member.done / member.total : 0;
                  return (
                    <View key={member.userId} style={styles.memberRow}>
                      <Avatar.Text
                        size={34}
                        label={initials(member.fullName)}
                        style={{ backgroundColor: `${color}1F` }}
                        labelStyle={{ color, fontWeight: '700', fontSize: 12 }}
                      />
                      <View style={styles.memberBody}>
                        <View style={styles.memberHeader}>
                          <Text variant="bodyMedium" numberOfLines={1} style={styles.memberName}>
                            {member.fullName}
                          </Text>
                          <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                            {member.done}/{member.total}
                          </Text>
                        </View>
                        <ProgressBar progress={ratio} color={color} style={styles.memberBar} />
                      </View>
                    </View>
                  );
                })
              )}
            </Card.Content>
          </Card>
        </ScrollView>
      )}
    </View>
  );
}

function Summary({ icon, label, value, color }: { icon: string; label: string; value: number; color?: string }) {
  const theme = useTheme();
  return (
    <View style={styles.summary}>
      <Icon source={icon} size={18} color={color ?? theme.colors.primary} />
      <Text variant="titleMedium" style={{ fontWeight: '700', color: color ?? theme.colors.onSurface }}>
        {value}
      </Text>
      <Text variant="labelSmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant }}>
        {label}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.lg },
  progressCard: { alignItems: 'center', gap: spacing.xs },
  percent: { fontWeight: '700' },
  progress: { height: 8, borderRadius: 4, width: '100%', marginTop: spacing.xs },
  summaryRow: { flexDirection: 'row', justifyContent: 'space-around', width: '100%', marginTop: spacing.md },
  summary: { alignItems: 'center', gap: 2, flex: 1 },
  cardTitle: { fontWeight: '700', marginBottom: spacing.md },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.sm },
  statusLabel: { width: 84 },
  statusBar: { flex: 1, height: 7, borderRadius: 4 },
  statusCount: { width: 28, textAlign: 'right' },
  memberRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.md },
  memberBody: { flex: 1, gap: 4 },
  memberHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: spacing.sm },
  memberName: { flex: 1 },
  memberBar: { height: 6, borderRadius: 3 },
});
