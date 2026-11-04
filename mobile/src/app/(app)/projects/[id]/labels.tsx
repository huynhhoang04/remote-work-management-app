import { Stack, useLocalSearchParams } from 'expo-router';
import React, { useState } from 'react';
import { Alert, ScrollView, StyleSheet, View } from 'react-native';
import { Card, Icon, IconButton, Text, TextInput, useTheme } from 'react-native-paper';
import { normalizeError } from '@/api/errors';
import { AppButton } from '@/components/AppButton';
import { LabelChip } from '@/components/TaskChips';
import { useSnackbar } from '@/components/SnackbarProvider';
import { SkeletonList } from '@/components/SkeletonList';
import { StateView } from '@/components/StateView';
import { useProject } from '@/features/projects/hooks';
import { useCreateLabel, useDeleteLabel, useLabels } from '@/features/tasks/hooks';
import { radius, spacing } from '@/theme/tokens';
import { STR } from '@/utils/constants';

const COLORS = ['#2C63E8', '#7C3AED', '#16A34A', '#F97316', '#DC2626', '#0EA5E9', '#DB2777', '#64748B'];

/**
 * Quản lý nhãn của dự án.
 * @reference docs/plan-app/02-phase-2.md §5.4
 */
export default function ProjectLabelsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const { show } = useSnackbar();
  const project = useProject(id);
  const labels = useLabels(id);
  const createLabel = useCreateLabel(id);
  const deleteLabel = useDeleteLabel(id);

  const [name, setName] = useState('');
  const [color, setColor] = useState(COLORS[0]);
  const canWrite = project.data?.role !== 'VIEWER';

  const onCreate = async () => {
    const trimmed = name.trim();
    if (!trimmed) return;
    try {
      await createLabel.mutateAsync({ name: trimmed, color });
      setName('');
      show(STR.common.created);
    } catch (error) {
      show(normalizeError(error).message);
    }
  };

  const onDelete = (labelId: string) => {
    Alert.alert(STR.labels.deleteConfirm, undefined, [
      { text: STR.cancel, style: 'cancel' },
      {
        text: STR.common.delete,
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteLabel.mutateAsync(labelId);
            show(STR.common.deleted);
          } catch (error) {
            show(normalizeError(error).message);
          }
        },
      },
    ]);
  };

  return (
    <View style={[styles.screen, { backgroundColor: theme.colors.background }]}>
      <Stack.Screen options={{ title: STR.labels.title }} />
      {labels.isPending ? (
        <SkeletonList count={4} />
      ) : labels.isError ? (
        <StateView status="error" errorText={normalizeError(labels.error).message} onRetry={() => labels.refetch()} />
      ) : (
        <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
          {canWrite ? (
            <Card mode="elevated" style={styles.card}>
              <Card.Content style={styles.createCard}>
                <Text variant="titleSmall" style={styles.cardTitle}>
                  {STR.labels.create}
                </Text>
                <TextInput
                  mode="outlined"
                  label={STR.labels.name}
                  value={name}
                  onChangeText={setName}
                  maxLength={50}
                  style={styles.input}
                />
                <Text variant="labelLarge" style={{ color: theme.colors.onSurfaceVariant }}>
                  {STR.labels.color}
                </Text>
                <View style={styles.colors}>
                  {COLORS.map((item) => (
                    <IconButton
                      key={item}
                      icon={color === item ? 'check-circle' : 'circle'}
                      iconColor={item}
                      size={26}
                      onPress={() => setColor(item)}
                    />
                  ))}
                </View>
                <AppButton
                  label={STR.labels.create}
                  icon="plus"
                  onPress={onCreate}
                  loading={createLabel.isPending}
                  disabled={!name.trim()}
                />
              </Card.Content>
            </Card>
          ) : null}

          <Card mode="elevated" style={styles.card}>
            <Card.Content style={styles.noPadding}>
              {(labels.data?.length ?? 0) === 0 ? (
                <View style={styles.empty}>
                  <Icon source="label-outline" size={32} color={theme.colors.onSurfaceVariant} />
                  <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                    {STR.labels.empty}
                  </Text>
                  <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
                    {STR.labels.emptyHint}
                  </Text>
                </View>
              ) : (
                labels.data?.map((label) => (
                  <View key={label.id} style={styles.labelRow}>
                    <LabelChip label={label} />
                    <View style={styles.flex} />
                    {canWrite ? (
                      <IconButton
                        icon="trash-can-outline"
                        size={18}
                        iconColor={theme.colors.error}
                        onPress={() => onDelete(label.id)}
                      />
                    ) : null}
                  </View>
                ))
              )}
            </Card.Content>
          </Card>
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  container: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },
  card: { borderRadius: radius.lg },
  createCard: { gap: spacing.sm },
  cardTitle: { fontWeight: '700' },
  input: { backgroundColor: 'transparent' },
  colors: { flexDirection: 'row', flexWrap: 'wrap' },
  noPadding: { padding: 0 },
  empty: { alignItems: 'center', gap: spacing.xs, padding: spacing.xl },
  labelRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: spacing.md, paddingVertical: spacing.xs },
  flex: { flex: 1 },
});
