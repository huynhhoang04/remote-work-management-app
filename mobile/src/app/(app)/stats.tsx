import dayjs from 'dayjs';
import { Stack } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Card, ProgressBar, SegmentedButtons, Text, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { useMyStats } from '@/features/stats/hooks';
import { radius, spacing } from '@/theme/tokens';
import type { CountPointDto } from '@/types/api';
import { STR } from '@/utils/constants';

type Granularity = 'day' | 'week' | 'month';

const RANGE_OPTIONS = [
  { value: '7', label: STR.stats.range7 },
  { value: '30', label: STR.stats.range30 },
  { value: '90', label: STR.stats.range90 },
];

/**
 * Thống kê năng suất cá nhân: chuỗi hoàn thành + tỉ lệ đúng hạn.
 * @reference docs/plan-app/02-phase-2.md §5.6
 */
export default function MyStatsScreen() {
  const theme = useTheme();
  const [range, setRange] = useState('30');
  const [granularity, setGranularity] = useState<Granularity>('day');

  const days = Number(range);
  const { from, to } = useMemo(
    () => ({
      from: dayjs().subtract(days - 1, 'day').startOf('day').toISOString(),
      to: dayjs().endOf('day').toISOString(),
    }),
    [days],
  );

  const stats = useMyStats(from, to);
  const points: CountPointDto[] = stats.data
    ? granularity === 'day'
      ? stats.data.completedPerDay
      : granularity === 'week'
        ? stats.data.completedPerWeek
        : stats.data.completedPerMonth
    : [];

  const max = Math.max(1, ...points.map((point) => point.count));
  const total = points.reduce((sum, point) => sum + point.count, 0);
  const onTimePercent = Math.round((stats.data?.onTimeRate ?? 0) * 100);

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.stats.personalTitle }} />
      {stats.isPending ? (
        <SkeletonList count={3} />
      ) : stats.isError || !stats.data ? (
        <StateView status="error" errorText={normalizeError(stats.error).message} onRetry={() => stats.refetch()} />
      ) : (
        <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
          <SegmentedButtons value={range} onValueChange={setRange} buttons={RANGE_OPTIONS} />

          <View style={styles.summaryRow}>
            <Card mode="elevated" style={styles.summaryCard}>
              <Card.Content style={styles.summaryContent}>
                <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.stats.completed}
                </Text>
                <Text variant="displaySmall" style={styles.summaryValue}>
                  {total}
                </Text>
                <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.stats.inRange}
                </Text>
              </Card.Content>
            </Card>
            <Card mode="elevated" style={styles.summaryCard}>
              <Card.Content style={styles.summaryContent}>
                <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.stats.onTimeRate}
                </Text>
                <Text variant="displaySmall" style={[styles.summaryValue, { color: '#16A34A' }]}>
                  {onTimePercent}%
                </Text>
                <ProgressBar progress={onTimePercent / 100} color={'#16A34A'} style={styles.progress} />
              </Card.Content>
            </Card>
          </View>

          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <Text variant="titleSmall" style={styles.cardTitle}>
                {STR.stats.completed}
              </Text>
              <SegmentedButtons
                value={granularity}
                onValueChange={(value) => setGranularity(value as Granularity)}
                buttons={[
                  { value: 'day', label: STR.stats.perDay },
                  { value: 'week', label: STR.stats.perWeek },
                  { value: 'month', label: STR.stats.perMonth },
                ]}
                style={styles.segment}
              />

              {points.length === 0 || total === 0 ? (
                <View style={styles.emptyChart}>
                  <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                    {STR.stats.empty}
                  </Text>
                  <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    {STR.stats.emptyHint}
                  </Text>
                </View>
              ) : (
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chart}>
                  {points.map((point, index) => {
                    const height = Math.max(4, (point.count / max) * 120);
                    const showLabel = points.length <= 14 || index % Math.ceil(points.length / 10) === 0;
                    return (
                      <View key={`${point.period}-${index}`} style={styles.barItem}>
                        <Text variant="labelSmall" style={styles.barValue}>
                          {point.count}
                        </Text>
                        <View
                          style={[
                            styles.bar,
                            { height, backgroundColor: point.count > 0 ? theme.colors.primary : theme.colors.surfaceVariant },
                          ]}
                        />
                        <Text variant="labelSmall" style={[styles.barLabel, { color: theme.colors.onSurfaceVariant }]}>
                          {showLabel ? shortPeriod(point.period, granularity) : ' '}
                        </Text>
                      </View>
                    );
                  })}
                </ScrollView>
              )}
            </Card.Content>
          </Card>
        </ScrollView>
      )}
    </View>
  );
}

function shortPeriod(period: string, granularity: Granularity): string {
  if (granularity === 'day') {
    const date = dayjs(period);
    return date.isValid() ? date.format('DD/MM') : period;
  }
  if (granularity === 'month') return period;
  const week = period.split('-W');
  return week.length === 2 ? `W${week[1]}` : period;
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  summaryRow: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
  summaryCard: { flex: 1, borderRadius: radius.lg },
  summaryContent: { gap: 2 },
  summaryValue: { fontWeight: '700' },
  progress: { height: 6, borderRadius: 3, marginTop: spacing.xs },
  card: { borderRadius: radius.lg },
  cardTitle: { fontWeight: '700', marginBottom: spacing.sm },
  segment: { marginBottom: spacing.md },
  emptyChart: { alignItems: 'center', gap: spacing.xs, paddingVertical: spacing.xl },
  chart: { alignItems: 'flex-end', gap: spacing.xs, paddingVertical: spacing.sm, paddingRight: spacing.md },
  barItem: { alignItems: 'center', width: 24, gap: 2 },
  barValue: { color: '#94A3B8' },
  bar: { width: 14, borderRadius: 4 },
  barLabel: { fontSize: 9, height: 14 },
});
