import { router, Stack } from 'expo-router';
import React from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { useSnackbar } from '@/components/SnackbarProvider';
import { ScheduleEventForm } from '@/features/schedule/ScheduleEventForm';
import { useCreateScheduleEvent } from '@/features/schedule/hooks';
import { spacing } from '@/theme/tokens';
import type { ScheduleEventPayload } from '@/types/api';
import { STR } from '@/utils/constants';

/** Tạo sự kiện lịch cá nhân. */
export default function NewScheduleEventScreen() {
  const theme = useTheme();
  const { show } = useSnackbar();
  const createEvent = useCreateScheduleEvent();

  const onSubmit = async (payload: ScheduleEventPayload) => {
    try {
      await createEvent.mutateAsync(payload);
      show(STR.common.created);
      router.back();
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ presentation: 'modal', title: STR.schedule.createTitle }} />
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <ScheduleEventForm submitting={createEvent.isPending} onSubmit={onSubmit} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, paddingBottom: spacing.xxl },
});
