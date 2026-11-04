import React, { useEffect, useMemo, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useForm } from 'react-hook-form';
import { Button, Checkbox, Chip, Divider, Text, useTheme } from 'react-native-paper';
import { AppButton } from '../../components/AppButton';
import { ChoiceRow } from '../../components/ChoiceRow';
import { ControlledField } from '../../components/ControlledField';
import { DateTimeField } from '../../components/DateTimeField';
import { MultiSelectDialog } from '../../components/MultiSelectDialog';
import { useLabels } from '../../features/tasks/hooks';
import { useMembers } from '../../features/projects/hooks';
import { radius, spacing } from '../../theme/tokens';
import type { CreateTaskPayload, TaskDto, TaskPriority, TaskStatus } from '../../types/api';
import { STR } from '../../utils/constants';
import { zodResolver } from '../../utils/zodResolver';
import { taskTextSchema } from '../../schemas/task';
import { toDate } from '../../utils/format';
import { TaskPriorityChip, TaskStatusChip } from '../../components/TaskChips';

export interface TaskFormValues {
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  startDate: Date | null;
  deadline: Date | null;
  assigneeIds: string[];
  labelIds: string[];
}

interface Props {
  projectId: string;
  initial?: TaskDto;
  submitting?: boolean;
  readOnly?: boolean;
  onSubmit: (payload: CreateTaskPayload) => void;
}

const STATUS_OPTIONS: { value: TaskStatus; label: string }[] = (
  ['TODO', 'DOING', 'REVIEW', 'DONE'] as TaskStatus[]
).map((value) => ({ value, label: STR.tasks.statuses[value] }));

