import React, { useEffect, useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useQueries } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Button, Chip, Divider, Text, useTheme } from 'react-native-paper';
import { AppButton } from '../../components/AppButton';
import { ControlledField } from '../../components/ControlledField';
import { DateTimeField } from '../../components/DateTimeField';
import { MultiSelectDialog } from '../../components/MultiSelectDialog';
import { tasksApi } from '../../api/tasks';
import { useProjects } from '../../features/projects/hooks';
import { radius, spacing } from '../../theme/tokens';
import type { ScheduleEventDto, ScheduleEventPayload } from '../../types/api';
import { STR } from '../../utils/constants';
import { zodResolver } from '../../utils/zodResolver';
import { scheduleTextSchema } from '../../schemas/schedule';

interface Props {
  initial?: ScheduleEventDto;
  submitting?: boolean;
  onSubmit: (payload: ScheduleEventPayload) => void;
}

function nextHour(): Date {
  const date = new Date();
  date.setMinutes(0, 0, 0);
  date.setHours(date.getHours() + 1);
  return date;
}

function combine(date: Date, time: Date): Date {
  const result = new Date(date);
  result.setHours(time.getHours(), time.getMinutes(), 0, 0);
  return result;
}

/**
 * Form tạo/sửa sự kiện lịch — start < end, liên kết task tuỳ chọn.
 * @reference docs/plan-app/02-phase-2.md §5.5
 */
export function ScheduleEventForm({ initial, submitting = false, onSubmit }: Props) {
  const theme = useTheme();
  const projects = useProjects();

  const form = useForm<{ title: string; description: string }>({
    resolver: zodResolver(scheduleTextSchema),
    defaultValues: {
      title: initial?.title ?? '',
      description: initial?.description ?? '',
    },
  });

  const start = initial ? new Date(initial.startTime) : new Date();
  const end = initial ? new Date(initial.endTime) : nextHour();

  const [startDate, setStartDate] = useState(start);
  const [startTime, setStartTime] = useState(start);
  const [endDate, setEndDate] = useState(end);
  const [endTime, setEndTime] = useState(end);
  const [taskId, setTaskId] = useState<string | null>(initial?.taskId ?? null);
  const [taskDialog, setTaskDialog] = useState(false);
  const [timeError, setTimeError] = useState<string | null>(null);

  useEffect(() => {
    form.reset({ title: initial?.title ?? '', description: initial?.description ?? '' });
    const baseStart = initial ? new Date(initial.startTime) : new Date();
    const baseEnd = initial ? new Date(initial.endTime) : nextHour();
    setStartDate(baseStart);
    setStartTime(baseStart);
    setEndDate(baseEnd);
    setEndTime(baseEnd);
    setTaskId(initial?.taskId ?? null);
  }, [initial, form]);

  const taskQueries = useQueries({
    queries: (projects.data ?? []).map((project) => ({
      queryKey: ['tasks', project.id],
      queryFn: () => tasksApi.list(project.id),
      enabled: Boolean(project.id),
    })),
  });

  const taskOptions = useMemo(() => {
    const options: { id: string; label: string; description?: string }[] = [];
    (projects.data ?? []).forEach((project, index) => {
      const tasks = taskQueries[index]?.data ?? [];
      tasks.forEach((task) => {
        options.push({ id: task.id, label: task.title, description: project.name });
      });
    });
    return options;
  }, [projects.data, taskQueries]);

  const selectedTask = taskOptions.find((option) => option.id === taskId);

  const submit = form.handleSubmit((values) => {
    setTimeError(null);
    const startValue = combine(startDate, startTime);
    const endValue = combine(endDate, endTime);
    if (endValue.getTime() <= startValue.getTime()) {
      setTimeError(STR.schedule.timeInvalid);
      return;
    }

    onSubmit({
      title: values.title.trim(),
      description: values.description.trim() || undefined,
      startTime: startValue.toISOString(),
      endTime: endValue.toISOString(),
      taskId: taskId || null,
    });
  });

  return (
    <View style={styles.container}>
      <ControlledField
        control={form.control}
        name="title"
        label={STR.schedule.titleLabel}
        error={form.formState.errors.title?.message}
        autoCapitalize="sentences"
        maxLength={200}
        leftIcon="calendar-edit"
      />
      <ControlledField
        control={form.control}
        name="description"
        label={STR.schedule.descriptionLabel}
        error={form.formState.errors.description?.message}
        autoCapitalize="sentences"
        maxLength={2000}
        multiline
      />

      <Divider style={styles.divider} />

      <Text variant="labelLarge" style={[styles.section, { color: theme.colors.onSurfaceVariant }]}>
        {STR.schedule.startTime}
      </Text>
      <View style={styles.row}>
        <View style={styles.flex}>
          <DateTimeField label="Ngày" value={startDate} mode="date" onChange={setStartDate} />
        </View>
        <View style={styles.flex}>
          <DateTimeField label="Giờ" value={startTime} mode="time" onChange={setStartTime} />
        </View>
      </View>

      <Text variant="labelLarge" style={[styles.section, { color: theme.colors.onSurfaceVariant }]}>
        {STR.schedule.endTime}
      </Text>
      <View style={styles.row}>
        <View style={styles.flex}>
          <DateTimeField
            label="Ngày"
            value={endDate}
            mode="date"
            onChange={setEndDate}
            error={timeError ?? undefined}
          />
        </View>
        <View style={styles.flex}>
          <DateTimeField label="Giờ" value={endTime} mode="time" onChange={setEndTime} />
        </View>
      </View>

      <Text variant="labelLarge" style={[styles.section, { color: theme.colors.onSurfaceVariant }]}>
        {STR.schedule.linkTask}
      </Text>
      {selectedTask ? (
        <View style={styles.chips}>
          <Chip icon="link-variant" onClose={() => setTaskId(null)}>
            {selectedTask.label}
          </Chip>
        </View>
      ) : (
        <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
          {STR.schedule.noTask}
        </Text>
      )}
      <Button mode="text" icon="link-plus" onPress={() => setTaskDialog(true)} style={styles.addButton}>
        {STR.schedule.linkTask}
      </Button>

      <AppButton
        label={initial ? STR.save : STR.schedule.createTitle}
        icon="content-save-outline"
        onPress={submit}
        loading={submitting}
        style={styles.submit}
      />

      <MultiSelectDialog
        visible={taskDialog}
        title={STR.schedule.linkTask}
        options={taskOptions}
        selectedIds={taskId ? [taskId] : []}
        onClose={() => setTaskDialog(false)}
        onConfirm={(ids) => {
          setTaskId(ids[ids.length - 1] ?? null);
          setTaskDialog(false);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs },
  divider: { marginVertical: spacing.md },
  section: { fontWeight: '600', marginTop: spacing.sm, marginBottom: spacing.xs },
  row: { flexDirection: 'row', gap: spacing.sm },
  flex: { flex: 1 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs },
  addButton: { alignSelf: 'flex-start' },
  submit: { marginTop: spacing.lg },
  card: { borderRadius: radius.lg },
});
