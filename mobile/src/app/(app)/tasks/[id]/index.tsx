import { router, Stack, useLocalSearchParams } from 'expo-router';
import React, { useMemo, useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import {
  Avatar,
  Button,
  Card,
  Checkbox,
  Chip,
  Divider,
  Icon,
  IconButton,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { filesApi } from '@/api/files';
import { AppButton } from '@/components/AppButton';
import { AssigneeAvatars } from '@/components/AssigneeAvatars';
import { ChoiceRow } from '@/components/ChoiceRow';
import { MultiSelectDialog } from '@/components/MultiSelectDialog';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { LabelChip, TaskPriorityChip, TaskStatusChip } from '@/components/TaskChips';
import { useProject, useMembers } from '@/features/projects/hooks';
import {
  useAddComment,
  useAddSubtask,
  useChangeTaskStatus,
  useDeleteAttachment,
  useDeleteComment,
  useDeleteSubtask,
  useDeleteTask,
  useTask,
  useUpdateSubtask,
  useUploadAttachment,
  useComments,
} from '@/features/tasks/hooks';
import { useSession } from '@/store/session';
import { radius, spacing } from '@/theme/tokens';
import type { CommentDto, FileDto, TaskStatus } from '@/types/api';
import { STR } from '@/utils/constants';
import {
  colorFromString,
  formatBytes,
  formatDate,
  formatRelative,
  initials,
  isOverdue,
} from '@/utils/format';
import { pickDocument, pickImage } from '@/utils/pickers';

const STATUS_OPTIONS: { value: TaskStatus; label: string }[] = (
  ['TODO', 'DOING', 'REVIEW', 'DONE'] as TaskStatus[]
).map((value) => ({ value, label: STR.tasks.statuses[value] }));

/**
 * Chi tiết task: trạng thái, mô tả, checklist, đính kèm, bình luận.
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export default function TaskDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const { user } = useSession();

  const task = useTask(id);
  const project = useProject(task.data?.projectId ?? '');
  const members = useMembers(task.data?.projectId ?? '');
  const comments = useComments(id);

  const changeStatus = useChangeTaskStatus(task.data?.projectId);
  const deleteTask = useDeleteTask();
  const addSubtask = useAddSubtask(id);
  const updateSubtask = useUpdateSubtask(id);
  const deleteSubtask = useDeleteSubtask(id);
  const uploadAttachment = useUploadAttachment(id);
  const deleteAttachment = useDeleteAttachment(id);
  const addComment = useAddComment(id);
  const deleteComment = useDeleteComment(id);

  const [subtaskTitle, setSubtaskTitle] = useState('');
  const [commentText, setCommentText] = useState('');
  const [mentionIds, setMentionIds] = useState<string[]>([]);
  const [mentionDialog, setMentionDialog] = useState(false);

  const canWrite = project.data ? project.data.role !== 'VIEWER' : false;
  const memberOptions = useMemo(
    () => (members.data ?? []).map((member) => ({ id: member.userId, label: member.fullName, description: member.email })),
    [members.data],
  );

  const showError = (error: unknown) => show(normalizeError(error).message);

  const onChangeStatus = async (status: TaskStatus) => {
    try {
      await changeStatus.mutateAsync({ taskId: id, status });
      show(STR.tasks.statusChanged);
    } catch (error) {
      showError(error);
    }
  };

  const confirmDeleteTask = () => {
    Alert.alert(STR.common.delete, STR.tasks.deleteConfirm, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.common.delete,
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteTask.mutateAsync(id);
            show(STR.common.deleted);
            router.back();
          } catch (error) {
            showError(error);
          }
        },
      },
    ]);
  };

  const onAddSubtask = async () => {
    const title = subtaskTitle.trim();
    if (!title) return;
    try {
      await addSubtask.mutateAsync(title);
      setSubtaskTitle('');
    } catch (error) {
      showError(error);
    }
  };

  const onUpload = async (source: 'file' | 'image' | 'camera') => {
    try {
      const file = source === 'file' ? await pickDocument() : await pickImage(source === 'camera');
      if (!file) return;
      await uploadAttachment.mutateAsync(file);
      show(STR.tasks.attachmentUploaded);
    } catch (error) {
      showError(error);
    }
  };

  const openAttachment = async (file: FileDto) => {
    try {
      await filesApi.downloadAndShare(file);
    } catch (error) {
      showErrorMessage(show, error);
    }
  };

  const onAddComment = async () => {
    const content = commentText.trim();
    if (!content) return;
    try {
      await addComment.mutateAsync({ content, mentionUserIds: mentionIds });
      setCommentText('');
      setMentionIds([]);
    } catch (error) {
      showError(error);
    }
  };

  const onDeleteComment = (comment: CommentDto) => {
    Alert.alert(STR.common.deleteConfirm, undefined, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.common.delete,
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteComment.mutateAsync(comment.id);
            show(STR.tasks.commentDeleted);
          } catch (error) {
            showError(error);
          }
        },
      },
    ]);
  };

  if (task.isPending) {
    return (
      <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
        <Stack.Screen options={{ title: STR.tasks.detailTitle }} />
        <SkeletonList count={3} />
      </View>
    );
  }

  if (task.isError || !task.data) {
    return (
      <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
        <Stack.Screen options={{ title: STR.tasks.detailTitle }} />
        <StateView status="error" errorText={normalizeError(task.error).message} onRetry={() => task.refetch()} />
      </View>
    );
  }

  const value = task.data;
  const overdue = value.status !== 'DONE' && isOverdue(value.deadline);
  const doneSubtasks = value.subtasks.filter((item) => item.done).length;

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen
        options={{
          title: STR.tasks.detailTitle,
          headerRight: canWrite
            ? () => (
                <View style={styles.headerActions}>
                  <IconButton icon="pencil-outline" onPress={() => router.push(`/tasks/${id}/edit`)} />
                  <IconButton icon="trash-can-outline" iconColor={theme.colors.error} onPress={confirmDeleteTask} />
                </View>
              )
            : undefined,
        }}
      />
      <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
        <Card mode="elevated" style={styles.card}>
          <Card.Content style={styles.cardContent}>
            <Text variant="titleLarge" style={styles.title}>
              {value.title}
            </Text>
            <View style={styles.chipRow}>
              <TaskStatusChip status={value.status} />
              <TaskPriorityChip priority={value.priority} />
              {overdue ? <Chip compact textStyle={{ color: theme.colors.error }}>Quá hạn</Chip> : null}
            </View>

            {project.data ? (
              <Text
                variant="labelMedium"
                style={{ color: theme.colors.primary }}
                onPress={() => router.push(`/projects/${project.data?.id}`)}
              >
                {project.data.name}
              </Text>
            ) : null}

            <View style={styles.metaRow}>
              <Icon source="calendar-start" size={15} color={theme.colors.onSurfaceVariant} />
              <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {STR.tasks.startDate}: {value.startDate ? formatDate(value.startDate) : '—'}
              </Text>
            </View>
            <View style={styles.metaRow}>
              <Icon
                source={overdue ? 'alert-circle-outline' : 'calendar-clock-outline'}
                size={15}
                color={overdue ? theme.colors.error : theme.colors.onSurfaceVariant}
              />
              <Text variant="labelSmall" style={{ color: overdue ? theme.colors.error : theme.colors.onSurfaceVariant }}>
                {STR.tasks.deadline}: {value.deadline ? formatDate(value.deadline) : '—'}
              </Text>
            </View>

            {value.assignees.length > 0 ? (
              <View style={styles.assigneeRow}>
                <AssigneeAvatars assignees={value.assignees} size={30} max={5} />
                <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant, flex: 1 }} numberOfLines={1}>
                  {value.assignees.map((item) => item.fullName).join(', ')}
                </Text>
              </View>
            ) : null}

            {value.labels.length > 0 ? (
              <View style={styles.chipRow}>
                {value.labels.map((label) => (
                  <LabelChip key={label.id} label={label} />
                ))}
              </View>
            ) : null}
          </Card.Content>
        </Card>

        {canWrite ? (
          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <Text variant="titleSmall" style={styles.sectionTitle}>
                {STR.tasks.status}
              </Text>
              <ChoiceRow label="" value={value.status} options={STATUS_OPTIONS} onChange={onChangeStatus} />
            </Card.Content>
          </Card>
        ) : null}

        {value.description ? (
          <Card mode="elevated" style={styles.card}>
            <Card.Content>
              <Text variant="titleSmall" style={styles.sectionTitle}>
                {STR.tasks.descriptionLabel}
              </Text>
              <Text variant="bodyMedium" style={{ color: theme.colors.onSurface }}>
                {value.description}
              </Text>
            </Card.Content>
          </Card>
        ) : null}

        <Card mode="elevated" style={styles.card}>
          <Card.Content>
            <View style={styles.sectionHeader}>
              <Text variant="titleSmall" style={styles.sectionTitle}>
                {STR.tasks.subtasks}
              </Text>
              <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {doneSubtasks}/{value.subtasks.length}
              </Text>
            </View>
            {value.subtasks.map((subtask) => (
              <View key={subtask.id} style={styles.subtaskRow}>
                <Checkbox
                  status={subtask.done ? 'checked' : 'unchecked'}
                  onPress={() => {
                    if (!canWrite) return;
                    updateSubtask.mutate({ subtaskId: subtask.id, payload: { done: !subtask.done } });
                  }}
                />
                <Text
                  variant="bodyMedium"
                  style={[
                    styles.subtaskTitle,
                    {
                      textDecorationLine: subtask.done ? 'line-through' : 'none',
                      color: subtask.done ? theme.colors.onSurfaceVariant : theme.colors.onSurface,
                    },
                  ]}
                >
                  {subtask.title}
                </Text>
                {canWrite ? (
                  <IconButton
                    icon="trash-can-outline"
                    size={18}
                    onPress={() => deleteSubtask.mutate(subtask.id)}
                  />
                ) : null}
              </View>
            ))}
            {canWrite ? (
              <View style={styles.inlineInput}>
                <TextInput
                  mode="outlined"
                  dense
                  placeholder={STR.tasks.subtaskPlaceholder}
                  value={subtaskTitle}
                  onChangeText={setSubtaskTitle}
                  style={styles.flex}
                  maxLength={200}
                  onSubmitEditing={onAddSubtask}
                />
                <IconButton icon="plus" mode="contained" onPress={onAddSubtask} disabled={!subtaskTitle.trim()} />
              </View>
            ) : null}
          </Card.Content>
        </Card>

        <Card mode="elevated" style={styles.card}>
          <Card.Content>
            <View style={styles.sectionHeader}>
              <Text variant="titleSmall" style={styles.sectionTitle}>
                {STR.tasks.attachments} ({value.attachments.length})
              </Text>
              {canWrite ? (
                <IconButton
                  icon="paperclip"
                  size={18}
                  loading={uploadAttachment.isPending}
                  onPress={() =>
                    Alert.alert(STR.tasks.addAttachment, undefined, [
                      { text: STR.tasks.pickFile, onPress: () => void onUpload('file') },
                      { text: STR.tasks.pickImage, onPress: () => void onUpload('image') },
                      { text: STR.tasks.takePhoto, onPress: () => void onUpload('camera') },
                      { text: STR.cancel, style: 'cancel' },
                    ])
                  }
                />
              ) : null}
            </View>
            {value.attachments.length === 0 ? (
              <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {STR.common.noData}
              </Text>
            ) : (
              value.attachments.map((file, index) => (
                <View key={file.id}>
                  {index > 0 ? <Divider /> : null}
                  <View style={styles.fileRow}>
                    <Icon source={fileIcon(file.mimeType)} size={22} color={theme.colors.primary} />
                    <View style={styles.flex}>
                      <Text variant="bodyMedium" numberOfLines={1}>
                        {file.name}
                      </Text>
                      <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                        {formatBytes(file.size)}
                      </Text>
                    </View>
                    <IconButton icon="download-outline" size={20} onPress={() => void openAttachment(file)} />
                    {canWrite ? (
                      <IconButton
                        icon="close"
                        size={18}
                        iconColor={theme.colors.error}
                        onPress={() => deleteAttachment.mutate(file.id)}
                      />
                    ) : null}
                  </View>
                </View>
              ))
            )}
          </Card.Content>
        </Card>

        <Card mode="elevated" style={styles.card}>
          <Card.Content>
            <Text variant="titleSmall" style={styles.sectionTitle}>
              {STR.tasks.comments} ({comments.data?.length ?? 0})
            </Text>

            {comments.isPending ? (
              <SkeletonList count={2} />
            ) : comments.data && comments.data.length > 0 ? (
              comments.data.map((comment) => {
                const mine = comment.userId === user?.id;
                const color = colorFromString(comment.userId);
                return (
                  <View key={comment.id} style={styles.commentRow}>
                    <Avatar.Text
                      size={34}
                      label={initials(comment.fullName)}
                      style={{ backgroundColor: `${color}1F` }}
                      labelStyle={{ color, fontWeight: '700', fontSize: 12 }}
                    />
                    <View style={styles.flex}>
                      <View style={styles.commentHeader}>
                        <Text variant="labelLarge" style={{ fontWeight: '600' }}>
                          {comment.fullName}
                        </Text>
                        <Text variant="labelSmall" style={{ color: theme.colors.onSurfaceVariant }}>
                          {formatRelative(comment.createdAt)}
                        </Text>
                      </View>
                      <Text variant="bodyMedium">{comment.content}</Text>
                    </View>
                    {mine ? (
                      <IconButton icon="close" size={16} onPress={() => onDeleteComment(comment)} />
                    ) : null}
                  </View>
                );
              })
            ) : (
              <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                {STR.common.noData}
              </Text>
            )}

            {mentionIds.length > 0 ? (
              <View style={styles.chipRow}>
                {mentionIds.map((userId) => {
                  const member = members.data?.find((item) => item.userId === userId);
                  return (
                    <Chip key={userId} compact icon="at" onClose={() => setMentionIds((ids) => ids.filter((item) => item !== userId))}>
                      {member?.fullName ?? userId}
                    </Chip>
                  );
                })}
              </View>
            ) : null}

            <View style={styles.commentInput}>
              <TextInput
                mode="outlined"
                placeholder={STR.tasks.commentPlaceholder}
                value={commentText}
                onChangeText={setCommentText}
                multiline
                maxLength={2000}
                style={styles.flex}
              />
              <View style={styles.commentActions}>
                <Button mode="text" icon="at" onPress={() => setMentionDialog(true)}>
                  {STR.tasks.mention}
                </Button>
                <AppButton
                  label={STR.tasks.addComment}
                  icon="send"
                  onPress={onAddComment}
                  loading={addComment.isPending}
                  disabled={!commentText.trim()}
                  style={styles.sendButton}
                />
              </View>
            </View>
          </Card.Content>
        </Card>

        <View style={styles.bottomSpace} />
      </ScrollView>

      <MultiSelectDialog
        visible={mentionDialog}
        title={STR.tasks.mention}
        options={memberOptions.filter((option) => option.id !== user?.id)}
        selectedIds={mentionIds}
        onClose={() => setMentionDialog(false)}
        onConfirm={(ids) => {
          setMentionIds(ids);
          setMentionDialog(false);
        }}
      />
    </View>
  );
}

function showErrorMessage(show: (message: string) => void, error: unknown): void {
  show(normalizeError(error).message);
}

function fileIcon(mimeType: string): string {
  if (mimeType.startsWith('image/')) return 'file-image-outline';
  if (mimeType === 'application/pdf') return 'file-pdf-box';
  if (mimeType.includes('zip')) return 'file-zip-outline';
  if (mimeType.includes('word')) return 'file-word-outline';
  if (mimeType.includes('sheet') || mimeType.includes('excel')) return 'file-excel-outline';
  return 'file-outline';
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.lg },
  cardContent: { gap: spacing.sm },
  title: { fontWeight: '700', lineHeight: 28 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.xs, alignItems: 'center' },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
  assigneeRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginTop: spacing.xs },
  sectionTitle: { fontWeight: '700', marginBottom: spacing.xs },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  subtaskRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs },
  subtaskTitle: { flex: 1 },
  inlineInput: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, marginTop: spacing.xs },
  flex: { flex: 1 },
  fileRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, paddingVertical: spacing.xs },
  commentRow: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm, marginBottom: spacing.md },
  commentHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.sm },
  commentInput: { marginTop: spacing.sm, gap: spacing.xs },
  commentActions: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  sendButton: { minWidth: 110 },
  headerActions: { flexDirection: 'row' },
  bottomSpace: { height: spacing.xl },
});