const PRIORITY_OPTIONS: { value: TaskPriority; label: string }[] = (
  ['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as TaskPriority[]
).map((value) => ({ value, label: STR.tasks.priorities[value] }));

/**
 * Form tạo/sửa task — validate khớp TaskRequest backend.
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export function TaskForm({ projectId, initial, submitting = false, readOnly = false, onSubmit }: Props) {
  const theme = useTheme();
  const members = useMembers(projectId);
  const labels = useLabels(projectId);

  const form = useForm<{ title: string; description: string }>({
    resolver: zodResolver(taskTextSchema),
    defaultValues: {
      title: initial?.title ?? '',
      description: initial?.description ?? '',
    },
  });

  const [status, setStatus] = useState<TaskStatus>(initial?.status ?? 'TODO');
  const [priority, setPriority] = useState<TaskPriority>(initial?.priority ?? 'MEDIUM');
  const [hasStart, setHasStart] = useState(Boolean(initial?.startDate));
  const [startDate, setStartDate] = useState<Date>(toDate(initial?.startDate));
  const [hasDeadline, setHasDeadline] = useState(Boolean(initial?.deadline));
  const [deadline, setDeadline] = useState<Date>(toDate(initial?.deadline));
  const [assigneeIds, setAssigneeIds] = useState<string[]>(initial?.assignees.map((item) => item.userId) ?? []);
  const [labelIds, setLabelIds] = useState<string[]>(initial?.labels.map((item) => item.id) ?? []);
  const [assigneeDialog, setAssigneeDialog] = useState(false);
  const [labelDialog, setLabelDialog] = useState(false);
  const [dateError, setDateError] = useState<string | null>(null);

  useEffect(() => {
    form.reset({ title: initial?.title ?? '', description: initial?.description ?? '' });
    setStatus(initial?.status ?? 'TODO');
    setPriority(initial?.priority ?? 'MEDIUM');
    setHasStart(Boolean(initial?.startDate));
    setStartDate(toDate(initial?.startDate));
    setHasDeadline(Boolean(initial?.deadline));
    setDeadline(toDate(initial?.deadline));
    setAssigneeIds(initial?.assignees.map((item) => item.userId) ?? []);
    setLabelIds(initial?.labels.map((item) => item.id) ?? []);
  }, [initial, form]);

  const memberOptions = useMemo(
    () => (members.data ?? []).map((member) => ({ id: member.userId, label: member.fullName, description: member.email })),
    [members.data],
  );

  const labelOptions = useMemo(
    () => (labels.data ?? []).map((label) => ({ id: label.id, label: label.name })),
    [labels.data],
  );

  const submit = form.handleSubmit((values) => {
    setDateError(null);
    if (hasStart && hasDeadline && deadline.getTime() < startDate.getTime()) {
      setDateError(STR.tasks.deadlineInvalid);
      return;
    }

    const payload: CreateTaskPayload = {
      projectId,
      title: values.title.trim(),
      description: values.description.trim() || undefined,
      status,
      priority,
      startDate: hasStart ? startDate.toISOString() : null,
      deadline: hasDeadline ? deadline.toISOString() : null,
      assigneeIds,
      labelIds,
      version: initial?.version,
    };
    onSubmit(payload);
  });

  return (
    <View style={styles.container}>
      <ControlledField
        control={form.control}
        name="title"
        label={STR.tasks.titleLabel}
        error={form.formState.errors.title?.message}
        autoCapitalize="sentences"
        maxLength={200}
        leftIcon="format-title"
        disabled={readOnly}
      />
      <ControlledField
        control={form.control}
        name="description"
        label={STR.tasks.descriptionLabel}
        error={form.formState.errors.description?.message}
        autoCapitalize="sentences"
        maxLength={5000}
        multiline
        disabled={readOnly}
      />

      <Divider style={styles.divider} />

      <ChoiceRow label={STR.tasks.status} value={status} options={STATUS_OPTIONS} onChange={setStatus} />
      <ChoiceRow label={STR.tasks.priority} value={priority} options={PRIORITY_OPTIONS} onChange={setPriority} />

      <Row label={STR.tasks.startDate}>
        <Checkbox.Item
          label=""
          status={hasStart ? 'checked' : 'unchecked'}
          onPress={() => setHasStart((value) => !value)}
          style={styles.checkbox}
        />
        {hasStart ? (
          <View style={styles.field}>
            <DateTimeField
              label={STR.tasks.startDate}
              value={startDate}
              mode="date"
              onChange={setStartDate}
            />
          </View>
        ) : null}
      </Row>

      <Row label={STR.tasks.deadline}>
        <Checkbox.Item
          label=""
          status={hasDeadline ? 'checked' : 'unchecked'}
          onPress={() => setHasDeadline((value) => !value)}
          style={styles.checkbox}
        />
        {hasDeadline ? (
          <View style={styles.field}>
            <DateTimeField
              label={STR.tasks.deadline}
              value={deadline}
              mode="date"
              onChange={setDeadline}
              error={dateError ?? undefined}
            />
          </View>
        ) : null}
      </Row>

      <Divider style={styles.divider} />

      <Text variant="labelLarge" style={[styles.groupLabel, { color: theme.colors.onSurfaceVariant }]}>
        {STR.tasks.assignees}
      </Text>
      <View style={styles.chips}>
        {assigneeIds.length === 0 ? (
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {STR.tasks.noAssignees}
          </Text>
        ) : (
          assigneeIds.map((id) => {
            const member = members.data?.find((item) => item.userId === id);
            return <Chip key={id} compact icon="account">{member?.fullName ?? id}</Chip>;
          })
        )}
      </View>
      {!readOnly ? (
        <Button mode="text" icon="account-plus-outline" onPress={() => setAssigneeDialog(true)} style={styles.addButton}>
          {STR.tasks.selectAssignees}
        </Button>
      ) : null}

      <Text variant="labelLarge" style={[styles.groupLabel, { color: theme.colors.onSurfaceVariant }]}>
        {STR.tasks.labels}
      </Text>
      <View style={styles.chips}>
        {labelIds.length === 0 ? (
          <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
            {STR.tasks.noLabels}
          </Text>
        ) : (
          labelIds.map((id) => {
            const label = labels.data?.find((item) => item.id === id);
            return (
              <Chip key={id} compact style={{ backgroundColor: `${label?.color ?? '#64748B'}1A` }}>
                {label?.name ?? id}
              </Chip>
            );
          })
        )}
      </View>
      {!readOnly ? (
        <Button mode="text" icon="label-outline" onPress={() => setLabelDialog(true)} style={styles.addButton}>
          {STR.tasks.selectLabels}
        </Button>
      ) : null}

      {!readOnly ? (
        <AppButton
          label={initial ? STR.save : STR.tasks.createTitle}
          icon="content-save-outline"
          onPress={submit}
          loading={submitting}
          style={styles.submit}
        />
      ) : null}

      <View style={styles.preview}>
        <TaskStatusChip status={status} />
        <TaskPriorityChip priority={priority} />
      </View>

      <MultiSelectDialog
        visible={assigneeDialog}
        title={STR.tasks.selectAssignees}
        options={memberOptions}
        selectedIds={assigneeIds}
        onClose={() => setAssigneeDialog(false)}
        onConfirm={(ids) => {
          setAssigneeIds(ids);
          setAssigneeDialog(false);
        }}
      />
      <MultiSelectDialog
        visible={labelDialog}
        title={STR.tasks.selectLabels}
        options={labelOptions}
        selectedIds={labelIds}
        onClose={() => setLabelDialog(false)}
        onConfirm={(ids) => {
          setLabelIds(ids);
          setLabelDialog(false);
        }}
      />
    </View>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  const theme = useTheme();
  return (
    <View style={styles.dateRow}>
      <Text variant="labelLarge" style={[styles.dateLabel, { color: theme.colors.onSurfaceVariant }]}>
        {label}
      </Text>
      <View style={styles.dateContent}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { gap: spacing.xs },
  divider: { marginVertical: spacing.md },
  groupLabel: { marginTop: spacing.sm, fontWeight: '600' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs, minHeight: 28, alignItems: 'center' },
  addButton: { alignSelf: 'flex-start' },
  submit: { marginTop: spacing.lg },
  dateRow: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm, marginBottom: spacing.xs },
  dateLabel: { width: 110, marginTop: spacing.md, fontWeight: '600' },
  dateContent: { flex: 1, flexDirection: 'row', alignItems: 'flex-start' },
  checkbox: { paddingHorizontal: 0, marginLeft: -spacing.sm },
  field: { flex: 1 },
  preview: { flexDirection: 'row', gap: spacing.xs, marginTop: spacing.lg },
});
