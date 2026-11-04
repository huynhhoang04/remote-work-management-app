import { router, Stack, useLocalSearchParams } from 'expo-router';
import React from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { ScheduleEventForm } from '@/features/schedule/ScheduleEventForm';
import {
  useDeleteScheduleEvent,
  useScheduleEvent,
  useUpdateScheduleEvent,
} from '@/features/schedule/hooks';
import { spacing } from '@/theme/tokens';
import type { ScheduleEventPayload } from '@/types/api';
import { STR } from '@/utils/constants';

/** Sửa/xoá sự kiện lịch (chỉ chủ sự kiện). */
export default function EditScheduleEventScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const event = useScheduleEvent(id);
  const updateEvent = useUpdateScheduleEvent();
  const deleteEvent = useDeleteScheduleEvent();

  const onSubmit = async (payload: ScheduleEventPayload) => {
    try {
      await updateEvent.mutateAsync({ eventId: id, payload });
      show(STR.common.saved);
      router.back();
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  const onDelete = () => {
    Alert.alert(STR.common.delete, STR.schedule.deleteConfirm, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.common.delete,
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteEvent.mutateAsync(id);
            show(STR.common.deleted);
            router.back();
          } catch (error) {
            show(normalizeError(error).message);
          }
        },
      },
    ]);
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ presentation: 'modal', title: STR.schedule.editTitle }} />
      {event.isPending ? (
        <SkeletonList count={2} />
      ) : event.isError || !event.data ? (
        <StateView
          status="error"
          errorText={event.data === null ? 'Không tìm thấy sự kiện' : normalizeError(event.error).message}
          onRetry={() => event.refetch()}
        />
      ) : (
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          <ScheduleEventForm initial={event.data} submitting={updateEvent.isPending} onSubmit={onSubmit} />
          <AppButton
            label={STR.common.delete}
            variant="danger"
            icon="trash-can-outline"
            onPress={onDelete}
            loading={deleteEvent.isPending}
            style={styles.delete}
          />
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, paddingBottom: spacing.xxl },
  delete: { marginTop: spacing.lg },
});
