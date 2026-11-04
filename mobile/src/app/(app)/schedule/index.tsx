import dayjs from 'dayjs';
import { router, Stack } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
import { FAB, Icon, IconButton, Text, TouchableRipple, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { StateView } from '@/components/StateView';
import { useScheduleEvents } from '@/features/schedule/hooks';
import { radius, spacing } from '@/theme/tokens';
import type { ScheduleEventDto } from '@/types/api';
import { STR } from '@/utils/constants';
import { formatDayLabel, formatTime } from '@/utils/format';

/**
 * Lịch cá nhân: lưới tháng + agenda theo ngày.
 * @reference docs/plan-app/02-phase-2.md §5.5
 */
export default function ScheduleScreen() {
  const theme = useTheme();
  const [month, setMonth] = useState(() => dayjs().startOf('month'));
  const [selected, setSelected] = useState(() => dayjs());
  const [refreshing, setRefreshing] = useState(false);

  const gridStart = useMemo(() => {
    const first = month.startOf('month');
    const offset = (first.day() + 6) % 7;
    return first.subtract(offset, 'day');
  }, [month]);
  const gridEnd = useMemo(() => gridStart.add(41, 'day').endOf('day'), [gridStart]);

  const events = useScheduleEvents(gridStart.toISOString(), gridEnd.toISOString());

  const days = useMemo(
    () => Array.from({ length: 42 }).map((_, index) => gridStart.add(index, 'day')),
    [gridStart],
  );

  const eventsByDay = useMemo(() => {
    const map = new Map<string, ScheduleEventDto[]>();
    events.data?.forEach((event) => {
      const key = dayjs(event.startTime).format('YYYY-MM-DD');
      const list = map.get(key) ?? [];
      list.push(event);
      map.set(key, list);
    });
    map.forEach((list) => list.sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime()));
    return map;
  }, [events.data]);

  const dayEvents = eventsByDay.get(selected.format('YYYY-MM-DD')) ?? [];

  const goToday = () => {
    setMonth(dayjs().startOf('month'));
    setSelected(dayjs());
  };

  const onRefresh = async () => {
    setRefreshing(true);
    try {
      await events.refetch();
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen
        options={{
          title: STR.schedule.title,
          headerRight: () => <IconButton icon="plus" onPress={() => router.push('/schedule/new')} />,
        }}
      />

      {events.isError ? (
        <StateView status="error" errorText={normalizeError(events.error).message} onRetry={() => events.refetch()} />
      ) : (
        <ScrollView
          contentContainerStyle={styles.container}
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.primary} />}
        >
          <View style={styles.monthHeader}>
            <IconButton icon="chevron-left" onPress={() => setMonth((current) => current.subtract(1, 'month'))} />
            <Text variant="titleMedium" style={styles.monthTitle}>
              {STR.schedule.monthTitle} {month.format('MM/YYYY')}
            </Text>
            <IconButton icon="chevron-right" onPress={() => setMonth((current) => current.add(1, 'month'))} />
            <IconButton icon="calendar-today" onPress={goToday} />
          </View>

          <View style={styles.weekdays}>
            {STR.schedule.weekdays.map((weekday) => (
              <Text key={weekday} variant="labelSmall" style={[styles.weekday, { color: theme.colors.onSurfaceVariant }]}>
                {weekday}
              </Text>
            ))}
          </View>

          {events.isPending ? (
            <ActivityIndicator style={styles.loading} />
          ) : (
            <View style={styles.grid}>
              {days.map((day) => {
                const key = day.format('YYYY-MM-DD');
                const dayList = eventsByDay.get(key) ?? [];
                const isSelected = day.isSame(selected, 'day');
                const isToday = day.isSame(dayjs(), 'day');
                const outside = !day.isSame(month, 'month');

                return (
                  <TouchableRipple
                    key={key}
                    onPress={() => setSelected(day)}
                    style={[
                      styles.dayCell,
                      isSelected ? { backgroundColor: theme.colors.primaryContainer } : null,
                      isToday && !isSelected ? { borderColor: theme.colors.primary } : null,
                    ]}
                  >
                    <View style={styles.dayContent}>
                      <Text
                        variant="labelMedium"
                        style={{
                          color: isSelected
                            ? theme.colors.onPrimaryContainer
                            : outside
                              ? theme.colors.onSurfaceVariant
                              : theme.colors.onSurface,
                          fontWeight: isToday || isSelected ? '700' : '400',
                          opacity: outside ? 0.45 : 1,
                        }}
                      >
                        {day.date()}
                      </Text>
                      <View style={styles.dots}>
                        {dayList.slice(0, 3).map((event) => (
                          <View key={event.id} style={[styles.dot, { backgroundColor: theme.colors.primary }]} />
                        ))}
                      </View>
                    </View>
                  </TouchableRipple>
                );
              })}
            </View>
          )}

          <View style={styles.agendaHeader}>
            <Icon source="calendar-check-outline" size={18} color={theme.colors.primary} />
            <Text variant="titleSmall" style={styles.agendaTitle}>
              {formatDayLabel(selected.toDate())}
            </Text>
            <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
              {dayEvents.length} {STR.schedule.eventCount}
            </Text>
          </View>

          {dayEvents.length === 0 ? (
            <View style={styles.emptyAgenda}>
              <Icon source="calendar-blank-outline" size={30} color={theme.colors.onSurfaceVariant} />
              <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                {STR.schedule.noEventsToday}
              </Text>
            </View>
          ) : (
            dayEvents.map((event) => (
              <TouchableRipple key={event.id} onPress={() => router.push(`/schedule/${event.id}`)} borderless style={styles.eventTouch}>
                <View style={[styles.eventRow, { backgroundColor: theme.colors.surface, borderColor: theme.colors.outline }]}>
                  <View style={[styles.eventTime, { backgroundColor: `${theme.colors.primary}12` }]}>
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
                    {event.description ? (
                      <Text variant="bodySmall" numberOfLines={1} style={{ color: theme.colors.onSurfaceVariant }}>
                        {event.description}
                      </Text>
                    ) : null}
                    {event.taskId ? (
                      <Text variant="labelSmall" style={{ color: theme.colors.primary }}>
                        Có liên kết công việc
                      </Text>
                    ) : null}
                  </View>
                  <Icon source="chevron-right" size={18} color={theme.colors.onSurfaceVariant} />
                </View>
              </TouchableRipple>
            ))
          )}
        </ScrollView>
      )}

      <FAB icon="plus" label={STR.schedule.createTitle} style={styles.fab} onPress={() => router.push('/schedule/new')} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, paddingBottom: 120 },
  monthHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  monthTitle: { fontWeight: '700' },
  weekdays: { flexDirection: 'row', marginTop: spacing.xs },
  weekday: { flex: 1, textAlign: 'center', fontWeight: '600' },
  grid: { flexDirection: 'row', flexWrap: 'wrap' },
  dayCell: {
    width: `${100 / 7}%`,
    aspectRatio: 1,
    borderRadius: radius.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'transparent',
    padding: 2,
  },
  dayContent: { flex: 1, alignItems: 'center', paddingTop: 4, gap: 3 },
  dots: { flexDirection: 'row', gap: 2, flexWrap: 'wrap', justifyContent: 'center' },
  dot: { width: 4, height: 4, borderRadius: 2 },
  loading: { paddingVertical: spacing.xl },
  agendaHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginTop: spacing.lg, marginBottom: spacing.sm },
  agendaTitle: { flex: 1, fontWeight: '700' },
  emptyAgenda: { alignItems: 'center', gap: spacing.xs, paddingVertical: spacing.xl },
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
  fab: { position: 'absolute', right: spacing.lg, bottom: spacing.xl },
});
