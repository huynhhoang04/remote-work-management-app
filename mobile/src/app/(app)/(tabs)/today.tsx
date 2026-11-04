import { router } from 'expo-router';
import React, { useCallback, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { Icon, Surface, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TabHeader } from '@/components/TabHeader';
import { TaskBriefCard } from '@/components/TaskBriefCard';
import { useProjects } from '@/features/projects/hooks';
import { useToday } from '@/features/stats/hooks';
import { useSession } from '@/store/session';
import { radius, spacing } from '@/theme/tokens';
import type { EventBriefDto, TaskBriefDto } from '@/types/api';
import { STR } from '@/utils/constants';
import { formatDayLabel, formatTime, isOverdue } from '@/utils/format';

/**
 * Tab "Hôm nay": task quá hạn / đến hạn / sắp tới + sự kiện trong ngày.
 * @reference docs/plan-app/02-phase-2.md §5.1
 */
export default function TodayScreen() {
  const theme = useTheme();
  const { user } = useSession();
  const today = useToday();
  const projects = useProjects();
  const [refreshing, setRefreshing] = useState(false);

  const projectNames = useMemo(() => {
    const map = new Map<string, string>();
    projects.data?.forEach((project) => map.set(project.id, project.name));
    return map;
  }, [projects.data]);

  const openTask = useCallback((task: TaskBriefDto) => {
    router.push(`/tasks/${task.id}`);
  }, []);

  const openEvent = useCallback((event: EventBriefDto) => {
    router.push(`/schedule/${event.id}`);
  }, []);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    try {
      await Promise.all([today.refetch(), projects.refetch()]);
    } finally {
      setRefreshing(false);
    }
  }, [today, projects]);

  const data = today.data;
  const totalTasks =
    (data?.overdueTasks.length ?? 0) + (data?.dueTodayTasks.length ?? 0) + (data?.upcomingTasks.length ?? 0);
  const isEmpty = totalTasks === 0 && (data?.todayEvents.length ?? 0) === 0;

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <TabHeader
        title={STR.today.title}
        subtitle={`${STR.today.greeting} ${user?.fullName ?? ''} · ${formatDayLabel(new Date())}`}
        actions={[
          { icon: 'calendar-month-outline', onPress: () => router.push('/schedule') },
          { icon: 'chart-box-outline', onPress: () => router.push('/stats') },
        ]}
      />

      {today.isPending ? (
        <View style={styles.skeleton}>
          <SkeletonList count={4} />
        </View>
      ) : today.isError ? (
        <StateView
          status="error"
          errorText={normalizeError(today.error).message}
          onRetry={() => today.refetch()}
        />
      ) : (
        <ScrollView
          contentContainerStyle={styles.container}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.primary} />
          }
        >
          <View style={styles.statsGrid}>
            <StatTile
              icon="alert-circle-outline"
              color={theme.colors.error}
              value={data?.overdueTasks.length ?? 0}
              label={STR.today.overdue}
            />
            <StatTile
              icon="calendar-today"
              color={theme.colors.primary}
              value={data?.dueTodayTasks.length ?? 0}
              label={STR.today.dueToday}
            />
            <StatTile
              icon="calendar-arrow-right"
              color="#F59E0B"
              value={data?.upcomingTasks.length ?? 0}
              label={STR.today.upcoming}
            />
            <StatTile
              icon="clock-outline"
              color="#16A34A"
              value={data?.todayEvents.length ?? 0}
              label={STR.today.events}
            />
          </View>

          {isEmpty ? (
            <StateView status="empty" icon="weather-sunny" emptyText={STR.today.empty} emptyHint={STR.today.emptyHint}>
              <AppButton
                label={STR.today.openSchedule}
                icon="calendar-month-outline"
                onPress={() => router.push('/schedule')}
              />
            </StateView>
          ) : null}

          {data && data.overdueTasks.length > 0 ? (
            <Section title={STR.today.overdue} color={theme.colors.error} count={data.overdueTasks.length}>
              {data.overdueTasks.map((task) => (
                <TaskBriefCard
                  key={task.id}
                  task={task}
                  projectName={projectNames.get(task.projectId)}
                  onPress={openTask}
                />
              ))}
            </Section>
          ) : null}

          {data && data.dueTodayTasks.length > 0 ? (
            <Section title={STR.today.dueToday} color={theme.colors.primary} count={data.dueTodayTasks.length}>
              {data.dueTodayTasks.map((task) => (
                <TaskBriefCard
                  key={task.id}
                  task={task}
                  projectName={projectNames.get(task.projectId)}
                  onPress={openTask}
                />
              ))}
            </Section>
          ) : null}

          {data && data.upcomingTasks.length > 0 ? (
            <Section title={STR.today.upcoming} color="#F59E0B" count={data.upcomingTasks.length}>
              {data.upcomingTasks.map((task) => (
                <TaskBriefCard
                  key={task.id}
                  task={task}
                  projectName={projectNames.get(task.projectId)}
                  onPress={openTask}
                />
              ))}
            </Section>
          ) : null}

          {data && data.todayEvents.length > 0 ? (
            <Section title={STR.today.events} color="#16A34A" count={data.todayEvents.length}>
              {data.todayEvents.map((event) => {
                const overdue = isOverdue(event.endTime);
                return (
                  <TouchableRipple
                    key={event.id}
                    onPress={() => openEvent(event)}
                    style={styles.eventTouch}
                  >
                    <View style={[styles.eventRow, { backgroundColor: theme.colors.surface, borderColor: theme.colors.outline }]}>
                    <View style={[styles.eventTime, { backgroundColor: `${theme.colors.primary}14` }]}>
                      <Text variant="labelMedium" style={{ color: theme.colors.primary, fontWeight: '700' }}>
                        {formatTime(event.startTime)}
                      </Text>
                      <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                        {formatTime(event.endTime)}
                      </Text>
                    </View>
                    <View style={styles.eventBody}>
                      <Text variant="titleSmall" numberOfLines={2}>
                        {event.title}
                      </Text>
                      {event.taskId ? (
                        <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                          Có liên kết công việc
                        </Text>
                      ) : null}
                    </View>
                    <Icon source={overdue ? 'check-circle-outline' : 'clock-outline'} size={18} color={theme.colors.onSurfaceVariant} />
                    </View>
                  </TouchableRipple>
                );
              })}
            </Section>
          ) : null}

          <View style={styles.bottomSpace} />
        </ScrollView>
      )}
    </View>
  );
}

