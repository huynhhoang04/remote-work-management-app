import { router, Stack, useLocalSearchParams } from 'expo-router';
import React, { useCallback, useMemo, useState } from 'react';
import { RefreshControl, ScrollView, SectionList, StyleSheet, View } from 'react-native';
import { useQueryClient } from '@tanstack/react-query';
import { Chip, FAB, IconButton, Menu, Searchbar, Text, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { KanbanBoard } from '@/components/KanbanBoard';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { TaskCard } from '@/components/TaskCard';
import { useMembers, useProject } from '@/features/projects/hooks';
import { useChangeTaskPosition, useLabels, useProjectTasks } from '@/features/tasks/hooks';
import { spacing, statusColors } from '@/theme/tokens';
import type { TaskDto, TaskPriority, TaskStatus } from '@/types/api';
import { STR } from '@/utils/constants';

const STATUSES: TaskStatus[] = ['TODO', 'DOING', 'REVIEW', 'DONE'];
const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

interface FilterOption {
  value: string;
  label: string;
}

/**
 * Bảng công việc dự án: Kanban kéo thả + danh sách, tìm kiếm/lọc.
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export default function ProjectKanbanScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const queryClient = useQueryClient();

  const project = useProject(id);
  const tasks = useProjectTasks(id);
  const members = useMembers(id);
  const labels = useLabels(id);
  const moveTask = useChangeTaskPosition(id);

  const [view, setView] = useState<'board' | 'list'>('board');
  const [query, setQuery] = useState('');
  const [priority, setPriority] = useState<string | undefined>();
  const [assignee, setAssignee] = useState<string | undefined>();
  const [label, setLabel] = useState<string | undefined>();

  const canWrite = Boolean(project.data && project.data.role !== 'VIEWER');

  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (tasks.data ?? []).filter((task) => {
      if (keyword && !task.title.toLowerCase().includes(keyword) && !(task.description ?? '').toLowerCase().includes(keyword)) {
        return false;
      }
      if (priority && task.priority !== priority) return false;
      if (assignee && !task.assignees.some((item) => item.userId === assignee)) return false;
      if (label && !task.labels.some((item) => item.id === label)) return false;
      return true;
    });
  }, [tasks.data, query, priority, assignee, label]);

  const sections = useMemo(
    () =>
      STATUSES.map((status) => ({
        status,
        title: (STR.tasks.statuses as Record<string, string>)[status] ?? status,
        data: filtered
          .filter((task) => task.status === status)
          .sort((a, b) => a.position - b.position),
      })).filter((section) => section.data.length > 0),
    [filtered],
  );

  const handleMove = useCallback(
    (task: TaskDto, status: TaskStatus, position: number) => {
      queryClient.setQueryData<TaskDto[]>(['tasks', id], (current) =>
        current?.map((item) => (item.id === task.id ? { ...item, status, position } : item)),
      );
      moveTask.mutate(
        { taskId: task.id, status, position },
        {
          onError: (error) => {
            show(normalizeError(error).message);
            void tasks.refetch();
          },
        },
      );
    },
    [id, moveTask, queryClient, show, tasks],
  );

  const openTask = useCallback((task: TaskDto) => {
    router.push(`/tasks/${task.id}`);
  }, []);

  const createTask = () => router.push(`/tasks/new?projectId=${id}`);

  const clearFilters = () => {
    setPriority(undefined);
    setAssignee(undefined);
    setLabel(undefined);
    setQuery('');
  };

  const memberOptions: FilterOption[] =
    members.data?.map((member) => ({ value: member.userId, label: member.fullName })) ?? [];
  const labelOptions: FilterOption[] =
    labels.data?.map((item) => ({ value: item.id, label: item.name })) ?? [];
  const priorityOptions: FilterOption[] = PRIORITIES.map((value) => ({
    value,
    label: (STR.tasks.priorities as Record<string, string>)[value],
  }));

  const hasFilter = Boolean(priority || assignee || label || query);

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen
        options={{
          title: project.data?.name ?? STR.tasks.title,
          headerRight: () => (
            <IconButton
              icon={view === 'board' ? 'view-list-outline' : 'view-column-outline'}
              onPress={() => setView((current) => (current === 'board' ? 'list' : 'board'))}
            />
          ),
        }}
      />

      <View style={styles.filters}>
        <Searchbar
          placeholder={STR.common.search}
          value={query}
          onChangeText={setQuery}
          style={styles.search}
          inputStyle={styles.searchInput}
        />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipRow}>
          <FilterMenu label={STR.tasks.priority} value={priority} options={priorityOptions} onSelect={setPriority} />
          <FilterMenu label={STR.tasks.assignees} value={assignee} options={memberOptions} onSelect={setAssignee} />
          <FilterMenu label={STR.tasks.labels} value={label} options={labelOptions} onSelect={setLabel} />
          {hasFilter ? <Chip icon="close" onPress={clearFilters}>{STR.common.clear}</Chip> : null}
        </ScrollView>
      </View>

      {tasks.isPending ? (
        <View style={styles.skeleton}>
          <SkeletonList count={4} />
        </View>
      ) : tasks.isError ? (
        <StateView status="error" errorText={normalizeError(tasks.error).message} onRetry={() => tasks.refetch()} />
      ) : (tasks.data?.length ?? 0) === 0 ? (
        <StateView status="empty" icon="clipboard-text-outline" emptyText={STR.tasks.empty} emptyHint={STR.tasks.emptyHint}>
          {canWrite ? <AppButton label={STR.tasks.createTitle} icon="plus" onPress={createTask} /> : null}
        </StateView>
      ) : filtered.length === 0 ? (
        <StateView status="empty" icon="filter-off-outline" emptyText="Không có công việc khớp bộ lọc">
          <AppButton label={STR.common.clear} icon="filter-off-outline" onPress={clearFilters} />
        </StateView>
      ) : view === 'board' ? (
        <KanbanBoard tasks={filtered} onOpenTask={openTask} onMoveTask={handleMove} readOnly={!canWrite} />
      ) : (
        <SectionList
          sections={sections}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => <TaskCard task={item} onPress={openTask} />}
          renderSectionHeader={({ section }) => (
            <View style={[styles.sectionHeader, { backgroundColor: theme.colors.background }]}>
              <View style={[styles.dot, { backgroundColor: statusColors[section.status] }]} />
              <Text variant="titleSmall" style={styles.sectionTitle}>
                {section.title}
              </Text>
              <Text variant="labelMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                {section.data.length}
              </Text>
            </View>
          )}
          contentContainerStyle={styles.list}
          stickySectionHeadersEnabled={false}
          refreshControl={
            <RefreshControl
              refreshing={tasks.isRefetching}
              onRefresh={() => void tasks.refetch()}
              tintColor={theme.colors.primary}
            />
          }
        />
      )}

      {canWrite ? (
        <FAB icon="plus" label={STR.tasks.createTitle} style={styles.fab} onPress={createTask} />
      ) : null}
    </View>
  );
}

function FilterMenu({
  label,
  value,
  options,
  onSelect,
}: {
  label: string;
  value?: string;
  options: FilterOption[];
  onSelect: (value: string | undefined) => void;
}) {
  const [visible, setVisible] = useState(false);
  const selected = options.find((option) => option.value === value);

  return (
    <Menu
      visible={visible}
      onDismiss={() => setVisible(false)}
      anchor={
        <Chip selected={Boolean(value)} icon="chevron-down" onPress={() => setVisible(true)} style={styles.filterChip}>
          {selected?.label ?? label}
        </Chip>
      }
    >
      <Menu.Item
        title={STR.common.all}
        onPress={() => {
          onSelect(undefined);
          setVisible(false);
        }}
      />
      {options.map((option) => (
        <Menu.Item
          key={option.value}
          title={option.label}
          onPress={() => {
            onSelect(option.value);
            setVisible(false);
          }}
        />
      ))}
    </Menu>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  filters: { paddingHorizontal: spacing.lg, paddingBottom: spacing.sm, gap: spacing.sm },
  search: { borderRadius: 999 },
  searchInput: { fontSize: 14 },
  chipRow: { gap: spacing.xs, paddingRight: spacing.lg },
  filterChip: { borderRadius: 999 },
  skeleton: { paddingTop: spacing.sm },
  list: { paddingHorizontal: spacing.lg, paddingBottom: 120 },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, paddingVertical: spacing.sm },
  dot: { width: 8, height: 8, borderRadius: 4 },
  sectionTitle: { flex: 1, fontWeight: '700' },
  fab: { position: 'absolute', right: spacing.lg, bottom: spacing.xl },
});