function StatTile({
  icon,
  color,
  value,
  label,
}: {
  icon: string;
  color: string;
  value: number;
  label: string;
}) {
  const theme = useTheme();
  return (
    <Surface style={[styles.statTile, { backgroundColor: theme.colors.surface, borderColor: theme.colors.outline }]} elevation={0}>
      <View style={[styles.statIcon, { backgroundColor: `${color}19` }]}>
        <Icon source={icon} size={18} color={color} />
      </View>
      <Text variant="headlineSmall" style={styles.statValue}>
        {value}
      </Text>
      <Text variant="labelSmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant }}>
        {label}
      </Text>
    </Surface>
  );
}

function Section({
  title,
  color,
  count,
  children,
}: {
  title: string;
  color: string;
  count: number;
  children: React.ReactNode;
}) {
  const theme = useTheme();
  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={[styles.sectionDot, { backgroundColor: color }]} />
        <Text variant="titleSmall" style={styles.sectionTitle}>
          {title}
        </Text>
        <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant }}>
          {count}
        </Text>
      </View>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  skeleton: { paddingTop: spacing.sm },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  statsGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  statTile: {
    flexBasis: '47%',
    flexGrow: 1,
    borderRadius: radius.lg,
    borderWidth: StyleSheet.hairlineWidth,
    padding: spacing.md,
    gap: 2,
  },
  statIcon: { width: 32, height: 32, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginBottom: 2 },
  statValue: { fontWeight: '700' },
  section: { marginTop: spacing.sm },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.sm },
  sectionDot: { width: 8, height: 8, borderRadius: 4 },
  sectionTitle: { flex: 1, fontWeight: '700' },
  eventTouch: { borderRadius: radius.md, marginBottom: spacing.sm },
  eventRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
    borderRadius: radius.md,
    borderWidth: StyleSheet.hairlineWidth,
  },
  eventTime: { alignItems: 'center', borderRadius: radius.md, paddingHorizontal: spacing.sm, paddingVertical: spacing.xs },
  eventBody: { flex: 1, gap: 2 },
  bottomSpace: { height: spacing.xl },
});
